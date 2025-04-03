package igoat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerHandler {

    private static final int SERVER_UDP_LISTENING_PORT = 61001;
    private static final String UDP_REGISTRATION_PREFIX = "register_udp:";
    private static final String NICKNAME_CONFIRM_PREFIX = "confirm:";

    private Socket msgSocket;
    private PrintWriter msgWriter;
    private BufferedReader msgReader;

    private DatagramSocket updateSocket;

    private Thread messageReceiver;
    private Thread updateReceiver;

    private boolean connected = false;
    private final BlockingQueue<String> messageBuffer = new LinkedBlockingQueue<>();
    private String lastUpdate = "";
    private String confirmedNickname = null;

    private final String host;
    private final int port;
    private final int TIMEOUT = 3000;

    public ServerHandler(String host, int port) {
        this.host = host;
        this.port = port;
        reconnect();
    }

    /**
     * Checks whether the server is still connected
     *
     * @return returns the connection status
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Sends a message to the server
     *
     * @param msg The message that will be sent
     */
    public void sendMessage(String msg) {
        try {
            msgWriter.println(msg);
        } catch (Exception e) {
            log("Couldn't send message");
            log(e.getMessage());
        }
    }

    /**
     * Gets the first message from the message buffer
     *
     * @return returns the message or null if there was no message
     */
    public String getMessage() {
        return messageBuffer.poll();
    }

    /**
     * Sends data to the server using UDP
     *
     * @param msg string to be sent to the Server
     */
    public void sendUpdate(String msg) {
        try {
            if (updateSocket == null || !connected) {
                System.err.println("[UDP_CLIENT] Cannot send - socket is null or not connected");
                return;
            }
            
            System.out.println("[UDP_CLIENT] Sending: '" + msg + "' to " + host + ":" + SERVER_UDP_LISTENING_PORT);
            System.out.println("[UDP_CLIENT] Using local port: " + updateSocket.getLocalPort());
            
            byte[] buffer = msg.getBytes();
            InetAddress targetAddress = InetAddress.getByName(host);
            DatagramPacket packet =
                new DatagramPacket(buffer, buffer.length, targetAddress, SERVER_UDP_LISTENING_PORT);
            updateSocket.send(packet);
            
            System.out.println("[UDP_CLIENT] Packet sent - " + buffer.length + " bytes");
        } catch (Exception e) {
            System.err.println("[UDP_CLIENT] Send error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the latest update sent via UDP
     *
     * @return The latest message from the server
     */
    public String getLastUpdate() {
        String update = lastUpdate;
        lastUpdate = "";
        return update;
    }

    /**
     * Closes and reopens the receiver thread and the socket
     */
    public void reconnect() {
        close();
        // (re)open socket and start receiver threads
        try {
            msgSocket = new Socket(host, port);
            msgWriter = new PrintWriter(msgSocket.getOutputStream(), true);
            msgReader = new BufferedReader(new InputStreamReader(msgSocket.getInputStream()));

            try {
                updateSocket = new DatagramSocket();
                System.out.println("[UDP_CLIENT] Created UDP socket on local port: " + updateSocket.getLocalPort());
                System.out.println("[UDP_CLIENT] Will send to server UDP port: " + SERVER_UDP_LISTENING_PORT);
            } catch (Exception e) {
                System.err.println("[UDP_CLIENT] Failed to create UDP socket: " + e.getMessage());
                e.printStackTrace();
            }
            
            connected = true;

            messageReceiver = new Thread(this::receiveMSG);
            updateReceiver = new Thread(this::receiveUpdate);
            messageReceiver.start();
            updateReceiver.start();
            log("Connected to server at " + host + ":" + port);
        } catch (IOException e) {
            log("Couldn't connect to server at " + host + ":" + port);
            log(e.getMessage());
        }
    }

    /**
     * Closes the socket and receiver thread
     */
    public void close() {
        connected = false;
        // close messageReceiver thread
        if (messageReceiver != null) {
            try {
                messageReceiver.join();
            } catch (InterruptedException e) {
                log(e.getMessage());
            }
        }
        // close updateReceiver thread
        if (updateReceiver != null) {
            try {
                updateReceiver.join();
            } catch (InterruptedException e) {
                log(e.getMessage());
            }
        }
        // close msgSocket
        if (msgSocket != null) {
            try {
                msgSocket.close();
                msgWriter.close();
                msgReader.close();
            } catch (Exception e) {
                log(e.getMessage());
            }
        }
        // close updateSocket
        if (updateSocket != null) {
            updateSocket.close();
        }
    }

    /**
     * Continuously checks for a received TCP message from the server and adds it to the message
     * buffer. If the message was a ping, it sends a response instead.
     */
    private void receiveMSG() {
        long pingTimer = System.currentTimeMillis();
        String msg;

        while (connected) {
            try {
                msg = msgReader.readLine();
            } catch (IOException e) {
                msg = "";
            }

            if ("ping".equals(msg)) {
                sendMessage("pong");
                pingTimer = System.currentTimeMillis();
            } else if (msg.startsWith(NICKNAME_CONFIRM_PREFIX)) {
                this.confirmedNickname = msg.substring(NICKNAME_CONFIRM_PREFIX.length());
                log("Nickname confirmed by server: " + this.confirmedNickname);
                sendUdpRegistrationPacket();
                messageBuffer.add(msg);
            } else if (!msg.isEmpty()) {
                messageBuffer.add(msg);
            }

            if (System.currentTimeMillis() - pingTimer > TIMEOUT) {
                log("Connection timed out");
                messageBuffer.add("Connection timed out");
                break;
            }
        }
        connected = false;
    }

    /**
     * Continuously checks for received UDP data from the server. The received message is written to
     * lastUpdate.
     */
    private void receiveUpdate() {
        System.out.println("[UDP_CLIENT] Starting UDP receiver on port " + 
                         (updateSocket != null ? updateSocket.getLocalPort() : "unknown"));
        
        byte[] receiveBuffer = new byte[512];

        try {
            while (connected) {
                if (updateSocket == null || updateSocket.isClosed()) {
                    System.err.println("[UDP_CLIENT] Cannot receive - socket is null or closed");
                    break;
                }
                
                updateSocket.setSoTimeout(10);
                
                try {
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer,
                        receiveBuffer.length);
                    
                    updateSocket.receive(receivePacket);
                    String receivedMsg = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    
                    //System.out.println("[UDP_CLIENT] Received: '" + receivedMsg + "' from " +
                                     //receivePacket.getAddress().getHostAddress() + ":" + 
                                     //receivePacket.getPort());

                    if (!receivedMsg.startsWith("udp_ack:")) {
                        lastUpdate = receivedMsg;
                    }
                } catch (java.net.SocketTimeoutException e) {
                }
            }
        } catch (Exception e) {
            if (connected && updateSocket != null && !updateSocket.isClosed()) {
                System.err.println("[UDP_CLIENT] Receive error: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("[UDP_CLIENT] UDP receiver stopped");
    }

    /**
     * Sends the UDP registration packet to the server's known listening port.
     * This should be called after the nickname is confirmed.
     */
    private void sendUdpRegistrationPacket() {
        if (this.confirmedNickname == null || updateSocket == null || updateSocket.isClosed()) {
            System.err.println("[UDP_CLIENT] Cannot register - nickname or socket unavailable");
            return;
        }

        try {
            int localUdpPort = updateSocket.getLocalPort();
            String registrationMsg = String.format("%s%s:%d", 
                                               UDP_REGISTRATION_PREFIX, 
                                               this.confirmedNickname, 
                                               localUdpPort);
            
            System.out.println("\n======= UDP REGISTRATION =======");
            System.out.println("[UDP_CLIENT] Preparing registration: '" + registrationMsg + "'");
            System.out.println("[UDP_CLIENT] From local port: " + localUdpPort);
            System.out.println("[UDP_CLIENT] To server listening port: " + SERVER_UDP_LISTENING_PORT);
            
            byte[] buffer = registrationMsg.getBytes();
            InetAddress serverAddress = InetAddress.getByName(host);
            DatagramPacket registrationPacket = new DatagramPacket(buffer, buffer.length, 
                                                                  serverAddress, SERVER_UDP_LISTENING_PORT);
            
            updateSocket.send(registrationPacket);
            System.out.println("[UDP_CLIENT] Registration packet sent successfully");
            System.out.println("===============================\n");
        } catch (IOException e) {
            System.err.println("[UDP_CLIENT] Registration error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gets the confirmed nickname from the server.
     *
     * @return The confirmed nickname, or null if not yet confirmed.
     */
    public String getConfirmedNickname() {
        return confirmedNickname;
    }

    private static void log(String msg) {
        System.out.println("[ServerHandler] " + msg);
    }
}
