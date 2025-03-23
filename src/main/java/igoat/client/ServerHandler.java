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

    private Socket msgSocket;
    private PrintWriter msgWriter;
    private BufferedReader msgReader;

    private DatagramSocket updateSocket;

    private Thread messageReceiver;
    private Thread updateReceiver;

    private boolean connected = false;
    private final BlockingQueue<String> messageBuffer = new LinkedBlockingQueue<>();
    private String lastUpdate = "";

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
            byte[] buffer = msg.getBytes();
            DatagramPacket packet =
                new DatagramPacket(buffer, buffer.length, InetAddress.getByName(host), port);
            updateSocket.send(packet);
        } catch (Exception e) {
            log(e.getMessage());
        }
    }

    /**
     * Retrieves the latest update sent via UDP
     *
     * @return The latest message from the server
     */
    public String getLastUpdate() {
        return lastUpdate;
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

            updateSocket = new DatagramSocket();
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

        try {
            while (connected) {
                msg = msgReader.readLine();

                switch (msg) {
                    case "ping":
                        sendMessage("pong");
                        pingTimer = System.currentTimeMillis();
                        break;
                    case null:
                        break;
                    default:
                        messageBuffer.add(msg);
                }

                if (System.currentTimeMillis() - pingTimer > TIMEOUT) {
                    log("Connection timed out");
                    break;
                }
            }
        } catch (IOException e) {
            log(e.getMessage());
        } finally {
            connected = false;
        }
    }

    /**
     * Continuously checks for received UDP data from the server. The received message is written to
     * lastUpdate.
     */
    private void receiveUpdate() {
        byte[] receiveBuffer = new byte[512];

        try {
            while (connected) {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer,
                    receiveBuffer.length);
                updateSocket.receive(receivePacket);
                lastUpdate = new String(receivePacket.getData(), 0, receivePacket.getLength());
            }
        } catch (Exception e) {
            log(e.getMessage());
        }
    }

    private static void log(String msg) {
        System.out.println("[ServerHandler] " + msg);
    }
}
