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
    private Thread pingThread;
    private String nickname;
    private volatile boolean running = true;
    private long lastPongTime;
    private final static long PING_INTERVAL = 2000; // 3 Sekunden
    private final static long TIMEOUT = 10000; // 10 Sekunden

    private final static List<ClientHandler> clientList = new CopyOnWriteArrayList<>();

    /**
     * Erstellt einen neuen ClientHandler für eine Socket Verbindung.
     * Generiert automatisch einen eindeutigen Nickname für den Client.
     *
     * @param clientSocket Die Socket Verbindung zum Client
     */
    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.lastPongTime = System.currentTimeMillis();
        this.nickname = generateUniqueNickname("spieler");
        System.out.println("Neuer Client verbunden als: " + this.nickname);
    }

    /**
     * Hauptschleife für die Client Verbindung.
     * Verarbeitet eingehende Nachrichten und handhabt die PingPong Verbindungsprüfung.
     */
    @Override
    public void run() {
        clientList.add(this);
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            sendMessage("info:Du bist verbunden als: " + this.nickname);
            broadcast("chat:User " + this.nickname + " connected");

            // Starte den Ping-Thread
            pingThread = new Thread(this::runPingPong);
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

    /**
     * Führt die PingPong Verbindungsprüfung durch.
     * Sendet periodisch Pings und überprüft Timeouts.
     */
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

    /**
     * Verarbeitet eingehende Befehle vom Client.
     * Format: command:param1,param2,...
     *
     * @param message Die empfangene Nachricht im Format "command:parameter"
     */
    private void handleCommand(String message) {
        try {
            if (message.equals("ciao") || message.equals("logout") || message.equals("exit")) {
                handleLogout();
                return;
            }

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

    /**
     * Generiert einen eindeutigen Nickname basierend auf einem Basis Nickname.
     * Fügt _1, _2, etc. hinzu, wenn der Name bereits vergeben ist.
     *
     * @param baseNickname Der gewünschte Basis Nickname
     * @return Ein eindeutiger Nickname
     */
    private String generateUniqueNickname(String baseNickname) {
        String newNickname = baseNickname;
        int counter = 1;
        
        while (isNicknameTaken(newNickname)) {
            newNickname = baseNickname + "_" + counter;
            counter++;
        }
        
        return newNickname;
    }

    /**
     * Prüft, ob ein Nickname bereits von einem anderen Client verwendet wird.
     *
     * @param nickname Der zu prüfende Nickname
     * @return true wenn der Nickname bereits vergeben ist, sonst false
     */
    private boolean isNicknameTaken(String nickname) {
        for (ClientHandler client : clientList) {
            if (client != this && client.nickname.equals(nickname)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verarbeitet eine Connect Anfrage.
     * Format: connect:nickname
     *
     * @param params Array mit Parametern, wobei params[0] der gewünschte Nickname ist
     */
    private void handleConnect(String[] params) {
        if (params.length < 1) {
            sendError("Kein Nickname angegeben");
            return;
        }
        
        String requestedNickname = params[0];
        this.nickname = generateUniqueNickname(requestedNickname);
        
        if (!requestedNickname.equals(this.nickname)) {
            sendMessage("info:Dein gewünschter Nickname war bereits vergeben. Neuer Nickname: " + this.nickname);
        }
        
        sendMessage("confirm:" + this.nickname);
        broadcast("chat:User " + this.nickname + " connected");
    }

    /**
     * Verarbeitet eine Chat Nachricht.
     * Format: chat:message
     *
     * @param params Array mit Parametern, wobei params[0] die Nachricht ist
     */
    private void handleChat(String[] params) {
        if (params.length < 1) {
            sendError("Keine Nachricht angegeben");
            return;
        }
        broadcast("chat:" + this.nickname + "," + params[0]);
    }

    /**
     * Verarbeitet eine Anfrage zur Änderung des Usernames.
     * Format: username:newname
     *
     * @param params Array mit Parametern, wobei params[0] der neue Username ist
     */
    private void handleUsername(String[] params) {
        if (params.length < 1) {
            sendError("Kein Username angegeben");
            return;
        }
        
        String oldNickname = this.nickname;
        String requestedNickname = params[0];
        String newNickname = generateUniqueNickname(requestedNickname);
        
        if (!requestedNickname.equals(newNickname)) {
            sendMessage("info:Dein gewünschter Nickname war bereits vergeben. Neuer Nickname: " + newNickname);
        }
        
        this.nickname = newNickname;
        sendMessage("confirm:Username gesetzt zu " + this.nickname);
        broadcast("chat:User " + oldNickname + " hat seinen/ihren Username zu " + this.nickname + " geändert");
    }

    /**
     * Verarbeitet eine Lobbybeitritts Anfrage.
     * Format: lobby:code
     *
     * @param params Array mit Parametern, wobei params[0] der Lobby Code ist
     */
    private void handleLobby(String[] params) {
        if (params.length < 1) {
            sendError("Kein Lobby Code angegeben");
            return;
        }
        // TODO: Lobby implementieren
        sendMessage("confirm:Beigetreten zu Lobby " + params[0]);
    }

    /**
     * Verarbeitet eine eingehende Pong Antwort vom Client.
     * Aktualisiert den Zeitstempel der letzten Pong Nachricht.
     */
    private void handlePong() {
        lastPongTime = System.currentTimeMillis();
        System.out.println("Pong received from " + nickname);
    }

    /**
     * Verarbeitet einen Logout Befehl vom Client.
     * Akzeptierte Befehle: "logout", "ciao", "exit"
     */
    private void handleLogout() {
        running = false; 
    }

    /**
     * Trennt die Verbindung zum Client.
     * Sendet entsprechende Broadcast Nachrichten und schließt Ressourcen.
     */
    private void disconnect() {
        try {
            if (!running) {     
                broadcast("chat:User " + this.nickname + " hat sich abgemeldet");
                System.out.println("Client " + nickname + " hat sich ausgeloggt");
            } else { 
                broadcast("chat:User " + this.nickname + " wurde getrennt");
                System.out.println("Client " + nickname + " wurde getrennt");
            }

            running = false;
            clientList.remove(this);

            // close pingPong thread
            try {
                pingThread.join();
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
            
            if(in != null) in.close();
            if(out != null) out.close();
            if(clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
        } catch(IOException e) {
            System.err.println("Fehler beim Abmelden: " + e.getMessage());
        }
    }

    /**
     * Sendet eine Fehlermeldung an den Client.
     * Format: error:message
     *
     * @param errorMessage Die zu sendende Fehlermeldung
     */
    private void sendError(String errorMessage) {
        sendMessage("error:" + errorMessage);
    }

    /**
     * Sendet eine Nachricht an den Client.
     *
     * @param message Die zu sendende Nachricht
     */
    private void sendMessage(String message) {
        if (out != null && !clientSocket.isClosed()) {
            out.println(message);
        }
    }

    /**
     * Verarbeitet eine Whisper Nachricht an einen spezifischen Client.
     * Format: whisper:recipient,message
     *
     * @param params Array mit Parametern, wobei params[0] der Empfänger und params[1] die Nachricht ist
     */
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

    /**
     * Sendet eine Nachricht an alle verbundenen Clients.
     *
     * @param message Die zu broadcastende Nachricht
     */
    private void broadcast(String message) {
        for (ClientHandler client : clientList) {
            client.sendMessage(message);
        }
    }
}

