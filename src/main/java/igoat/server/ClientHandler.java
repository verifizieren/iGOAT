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
    private static final long PING_INTERVAL = 2000; // 2 seconds
    private static final long TIMEOUT = 3000; // 3 seconds

    private static final List<ClientHandler> clientList = new CopyOnWriteArrayList<>();

    /**
     * Creates a new ClientHandler for a socket connection. Automatically generates a unique
     * nickname for the client.
     *
     * @param clientSocket The socket connection to the client
     */
    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.lastPongTime = System.currentTimeMillis();
        this.nickname = generateUniqueNickname("player");
        System.out.println("New client connected as: " + this.nickname);
    }

    /**
     * Main loop for the client connection. Processes incoming messages and handles the PingPong
     * connection check.
     */
    @Override
    public void run() {
        clientList.add(this);
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Starte den Ping-Thread
            pingThread = new Thread(this::runPingPong);
            pingThread.start();

            String message;
            while (running && (message = in.readLine()) != null) {
                System.out.println("Received from " + nickname + ": " + message);

                if (message.equals("pong")) {
                    handlePong();
                    continue;
                }
                else if (message.equals("exit")) {
                    running = false;
                    break;
                }

                handleCommand(message);
            }
        } catch (IOException e) {
            System.err.println("Problem: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    /**
     * Performs the PingPong connection check. Sends periodic pings and checks for timeouts.
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
     * Processes incoming commands from the client. Format: command:message
     *
     * @param message The received message
     */
    private void handleCommand(String message) {
        try {
            int colonIndex = message.indexOf(':');
            if (colonIndex == -1) {
                sendError("Ungültige Befehlsformatierung - fehlender Doppelpunkt");
                return;
            }
            String command = message.substring(0, colonIndex).toLowerCase();
            String params = message.substring(colonIndex + 1);

            switch (command) {
                case "connect":
                    handleConnect(new String[]{params.trim()});
                    break;
                case "chat":
                    handleChat(new String[]{params});
                    break;
                case "lobby":
                    handleLobby(new String[]{params.trim()});
                    break;
                case "newlobby":
                    handleNewLobby();
                    break;
                case "username":
                    handleUsername(new String[]{params.trim()});
                    break;
                case "whisper":
                    int commaIndex = params.indexOf(',');
                    if (commaIndex != -1) {
                        String recipient = params.substring(0, commaIndex).trim();
                        String whisperMessage = params.substring(commaIndex + 1);
                        handleWhisper(new String[]{recipient, whisperMessage});
                    } else {
                        sendError("Ungültige Whisper Nachricht. Nutze: whisper:recipient,message");
                    }
                    break;
                default:
                    sendError("Unbekannter Befehl: " + command);
            }
        } catch (Exception e) {
            sendError("Fehler beim Verarbeiten des Befehls: " + e.getMessage());
        }
    }

    /**
     * Generates a unique nickname based on a base nickname. Adds _1, _2, etc. if the name is
     * already taken.
     *
     * @param baseNickname The desired base nickname
     * @return A unique nickname
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
     * Checks if a nickname is already being used by another client.
     *
     * @param nickname The nickname to check
     * @return true if the nickname is already taken, false otherwise
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
     * Processes a connect request. Format: connect:nickname
     *
     * @param params Array of parameters, where params[0] is the desired nickname
     */
    private void handleConnect(String[] params) {
        if (params.length < 1) {
            sendError("Kein Nickname angegeben");
            return;
        }

        String requestedNickname = params[0];
        this.nickname = generateUniqueNickname(requestedNickname);

        if (!requestedNickname.equals(this.nickname)) {
            sendMessage(
                "chat:Dein gewünschter Nickname war bereits vergeben. Neuer Nickname: "
                    + this.nickname);
        }

        sendMessage("confirm:" + this.nickname);
        broadcast("chat:User " + this.nickname + " connected");
    }

    /**
     * Processes a chat message.
     *
     * @param params Array of parameters, where params[0] is the message
     */
    private void handleChat(String[] params) {
        if (params.length < 1 || params[0].isEmpty()) {
            sendError("Keine Nachricht angegeben");
            return;
        }
        broadcast("chat:" + this.nickname + "," + params[0]);
    }

    /**
     * Processes a username change request. Format: username:newname
     *
     * @param params Array of parameters, where params[0] is the new username
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
            sendMessage(
                "chat:Dein gewünschter Nickname war bereits vergeben. Neuer Nickname: "
                    + newNickname);
        }

        this.nickname = newNickname;
        sendMessage("confirm:Username gesetzt zu " + this.nickname);
        broadcast(
            "chat:User "
                + oldNickname
                + " hat seinen/ihren Username zu "
                + this.nickname
                + " geändert");
    }

    /**
     * Processes a lobby join request. Format: lobby:code
     *
     * @param params Array of parameters, where params[0] is the lobby code
     */
    private void handleLobby(String[] params) {
        if (params.length < 1) {
            sendError("Kein Lobby Code angegeben");
            return;
        }
        // TODO: Lobby implementieren
        sendMessage("lobby:" + params[0]);
        sendMessage("chat:Beigetreten zu Lobby " + params[0]);
    }

    private void handleNewLobby() {
        // TODO: Lobby Creation
        int lobbyCode = 1984;
        sendMessage("lobby:" + lobbyCode);
        sendMessage("chat:Beigetreten zu Lobby " + lobbyCode);
    }

    /**
     * Processes an incoming pong response from the client. Updates the timestamp of the last pong
     * message.
     */
    private void handlePong() {
        lastPongTime = System.currentTimeMillis();
        System.out.println("Pong received from " + nickname);
    }

    /**
     * Processes a logout command from the client.
     */
    private void handleLogout() {
        running = false;
    }

    /**
     * Disconnects the client. Sends appropriate broadcast messages and closes resources.
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

            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Fehler beim Abmelden: " + e.getMessage());
        }
    }

    /**
     * Sends an error message to the client. Format: error:message
     *
     * @param errorMessage The error message to send
     */
    private void sendError(String errorMessage) {
        sendMessage("error:" + errorMessage);
    }

    /**
     * Sends a message to the client.
     *
     * @param message The message to send
     */
    private void sendMessage(String message) {
        if (out != null && !clientSocket.isClosed()) {
            out.println(message);
        }
    }

    /**
     * Processes a whisper message to a specific client. Format: whisper:recipient,message
     *
     * @param params Array of parameters, where params[0] is the recipient and params[1] is the
     *               message
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
                client.sendMessage("chat:[" + this.nickname + "  whispered] " + message);
                return;
            }
        }
        sendError("User " + recipient + " nicht gefunden");
    }

    /**
     * Sends a message to all connected clients.
     *
     * @param message The message to broadcast
     */
    private void broadcast(String message) {
        for (ClientHandler client : clientList) {
            client.sendMessage(message);
        }
    }
}
