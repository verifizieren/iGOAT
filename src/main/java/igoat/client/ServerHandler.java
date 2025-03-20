package igoat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static igoat.client.Client.log;

public class ServerHandler {
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    private boolean connected = false;
    private boolean closed = false;
    private final List<String> messageBuffer = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private static final int PORT = 8888;

    public ServerHandler() {
        log("connecting to server");
        reconnect();

        Thread receiver = new Thread(this::receive);
        receiver.start();
        Thread pingPong = new Thread(this::pingPong);
        pingPong.start();
    }
    /**
     * sends the connection status
     */
    public boolean isConnected() {
        return connected;
    }
    /**
     * sends a message to the server and returns whether it was successful
     */
    public void send(String msg) {
        try {
            writer.println(msg);
            log("Sent: " + msg);
        } catch (Exception e) {
            log("method send " + e.toString());
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
        closed = true;
        try {
            socket.close();
            writer.close();
            reader.close();
            log("Closed.");
        } catch (IOException e) {
            log("method close " + e.toString());
        }
    }
    /**
     * checks for received message and adds it to the buffer
     */
    private void receive() {
        while (!closed) {
            try {
                String msg = reader.readLine();
                if (msg != null) {
                    lock.lock();
                    messageBuffer.add(msg);
                    lock.unlock();
                    log("Received: " + msg);
                }
            } catch (IOException e) {
                log("method receive " + e.toString());
                reconnect();
            }
        }
    }
    /**
     * check for pings every 100ms and respond
     */
    private void pingPong() {
        long timer = System.currentTimeMillis();

        while (!closed) {
            if (System.currentTimeMillis() - timer > 100) {
                if (checkPing()) {
                    log("pinged");
                    send("pong");
                    timer = System.currentTimeMillis();
                    connected = true;
                }
            }
            if (System.currentTimeMillis() - timer > 5000) {
                log("Connection timed out");
                timer = System.currentTimeMillis();
                connected = false;
                break;
            }
        }
    }
    /**
     * check if there were any incoming pings
     */
    private boolean checkPing() {
        boolean ping = false;
        lock.lock();
        // check for pings and clear them from the buffer
        for (int i = 0; i < messageBuffer.size(); i++) {
            if (!messageBuffer.get(i).equals("ping")) {
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
    private void reconnect() {
        if (socket != null) {
            try {
                socket.close();
                writer.close();
                reader.close();
            } catch (Exception e) {
                log("method reconnect, closing socket " + e.toString());
            }
        }

        try {
            socket = new Socket("localhost", PORT);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            log("method reconnect, new socket " + e.toString());
        }
    }
}