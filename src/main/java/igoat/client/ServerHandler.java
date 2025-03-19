package igoat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

import static igoat.client.Client.log;

public class ServerHandler {
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private final ReentrantLock lock = new ReentrantLock();
    private boolean connected = false;

    private static final int PORT = 8888;

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
     * reconnects to server and returns true if successful
     */
    public void reconnect() {
        try {
            socket = new Socket("localhost", PORT);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            startPingPong();
        } catch (IOException e) {
            log(e.toString());
        }
    }
    /**
     * sends a message to the server and returns whether it was successful
     */
    public void send(String msg) {
        lock.lock();
        try {
            writer.println(msg);
            log("Sent: " + msg);
        } catch (Exception e) {
            log(e.toString());
        } finally {
            lock.unlock();
        }
    }
    /**
     * checks for a received message and returns it
     */
    public String[] receive() {
        lock.lock();
        try {
            String msg = reader.readLine();
            log("Received: " + msg);
            return msg.split("\\s"); // separate message into words
        } catch (IOException e) {
            log(e.toString());
            return new String[0];
        } finally {
            lock.unlock();
        }

    }
    /**
     * close the connection
     */
    public void close() {
        try {
            socket.close();
            writer.close();
            reader.close();
        } catch (IOException e) {
            log(e.toString());
        }
    }
    /**
     * start the thread for PingPong
     */
    private void startPingPong() {
        Thread pong = new Thread(this::pingPong);
        pong.start();
    }
    /**
     * check for pings every 100ms and respond
     */
    private void pingPong() {
        try {
            long timer = System.currentTimeMillis();
            while (true) {
                if (System.currentTimeMillis() - timer > 100) {
                    timer = System.currentTimeMillis();
                    if (this.receive()[0].equals("ping")) {
                        connected = true;
                        this.send("pong");
                    }
                }
                if (System.currentTimeMillis() - timer > 2000) {
                    log("Connection timed out");
                    connected = false;
                    break;
                }
            }
        } catch (Exception e) {
            log(e.toString());
        } finally {
            connected = false;
        }
    }
}
