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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles the connection of the client to the server, including sockets for TCP and UDP.
 */
public class ServerHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);

    private static final int SERVER_UDP_LISTENING_PORT = 61001;
    private static final String UDP_REGISTRATION_PREFIX = "register_udp:";
    private static final String NICKNAME_CONFIRM_PREFIX = "confirm:";
    private static final int MAX_MSG_LENGTH = 200;

    private Socket msgSocket;
    PrintWriter msgWriter;
    BufferedReader msgReader;

    UDPSocket updateSocket; // abstraction for testability

    private Thread messageReceiver;
    private Thread updateReceiver;

    boolean connected = false;
    final BlockingQueue<String> messageBuffer = new LinkedBlockingQueue<>();
    String lastUpdate = "";
    String confirmedNickname = null;

    private final String host;
    private final int port;
    private final int TIMEOUT = 3000;

    private final String username;

    /**
     * Creates a new ServerHandler instance
     * @param host Host IP
     * @param port Port
     * @param username Client username
     */
    /**
     * Protected constructor for testing: does not connect to any server or open sockets.
     */
    protected ServerHandler(String host, int port, String username, boolean skipNetwork) {
        this.host = host;
        this.port = port;
        this.username = username;
    }

    public ServerHandler(String host, int port, String username) {
        this.host = host;
        this.port = port;
        this.username = username;
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
        if (msg.length() > MAX_MSG_LENGTH) {
            msg = msg.substring(0, MAX_MSG_LENGTH);
            logger.error("message to be sent was too long, shortening..");
        }

        try {
            msgWriter.println(msg);
        } catch (Exception e) {
            logger.error("Couldn't send message", e);
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
                logger.error("Cannot send - socket is null or not connected");
                return;
            }
            //logger.info("Sending {} to {}:{}", msg, host, SERVER_UDP_LISTENING_PORT)
            //logger.info("Using local port: ", updateSocket.getLocalPort());
            byte[] buffer = msg.getBytes();
            InetAddress targetAddress = InetAddress.getByName(host);
            DatagramPacket packet =
                new DatagramPacket(buffer, buffer.length, targetAddress, SERVER_UDP_LISTENING_PORT);
            updateSocket.send(packet);
            //logger.info("Packet sent - {} bytes", buffer.length);
        } catch (Exception e) {
            logger.error("Couldn't send update", e);
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
        try {
            msgSocket = new Socket(host, port);
            msgWriter = new PrintWriter(msgSocket.getOutputStream(), true);
            msgReader = new BufferedReader(new InputStreamReader(msgSocket.getInputStream()));

            if (username != null) {
                sendMessage("connect:" + username);
            }

            try {
                updateSocket = new RealUDPSocket(new DatagramSocket());
                connected = true;

                messageReceiver = new Thread(this::receiveMSG);
                updateReceiver = new Thread(this::receiveUpdate);
                messageReceiver.start();
                updateReceiver.start();
                return;
            } catch (Exception e) {
                logger.error("Failed to create UDP socket: ", e);
                close();
                return;
            }
        } catch (IOException e) {
            logger.error("Connection error: ", e);
            close();
            return;
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
                logger.error("Couldn't close messageReceiver", e);
            }
        }
        // close updateReceiver thread
        if (updateReceiver != null) {
            try {
                updateReceiver.join();
            } catch (InterruptedException e) {
                logger.error("Couldn't close updateReceiver", e);
            }
        }
        // close msgSocket
        if (msgSocket != null) {
            try {
                msgSocket.close();
                msgWriter.close();
                msgReader.close();
            } catch (Exception e) {
                logger.error("Couldn't close msgSocket", e);
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
    void receiveMSG() {
        long pingTimer = System.currentTimeMillis();
        String msg;

        while (connected) {
            try {
                msg = msgReader.readLine();
                if (msg == null) {
                    logger.warn("Server closed connection");
                    connected = false;
                    break;
                }
            } catch (IOException e) {
                logger.error("Error reading from server", e);
                connected = false;
                break;
            }

            if ("ping".equals(msg)) {
                sendMessage("pong");
                pingTimer = System.currentTimeMillis();
                continue;
            } else if (msg.startsWith(NICKNAME_CONFIRM_PREFIX)) {
                String confirmMessage = msg.substring(NICKNAME_CONFIRM_PREFIX.length());
                String newNickname;
                if (confirmMessage.startsWith("Username gesetzt zu ")) {
                    newNickname = confirmMessage.substring("Username gesetzt zu ".length());
                } else {
                    newNickname = confirmMessage;
                }
                this.confirmedNickname = newNickname;
                sendUdpRegistrationPacket();
                messageBuffer.add(msg);
            } else if (!msg.isEmpty()) {
                messageBuffer.add(msg);
            }

            if (System.currentTimeMillis() - pingTimer > TIMEOUT) {
                logger.warn("Connection timed out");
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
        byte[] receiveBuffer = new byte[512];

        try {
            while (connected) {
                if (updateSocket == null) {
                    logger.error("Cannot receive - socket is null");
                    break;
                }
                if (updateSocket instanceof RealUDPSocket real) {
                    DatagramSocket ds = real.getSocket();
                    if (ds.isClosed()) {
                        logger.error("Cannot receive - socket is closed");
                        break;
                    }
                    ds.setSoTimeout(10);
                    try {
                        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer,
                            receiveBuffer.length);
                        ds.receive(receivePacket);
                        String receivedMsg = new String(receivePacket.getData(), 0, receivePacket.getLength());
                        //logger.info("Received: {} from {}:{}", receivedMsg, receivePacket.getAddress(), receivePacket.getPort());
                        if (!receivedMsg.startsWith("udp_ack:")) {
                            lastUpdate = receivedMsg;
                        }
                    } catch (java.net.SocketTimeoutException e) {
                    }
                } else {
                    // Not supported for mocks
                    break;
                }
            }
        } catch (Exception e) {
            if (connected && updateSocket instanceof RealUDPSocket real && !real.getSocket().isClosed()) {
                logger.error("Receive error", e);
            }
        }
    }

    /**
     * Sends the UDP registration packet to the server's known listening port.
     * This should be called after the nickname is confirmed.
     */
    private void sendUdpRegistrationPacket() {
        if (this.confirmedNickname == null || updateSocket == null) {
            logger.error("Cannot register - nickname or socket unavailable");
            return;
        }
        int localUdpPort = -1;
        if (updateSocket instanceof RealUDPSocket real) {
            localUdpPort = real.getSocket().getLocalPort();
        } else {
            // For mocks, use dummy port
            localUdpPort = 0;
        }
        try {
            String registrationMsg = String.format("%s%s:%d",
                                               UDP_REGISTRATION_PREFIX,
                                               this.confirmedNickname,
                                               localUdpPort);
            byte[] buffer = registrationMsg.getBytes();
            InetAddress serverAddress = InetAddress.getByName(host);
            DatagramPacket registrationPacket = new DatagramPacket(buffer, buffer.length,
                                                                  serverAddress, SERVER_UDP_LISTENING_PORT);
            updateSocket.send(registrationPacket);
        } catch (IOException e) {
            logger.error("Registration error: " + e.getMessage());
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
}

