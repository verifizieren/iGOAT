package igoat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {

    // Constants for UDP Auto-Registration
    public static final int SERVER_UDP_LISTENING_PORT = 61001; // Fixed port server listens on for UDP registration
    private static final String UDP_REGISTRATION_PREFIX = "register_udp:";
    private static final int UDP_BUFFER_SIZE = 512;

    private final Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread pingThread;
    private String nickname;
    private volatile boolean running = true;
    private long lastPongTime;
    private static final long PING_INTERVAL = 2000; // 2 seconds
    private static final long TIMEOUT = 3000; // 3 seconds
    private int udpPort = -1; // Port the client is listening on for UDP updates

    private static final List<ClientHandler> clientList = new CopyOnWriteArrayList<>();

    private static final List<Lobby> lobbyList = new CopyOnWriteArrayList<>();
    private Lobby currentLobby;
    private static int nextLobbyCode = 1000;

    private boolean isDown = false;
    private boolean isCaught = false;

    private int role;

    private static DatagramSocket serverUpdateSocket;

    private static DatagramSocket udpListeningSocket;
    private static volatile boolean udpListenerRunning = false;
    private static Thread udpListenerThread;

    static {
        try {
            serverUpdateSocket = new DatagramSocket();
            System.out.println("Server UDP sending socket created on port: " + serverUpdateSocket.getLocalPort());
        } catch (SocketException e) {
            System.err.println("FATAL: Could not create server UDP sending socket: " + e.getMessage());
            serverUpdateSocket = null;
        }
    }

    /**
     * Starts the static UDP listener thread if it's not already running.
     * Should be called once during server initialization.
     */
    public static synchronized void startUdpListener() {
        if (udpListenerRunning || udpListenerThread != null) {
            System.out.println("UDP Listener is already running or was not properly stopped.");
            return;
        }
        try {
            udpListeningSocket = new DatagramSocket(SERVER_UDP_LISTENING_PORT);
            udpListenerRunning = true;
            udpListenerThread = new Thread(ClientHandler::runUdpListenerLoop);
            udpListenerThread.setName("Server-UDP-Listener");
            udpListenerThread.setDaemon(true);
            udpListenerThread.start();
            System.out.println("Server UDP Listener started on port: " + SERVER_UDP_LISTENING_PORT);
        } catch (SocketException e) {
            System.err.println("FATAL: Could not start UDP Listener on port " + SERVER_UDP_LISTENING_PORT + ": " + e.getMessage());
            udpListenerRunning = false;
            udpListeningSocket = null;
            udpListenerThread = null;
        }
    }

    /**
     * Stops the static UDP listener thread.
     * Should be called once during server shutdown.
     */
    public static synchronized void stopUdpListener() {
        if (!udpListenerRunning) {
            System.out.println("UDP Listener is not running.");
            return;
        }
        udpListenerRunning = false;
        if (udpListeningSocket != null && !udpListeningSocket.isClosed()) {
            udpListeningSocket.close();
        }
        if (udpListenerThread != null) {
            try {
                udpListenerThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("Server UDP Listener stopped.");
        udpListeningSocket = null;
        udpListenerThread = null;
    }

    /**
     * The main loop for the UDP listener thread.
     * Receives UDP packets and processes registration requests.
     */
    private static void runUdpListenerLoop() {
        byte[] buffer = new byte[UDP_BUFFER_SIZE];
        while (udpListenerRunning && udpListeningSocket != null && !udpListeningSocket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpListeningSocket.receive(packet);

                InetAddress clientIp = packet.getAddress();
                int clientUdpPort = packet.getPort();
                String message = new String(packet.getData(), 0, packet.getLength()).trim();

                if (message.startsWith(UDP_REGISTRATION_PREFIX)) {
                    String clientNickname = message.substring(UDP_REGISTRATION_PREFIX.length());
                    registerClientUdpPort(clientIp, clientNickname, clientUdpPort);
                }

            } catch (SocketException se) {
                if (udpListenerRunning) {
                    System.err.println("UDP Listener SocketException (might be expected on shutdown): " + se.getMessage());
                }
            } catch (IOException e) {
                if (udpListenerRunning) {
                    System.err.println("UDP Listener IOException: " + e.getMessage());
                }
            }
        }
        System.out.println("UDP Listener thread exiting.");
    }

    /**
     * Finds the ClientHandler associated with the given IP and nickname and updates its UDP port.
     *
     * @param clientIp The IP address of the client.
     * @param nickname The nickname reported by the client.
     * @param clientUdpPort The source UDP port from the registration packet.
     */
    private static void registerClientUdpPort(InetAddress clientIp, String nickname, int clientUdpPort) {
        boolean found = false;
        for (ClientHandler handler : clientList) {
            if (handler.clientSocket != null && !handler.clientSocket.isClosed() &&
                handler.clientSocket.getInetAddress().equals(clientIp) &&
                handler.nickname.equals(nickname)) {

                handler.udpPort = clientUdpPort;
                found = true;
                System.out.println("UDP registered for " + nickname);

                if (serverUpdateSocket != null) {
                    try {
                        String ackMsg = "udp_ack:";
                        byte[] ackBuf = ackMsg.getBytes();
                        DatagramPacket ackPacket = new DatagramPacket(ackBuf, ackBuf.length, clientIp, clientUdpPort);
                        serverUpdateSocket.send(ackPacket);
                    } catch (IOException e) {
                        // Silently handle ack sending failures
                    }
                }
                break;
            }
        }
        if (!found) {
            System.err.println("UDP registration failed for " + nickname);
        }
    }

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
                case "getlobbies":
                    handleGetLobbies();
                    break;
                case "lobbychat":
                    if (currentLobby != null) {
                        currentLobby.broadcastToLobby("chat:" + nickname + " " + params);
                    } else {
                        sendError("Du bist in keiner Lobby");
                    }
                    break;
                case "getplayers":
                    handleGetPlayers();
                    break;
                case "getlobbyplayers":
                    handleGetLobbyPlayers();
                    break;
                case "ready":
                    handleReady();
                    break;
                case "role":
                    handleRoleConfirmation(params.trim());
                    break;
                case "catch":
                    handleCatch(params.trim());
                    break;
                case "revive":
                    handleRevive(params.trim());
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
                case "udp_bcast":
                    if (currentLobby != null) {
                        currentLobby.broadcastUpdateToLobby("UDP Broadcast Test: " + params);
                        sendMessage("chat:Sent UDP broadcast to lobby " + currentLobby.getCode());
                    } else {
                        sendError("You are not in a lobby to broadcast UDP.");
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
            sendMessage("lobby:0");
            return;
        }

        int code;
        try{
            code = Integer.parseInt(params[0]);
        } catch(NumberFormatException e){
            sendError("Ungültiger Code angegeben");
            sendMessage("lobby:0");
            return;
        }

        if (code == 0) {
            leaveCurrentLobby();
            sendMessage("lobby:0");
            return;
        }

        Lobby lobbyToJoin = null;
        for (Lobby lobby : lobbyList) {
            if (lobby.getCode() == code) {
                lobbyToJoin = lobby;
                break;
            }
        }

        if (lobbyToJoin == null) {
            sendError("lobby mit Code " + code + " nicht gefunden");
            sendMessage("lobby:0");
            return;
        }

        if (lobbyToJoin.isFull()) {
            sendError("Die Lobby " + code + " ist voll");
            sendMessage("lobby:0");
            return;
        }

        leaveCurrentLobby();

        currentLobby = lobbyToJoin;
        currentLobby.addMember(this);

        sendMessage("lobby:" + code);
        currentLobby.broadcastToLobby("chat:" + nickname + " ist der Lobby " + code + " beigetreten");
    }


    private void handleNewLobby() {
        int code = nextLobbyCode++;
        Lobby newLobby = new Lobby(code);
        lobbyList.add(newLobby);

        leaveCurrentLobby();

        currentLobby = newLobby;
        currentLobby.addMember(this);

        sendMessage("lobby:" + code);
        currentLobby.broadcastToLobby("chat:" + nickname + " hat eine neue Lobby erstellt (" + code + ")");
    }

    private void leaveCurrentLobby() {
        if (currentLobby != null) {
            currentLobby.removeMember(this);
            currentLobby.broadcastToLobby(("chat:" + nickname + " hat die Lobby verlassen"));

            if (currentLobby.getMembers().isEmpty()) {
                lobbyList.remove(currentLobby);
            }

            currentLobby = null;
        }
    }

    private void handleGetLobbies() {
        if (lobbyList.isEmpty()) {
            sendMessage("lobbies:");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (Lobby lobby : lobbyList) {
            sb.append(lobby.getCode())
                .append("=")
                .append(lobby.getMembers().size())
                .append(",");
        }

        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }

        sendMessage("lobbies:" + sb.toString());
    }

    private void handleGetPlayers() {
        StringBuilder sb = new StringBuilder();
        for (ClientHandler client : clientList) {
            sb.append(client.nickname).append(",");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1); // remove last comma
        }
        sendMessage("players:" + sb);
    }

    private void handleGetLobbyPlayers() {
        if (currentLobby == null) {
            sendMessage("lobbyplayers:");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (ClientHandler member : currentLobby.getMembers()) {
            sb.append(member.nickname).append(",");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        sendMessage("lobbyplayers:" + sb);
    }

    private void handleReady() {
        if (currentLobby == null) {
            sendError("Du bist in keiner Lobby");
            return;
        }

        appendToLobbyChat("Player " + nickname + " ist bereit.");

        int assignRole = assignRole();
        sendMessage("role:" + assignRole);
    }

    private int assignRole() {
        return (int) (Math.random() * 3);
    }

    private void handleRoleConfirmation(String roleString) {
        try {
            int parsedRole = Integer.parseInt(roleString);
            this.role = parsedRole;
            appendToLobbyChat("Player " + nickname + " hat Rolle " + role + " erhalten");
        } catch (NumberFormatException e){
            sendError("Ungültige Rolle");
        }
    }

    private void appendToLobbyChat(String message) {
        if (currentLobby != null) {
            currentLobby.broadcastToLobby("chat:" + nickname + " " + message);
        }
    }

    private void handleCatch(String targetName) {
        ClientHandler target = findPlayer(targetName);
        if (target == null) {
            sendError("Spieler " + targetName + " nicht gefunden.");
            return;
        }

        if (this.role != 2) {
            sendError("Nur Wächter dürfen fangen.");
            return;
        }

        if (!isInRange(this, target)) {
            sendError("Spieler " + targetName + " nicht in reichweite.");
            return;
        }

        if (target.isCaught()) {
            sendError("Spieler " + targetName + " ist bereits gefangen");
            return;
        }

        target.setCaught(true);
        broadcast("catch:" + targetName);
    }

    private  void handleRevive(String targetName) {
        ClientHandler target = findPlayer(targetName);
        if (target == null) {
            sendError("Spieler " + targetName + " nicht gefunden.");
            return;
        }

        if (this.role != 0) {
            sendError("Nur Ziegen dürfen Roboter neu starten.");
            return;
        }

        if (target.getRole() != 1 || !target.isDown()) {
            sendError("Ziel ist kein ausgeschalltener ");
            return;
        }

        if (!isInRange(this, target)) {
            sendError("Spieler " + targetName + " nicht in reichweite.");
            return;
        }

        target.setDown(false);
        broadcast("revive:" + targetName);
    }

    public boolean isCaught() {
        return isCaught;
    }

    public void setCaught(boolean caught) {
        this.isCaught = isCaught;
    }

    public boolean isDown() {
        return isDown;
    }

    public void setDown(boolean down) {
        this.isDown = isDown;
    }

    public int getRole() {
        return role;
    }

    public String getNickname() {
        return nickname;
    }

    private ClientHandler findPlayer(String name) {
        for (ClientHandler client : clientList) {
            if (client.nickname.equals(name)) {
                return client;
            }
        }
        return null;
    }

    private boolean isInRange(ClientHandler client, ClientHandler target) {
        // to be implemented
        return true;
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
            leaveCurrentLobby();

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
    void sendMessage(String message) {
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

    /**
     * Sends a UDP update message to this specific client.
     * Assumes the client has already sent its UDP listening port via the 'udp_port' command.
     *
     * @param message The message string to send.
     */
    public void sendUpdate(String message) {
        if (serverUpdateSocket == null || udpPort == -1 || 
            clientSocket == null || clientSocket.isClosed()) {
            return;
        }

        try {
            byte[] buffer = message.getBytes();
            InetAddress clientAddress = clientSocket.getInetAddress();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, clientAddress, this.udpPort);
            serverUpdateSocket.send(packet);
        } catch (IOException e) {
            if (running) {
                System.err.println("UDP send failed to " + nickname + ": " + e.getMessage());
            }
        }
    }
}
