package igoat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerHandler {
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    private Thread receiver;

    private boolean connected = false;
    private final BlockingQueue<String> messageBuffer = new LinkedBlockingQueue<>();
    private static final int PORT = 8889;

    public ServerHandler() {
        reconnect();
    }
    /**
     * Returns the connection status
     */
    public boolean isConnected() {
        return connected;
    }
    /**
     * Sends a message to the server
     */
    public void send(String msg) {
        try {
            writer.println(msg);
        } catch (Exception e) {
            log("Couldn't send message");
            log(e.getMessage());
        }
    }
    /**
     * Returns the first element from the message buffer. Returns null when the buffer is empty.
     */
    public String getMessage() {
        return messageBuffer.poll();
    }
    /**
     * Closes the connection
     */
    public void close() {
        connected = false;
        // close receiver thread
        if (receiver != null) {
            try {
                receiver.join();
            } catch (InterruptedException e) {
                log(e.getMessage());
            }
        }
        // close socket
        if (socket != null) {
            try {
                socket.close();
                writer.close();
                reader.close();
            } catch (Exception e) {
                log(e.getMessage());
            }
        }
    }
    /**
     * Checks for received message and adds it to the buffer. If the message was a ping, it sends a response instead
     */
    private void receive() {
        long pingTimer = System.currentTimeMillis();
        String msg;

        try {
            while (connected) {
                msg = reader.readLine();

                switch (msg) {
                    case "ping":
                        send("pong");
                        pingTimer = System.currentTimeMillis();
                        break;
                    case null:
                        break;
                    default:
                        messageBuffer.add(msg);
                }

                if (System.currentTimeMillis() - pingTimer > 3000) {
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
     * reconnects to the server
     */
    public void reconnect() {
        close();
        // (re)open socket and start receiver thread
        try {
            socket = new Socket("localhost", PORT);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;

            receiver = new Thread(this::receive);
            receiver.start();
            log("Connected to server");
        } catch (IOException e) {
            log("Couldn't connect to server");
            log(e.getMessage());
        }
    }

    private static void log(String msg) {
        System.out.println("[ServerHandler] " + msg);
    }
}