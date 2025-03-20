package igoat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private String nickname = "unnamed";
    private volatile boolean running = true;
    private long lastPongTime;
    private final static long PING_INTERVAL = 3000; // 3 Sekunden
    private final static long TIMEOUT = 10000; // 10 Sekunden

    private final static List<ClientHandler> clientList = new CopyOnWriteArrayList<>();

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.lastPongTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        clientList.add(this);
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Starte den Ping-Thread
            Thread pingThread = new Thread(this::runPingPong);
            pingThread.start();

            String message;
            while(running && (message = in.readLine()) != null) {
                System.out.println("Received from " + nickname + ": " + message);
                
                if (message.equals("pong")) {
                    handlePong();
                    continue;
                }
                
                handleCommand(message);
            }
        } catch(IOException e) {
            System.err.println("Problem: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void runPingPong() {
        long lastPingSent = 0;
        
        while (running) {
            long currentTime = System.currentTimeMillis();
            
            if (currentTime - lastPingSent >= PING_INTERVAL) {
                sendMessage("ping");
                lastPingSent = currentTime;
                System.out.println("Ping sent to " + nickname);
            }
            
            if (currentTime - lastPongTime >= TIMEOUT) {
                System.out.println("Client " + nickname + " timed out");
                running = false;
                break;
            }
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void handleCommand(String message) {
        try {
            String[] parts = message.split(":");
            if (parts.length != 2) {
                sendError("Invalides Command Format");
                return;
            }

            String command = parts[0];
            String[] params = parts[1].split(",");

            switch (command.toLowerCase()) {
                case "connect":
                    handleConnect(params);
                    break;
                case "chat":
                    handleChat(params);
                    break;
                case "lobby":
                    handleLobby(params);
                    break;
                case "username":
                    handleUsername(params);
                    break;
                case "whisper":
                    handleWhisper(params);
                    break;
                default:
                    sendError("Unbekanntes Command: " + command);
            }
        } catch (Exception e) {
            sendError("Fehler beim Verarbeiten des Commands: " + e.getMessage());
        }
    }

    private void handleConnect(String[] params) {
        if (params.length < 1) {
            sendError("Kein Nickname angegeben");
            return;
        }
        this.nickname = params[0];
        sendMessage("confirm:" + this.nickname);
        broadcast("chat:User " + this.nickname + " connected");
    }

    private void handleChat(String[] params) {
        if (params.length < 1) {
            sendError("Keine Nachricht angegeben");
            return;
        }
        broadcast("chat:" + this.nickname + "," + params[0]);
    }

    private void handleUsername(String[] params) {
        if (params.length < 1) {
            sendError("Kein Username angegeben");
            return;
        }
        String oldNickname = this.nickname;
        this.nickname = params[0];
        sendMessage("confirm:Username gesetzt zu " + this.nickname);
        broadcast("chat:User " + oldNickname + " hat seinen/ihren Username zu " + this.nickname + " geändert");
    }

    private void handleLobby(String[] params) {
        if (params.length < 1) {
            sendError("Kein Lobby Code angegeben");
            return;
        }
        // TODO: Lobby implementieren
        sendMessage("confirm:Beigetreten zu Lobby " + params[0]);
    }

    private void handlePong() {
        lastPongTime = System.currentTimeMillis();
        System.out.println("Pong received from " + nickname);
    }

    private void disconnect() {
        try {
            running = false;
            clientList.remove(this);
            if(in != null) in.close();
            if(out != null) out.close();
            if(clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            broadcast("chat:User " + this.nickname + " hat sich abgemeldet");
            System.out.println("Client " + nickname + " disconnected");
        } catch(IOException e) {
            System.err.println("Fehler beim Abmelden: " + e.getMessage());
        }
    }

    private void sendError(String errorMessage) {
        sendMessage("error:" + errorMessage);
    }

    private void sendMessage(String message) {
        if (out != null && !clientSocket.isClosed()) {
            out.println(message);
        }
    }

    private void handleWhisper(String[] params) {
        if (params.length < 2) {
            sendError("Keine Whisper Nachricht angegeben");
            return;
        }
        String recipient = params[0];
        String message = params[1];
        for (ClientHandler client : clientList) {
            if (client.nickname.equals(recipient)) {
                client.sendMessage("whisper:" + this.nickname + "," + message);
                return;
            }
        }
        sendError("User " + recipient + " nicht gefunden");
    }

    private void broadcast(String message) {
        for (ClientHandler client : clientList) {
            client.sendMessage(message);
        }
    }
}

