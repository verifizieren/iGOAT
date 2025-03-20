package igoat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class ServerHandler {
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    private boolean connected = false;
    private final List<String> messageBuffer = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private static final int PORT = 8889;

    public ServerHandler() {
        reconnect();
    }
    /**
     * sends the connection status
     */
    public boolean isConnected() {
        return connected;
    }
    /**
     * sends a message to the server
     */
    public void send(String msg) {
        if (!connected) {
            log("Server is offline");
            return;
        }

        try {
            writer.println(msg);
        } catch (Exception e) {
            log("Couldn't send message");
            log(e.getMessage());
        }
    }
    /**
     * returns the first non-ping message from the message buffer
     */
    public String getMessage() {
        lock.lock();
        for (int i = 0; i < messageBuffer.size(); i++) {
            if (!messageBuffer.get(i).equals("ping")) {
                return messageBuffer.remove(i);
            }
        }
        lock.unlock();
        return "";
    }
    /**
     * close the connection
     */
    public void close() {
        connected = false;
        try {
            socket.close();
            writer.close();
            reader.close();
            log("Connection closed.");
        } catch (IOException e) {
            log("Couldn't close connection");
            log(e.getMessage());
        }
    }
    /**
     * checks for received message and adds it to the buffer
     */
    private void receive() {
        while (connected) {
            try {
                String msg = reader.readLine();
                if (msg != null) {
                    lock.lock();
                    messageBuffer.add(msg);
                    lock.unlock();
                }
            } catch (IOException e) {
                log("Connection lost");
                log(e.getMessage());
                connected = false;
            }
        }
        log("Receiver thread closed");
    }
    /**
     * check for pings every 100ms and respond
     */
    private void pingPong() {
        long timer = System.currentTimeMillis();

        while (connected) {
            if (System.currentTimeMillis() - timer > 1100) {
                if (checkPing()) {
                    send("pong");
                    timer = System.currentTimeMillis();
                    log("ping pong still running");
                }
            }
            if (System.currentTimeMillis() - timer > 5000) {
                log("Connection timed out");
                timer = System.currentTimeMillis();
                connected = false;
                break;
            }
        }
        log("Pong thread closed");
    }
    /**
     * check if there were any incoming pings
     */
    private boolean checkPing() {
        boolean ping = false;
        lock.lock();
        // check for pings and clear them from the buffer
        for (int i = 0; i < messageBuffer.size(); i++) {
            if (messageBuffer.get(i).equals("ping")) {
                messageBuffer.remove(i);
                ping = true;
            }
        }
        lock.unlock();
        return ping;
    }
    /**
     * reconnects to the server
     */
    public void reconnect() {
        connected = false;

        if (socket != null) {
            try {
                socket.close();
                writer.close();
                reader.close();
            } catch (Exception e) {
                log(e.getMessage());
            }
        }

        try {
            socket = new Socket("localhost", PORT);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;

            Thread receiver = new Thread(this::receive);
            Thread pingpong = new Thread(this::pingPong);
            receiver.start();
            pingpong.start();
        } catch (IOException e) {
            log("Couldn't connect to server");
            log(e.getMessage());
        }
    }

    private static void log(String msg) {
        System.out.println("[ServerHandler] " + msg);
    }
}