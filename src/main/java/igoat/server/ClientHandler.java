package igoat.server;

import igoat.Role;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles individual client connections in the game server.
 * Each instance manages one client's TCP connection and UDP communication,
 * handling game state updates, chat messages, and player actions.
 * Implements Runnable to run in its own thread.
 */
public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    
    // Constants for UDP Auto-Registration
    /** Fixed port that the server listens on for UDP client registration */
    public static final int SERVER_UDP_LISTENING_PORT = 61001;
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

    private boolean isReady = false;
    private boolean isDown = false;
    private boolean isCaught = false;

    private int reportedTerminalCount = -1;

    private Role role;

    // Add role mapping
    private static final Map<String, Role> roleMap = new ConcurrentHashMap<>();
    public HashMap<String, Role> roles = new HashMap<>();
    
    // Add list for available roles (0 = goat, 1 = robot, 2 = guard)
    private static final List<Role> availableRoles = new CopyOnWriteArrayList<>();
    private static final Role[] INITIAL_ROLES = {Role.GUARD, Role.IGOAT, Role.IGOAT, Role.GOAT}; // 1 goat, 2 igoat, 1 guard

    private static DatagramSocket serverUpdateSocket;

    private static DatagramSocket udpListeningSocket;
    private static volatile boolean udpListenerRunning = false;
    private static Thread udpListenerThread;

    static {
        try {
            serverUpdateSocket = new DatagramSocket();
            logger.info("======= SERVER UDP SETUP =======");
            logger.info("Server UDP sending socket created on port {}", serverUpdateSocket.getLocalPort());
            logger.info("UDP listening port configuration {}", SERVER_UDP_LISTENING_PORT);
            logger.info("===============================");
        } catch (SocketException e) {
            logger.error("Could not create server UDP sending socket", e);
            serverUpdateSocket = null;
        }
    }

    /**
     * Starts the static UDP listener thread if it's not already running.
     * Should be called once during server initialization.
     */
    public static synchronized void startUdpListener() {
        logger.info("======= STARTING UDP LISTENER =======");
        
        if (udpListenerRunning || udpListenerThread != null) {
            logger.warn("UDP Listener is already running or was not properly stopped.");
            return;
        }
        
        try {
            logger.info("Creating UDP listening socket on port " + SERVER_UDP_LISTENING_PORT);
            udpListeningSocket = new DatagramSocket(SERVER_UDP_LISTENING_PORT);
            logger.info("UDP listening socket created successfully");
            
            udpListenerRunning = true;
            udpListenerThread = new Thread(ClientHandler::runUdpListenerLoop);
            udpListenerThread.setName("Server-UDP-Listener");
            udpListenerThread.setDaemon(true);
            udpListenerThread.start();
            
            logger.info("Server UDP Listener thread started");
            logger.info("================================");
        } catch (SocketException e) {
            logger.error("Could not start UDP Listener on port {}", SERVER_UDP_LISTENING_PORT, e);
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
            logger.warn("UDP Listener is not running.");
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
        logger.info("Server UDP Listener stopped.");
        udpListeningSocket = null;
        udpListenerThread = null;
    }

    /**
     * Static method to run a UDP listener loop for position updates.
     * This is run in its own thread.
     */
    private static void runUdpListenerLoop() {
        if (serverUpdateSocket == null) {
            logger.error("Cannot run UDP listener - socket is null");
            return;
        }

        logger.info("UDP Listener starting on port {}", SERVER_UDP_LISTENING_PORT);
        
        if (udpListeningSocket == null) {
            logger.error("udpListeningSocket is null!");
            return;
        }
        
        logger.info("UDP Listener ready on local port {}", udpListeningSocket.getLocalPort());
        logger.info("Waiting for incoming UDP packets...");
        
        byte[] buffer = new byte[UDP_BUFFER_SIZE];
        
        try {
            udpListeningSocket.setSoTimeout(10);
        } catch (SocketException e) {
            logger.error("Could not set socket timeout", e);
        }
        
        while (udpListenerRunning && udpListeningSocket != null && !udpListeningSocket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                
                udpListeningSocket.receive(packet);
                
                InetAddress clientIp = packet.getAddress();
                int sourcePort = packet.getPort();
                String message = new String(packet.getData(), 0, packet.getLength());

                //logger.info("Received: {} from {}:{}", message, clientIp, sourcePort);
                
                if (message.startsWith(UDP_REGISTRATION_PREFIX)) {
                    String[] parts = message.split(":");
                    if (parts.length == 3) {
                        String nickname = parts[1];
                        try {
                            int clientListeningPort = Integer.parseInt(parts[2]);
                            registerClientUdpPort(clientIp, nickname, clientListeningPort);
                        } catch (NumberFormatException e) {
                            logger.error("Invalid port in registration: {}", parts[2]);
                        }
                    }
                } else if (message.startsWith("position:")) {
                    handlePositionUpdate(message);
                }
                
                Arrays.fill(buffer, (byte) 0);
                
            } catch (java.net.SocketTimeoutException e) {
            } catch (SocketException se) {
                if (udpListenerRunning) {
                    logger.error("Socket exception", se);
                }
            } catch (IOException e) {
                if (udpListenerRunning) {
                    logger.error("IO exception", e);
                }
            } catch (Exception e) {
                logger.error("Unexpected exception", e);
            }
        }
        logger.info("UDP listener stopped");
    }

    /**
     * Handles a position update from a client.
     * Format: position:playerName:lobbyCode:x:y
     *
     * @param message The position update message
     */
    private static void handlePositionUpdate(String message) {
        String[] parts = message.split(":");
        if (parts.length != 5) {
            logger.warn("Invalid position format: {}", message);
            return;
        }
        
        try {
            String senderName = parts[1];
            String lobbyCode = parts[2];
            int x = Integer.parseInt(parts[3]);
            int y = Integer.parseInt(parts[4]);
            
            final ClientHandler sender = findClientHandlerByNickname(senderName);
            
            if (sender == null) {
                logger.warn("Cannot find player: {}", senderName);
                return;
            }
            
            if (sender.currentLobby != null) {
                String currentLobbyCode = String.valueOf(sender.currentLobby.getCode());
                if (!currentLobbyCode.equals(lobbyCode)) {
                    logger.warn("Lobby code mismatch for {}", senderName);
                    return;
                }
                
                String broadcastMessage = "player_position:" + senderName + ":" + x + ":" + y;
                
                sender.currentLobby.broadcastUpdateToLobby(broadcastMessage, sender);
            } else {
                logger.warn("Player {} not in a lobby", senderName);
            }
        } catch (NumberFormatException e) {
            logger.error("Invalid coordinates", e);
        } catch (Exception e) {
            logger.error("Error processing update: ", e);
        }
    }

    /**
     * Finds the ClientHandler associated with the given IP and nickname and updates its UDP port.
     *
     * @param clientIp The IP address of the client.
     * @param nickname The nickname reported by the client.
     * @param clientListeningPort The actual UDP port the client is listening on (from the message).
     */
    private static void registerClientUdpPort(InetAddress clientIp, String nickname, int clientListeningPort) {
        logger.info("Attempting to register UDP for {} from IP {} using reported listening port {}", nickname, clientIp, clientListeningPort);
        boolean found = false;
        for (ClientHandler handler : clientList) {
            if (handler.nickname.equals(nickname)) {

                handler.udpPort = clientListeningPort;
                found = true;
                logger.info("Successfully registered UDP listening port {} for {}",clientListeningPort, nickname);

                if (serverUpdateSocket != null) {
                    try {
                        String ackMsg = "udp_ack:";
                        byte[] ackBuf = ackMsg.getBytes();
                        DatagramPacket ackPacket = new DatagramPacket(ackBuf, ackBuf.length, clientIp, clientListeningPort);
                        logger.info("Sending UDP ACK to {} at {}:{}", nickname, clientIp, clientListeningPort);
                        serverUpdateSocket.send(ackPacket);
                    } catch (IOException e) {
                        logger.error("Failed to send UDP ACK to {}", nickname, e);
                    }
                }
                break;
            }
        }
        if (!found) {
            logger.error("UDP registration failed for {} (Handler not found or IP/name mismatch)", nickname);
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
        logger.info("New client connected as {}", this.nickname);
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
                //logger.info("Received from {}: {}", nickname, message);

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
            logger.error("Exception", e);
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
                //logger.info("Ping sent to {}", nickname);
            }

            if (currentTime - lastPongTime >= TIMEOUT) {
                logger.info("Client {} timed out", nickname);
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
                        currentLobby.broadcastChatToLobby(nickname + ":" + params);
                    } else {
                        sendError("Du bist in keiner Lobby");
                    }
                    break;
                case "teamchat": // WORK IN PROGRESS
                    if (currentLobby != null) {
                        broadcast(nickname + ":" + params);
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
                        currentLobby.broadcastUpdateToLobby("UDP Broadcast Test: " + params, this);
                        sendMessage("chat:Sent UDP broadcast to lobby " + currentLobby.getCode());
                    } else {
                        sendError("You are not in a lobby to broadcast UDP.");
                    }
                    break;
                case "startgame":
                    handleStartGame();
                    break;
                case "terminal":
                    handleTerminalActivation(params.trim());
                    break;
                case "getroles":
                    handleGetRoles();
                    break;
                case "mapinfo":
                    handleMapInfo(params.trim());
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
        broadcastGlobalPlayerList();
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
        broadcast("chat:" + this.nickname + ":" + params[0]);
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

        broadcastGlobalPlayerList();
        if (currentLobby != null) broadcastLobbyPlayerList();
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

        if (currentLobby != null && code != 0) {
            sendError("Du bist bereits in einem Lobby");
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
        currentLobby.broadcastChatToLobby(nickname + " ist der Lobby " + code + " beigetreten");

        broadcastGetLobbiesToAll();
        broadcastLobbyPlayerList();
    }


    private void handleNewLobby() {
        if (currentLobby != null) {
            sendError("Du bist bereits in einem Lobby");
            return;
        }

        int code = nextLobbyCode++;
        Lobby newLobby = new Lobby(code);
        lobbyList.add(newLobby);

        leaveCurrentLobby();

        currentLobby = newLobby;
        currentLobby.addMember(this);

        sendMessage("lobby:" + code);
        currentLobby.broadcastChatToLobby(nickname + " hat eine neue Lobby erstellt (" + code + ")");

        broadcastGetLobbiesToAll();
        broadcastLobbyPlayerList();
    }

    private void leaveCurrentLobby() {
        if (currentLobby != null) {
            currentLobby.removeMember(this);
            currentLobby.broadcastChatToLobby(nickname + " hat die Lobby verlassen");

            if (currentLobby.getMembers().isEmpty()) {
                lobbyList.remove(currentLobby);
            }
            broadcastGetLobbiesToAll();
            broadcastLobbyPlayerList();
            currentLobby = null;
        }
    }

    private void handleGetLobbies() {
        if (lobbyList.isEmpty()) {
            sendMessage("getlobbies:");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (Lobby lobby : lobbyList) {
            sb.append(lobby.getCode())
                .append("=")
                .append(lobby.getMembers().size())
                .append("/")
                .append(Lobby.MAX_PLAYERS)
                .append(" [")
                .append(lobby.getState().toString().toLowerCase())
                .append("],");
        }


        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }

        sendMessage("getlobbies:" + sb.toString());
    }

    private void handleGetPlayers() {
        StringBuilder sb = new StringBuilder();
        for (ClientHandler client : clientList) {
            sb.append(client.nickname).append(",");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        sendMessage("getplayers:" + sb);
    }

    private void handleGetLobbyPlayers() {
        if (currentLobby == null) {
            sendMessage("getlobbyplayers:");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (ClientHandler member : currentLobby.getMembers()) {
            sb.append(member.nickname).append(",");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        sendMessage("getlobbyplayers:" + sb);
    }

    private void handleReady() {
        if (currentLobby == null) {
            sendError("Du bist in keiner Lobby");
            return;
        }

        this.setReady(true); 
        logger.info("Player {} set status to ready.", nickname);

        String statusMessage = "ready_status:" + this.nickname + "," + this.isReady;
        currentLobby.broadcastToLobby(statusMessage);
        logger.info("Broadcasting ready status to lobby {}: {}", currentLobby.getCode(), statusMessage);

        appendToLobbyChat("Player " + nickname + " ist bereit.", false);

        // Assign role when ready
        Role assignedRole = assignRole();
        this.role = assignedRole;
        roleMap.put(nickname, assignedRole);

        // Broadcast role assignment
        String roleMessage = "role:" + nickname + ":" + assignedRole;
        currentLobby.broadcastToLobby(roleMessage); //delete later
        appendToLobbyChat("Player " + nickname + " wurde Rolle " + assignedRole + " zugewiesen", false);
    }

    private Role assignRole() {
        synchronized (availableRoles) {
            if (availableRoles.isEmpty()) {
                // Reset the roles list if empty
                availableRoles.addAll(Arrays.asList(INITIAL_ROLES[0], INITIAL_ROLES[1], INITIAL_ROLES[2], INITIAL_ROLES[3]));
            }
            
            int randomIndex = (int)(Math.random() * availableRoles.size());
            Role selectedRole = availableRoles.remove(randomIndex);
            return selectedRole;
        }
    }

    private void handleRoleConfirmation(String roleString) {
        try {
            Role parsedRole = Role.valueOf(roleString);
            this.role = parsedRole;
            roleMap.put(nickname, parsedRole);
            appendToLobbyChat("Player " + nickname + " hat Rolle " + role + " erhalten", false);
        } catch (NumberFormatException e){
            logger.error("Invalid Role {}",roleString ,e);
            sendError("Ungültige Rolle");
        }
    }

    private void appendToLobbyChat(String message, boolean chatMSG) {
        if (currentLobby == null) {
            logger.error("Couldn't find lobby");
            return;
        }

        if (chatMSG) {
            currentLobby.broadcastChatToLobby(nickname + " " + message);
        } else {
            currentLobby.broadcastChatToLobby(message);
        }
    }

    private void handleCatch(String targetName) {
        ClientHandler target = findPlayer(targetName);
        if (target == null) {
            sendError("Spieler " + targetName + " nicht gefunden.");
            return;
        }

        if (this.role != Role.GUARD) {
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
        target.setDown(true);
        broadcast("catch:" + targetName);
    }

    private  void handleRevive(String targetName) {
        ClientHandler target = findPlayer(targetName);
        if (target == null) {
            sendError("Spieler " + targetName + " nicht gefunden.");
            return;
        }

        if (this.role != Role.GOAT) {
            sendError("Nur Ziegen dürfen Roboter neu starten.");
            return;
        }

        if (target.getRole() != Role.IGOAT || !target.isDown()) {
            sendError("Ziel ist kein ausgeschaltener ");
            return;
        }

        if (!isInRange(this, target)) {
            sendError("Spieler " + targetName + " nicht in reichweite.");
            return;
        }

        target.setDown(false);
        broadcast("revive:" + targetName);
    }

    /**
     * Checks if the player has been caught.
     * 
     * @return true if the player has been caught, false otherwise
     */
    public boolean isCaught() {
        return isCaught;
    }

    /**
     * Sets the caught status of the player.
     * 
     * @param caught true to mark the player as caught, false otherwise
     */
    public void setCaught(boolean caught) {
        this.isCaught = caught;
    }

    /**
     * Checks if the player is currently down (unable to move).
     * 
     * @return true if the player is down, false otherwise
     */
    public boolean isDown() {
        return isDown;
    }

    /**
     * Sets whether the player is down (unable to move).
     * 
     * @param down true to mark the player as down, false otherwise
     */
    public void setDown(boolean down) {
        this.isDown = down;
    }

    /**
     * Gets the client's role in the game.
     * 
     * @return The current role of the client (e.g., hunter or runner)
     */
    public Role getRole() {
        return role;
    }

    /**
     * Checks if the player is ready to start the game.
     * 
     * @return true if the player is ready, false otherwise
     */
    public boolean isReady() {
        return isReady;
    }

    /**
     * Sets whether the player is ready to start the game.
     * 
     * @param ready true to mark the player as ready, false otherwise
     */
    public void setReady(boolean ready) {
        this.isReady = ready;
    }

    /**
     * Gets the client's nickname.
     * 
     * @return The current nickname of the client
     */
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
        //logger.info("Pong received from {}", nickname);
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
                logger.info("Client {} hat sich ausgeloggt", nickname);
            } else {
                broadcast("chat:User " + this.nickname + " wurde getrennt");
                logger.info("Client {} wurde getrennt", nickname);
            }
            broadcastGlobalPlayerList();

            running = false;
            clientList.remove(this);

            // close pingPong thread
            try {
                pingThread.join();
            } catch (InterruptedException e) {
                logger.error("Couldn't close ping Thread.", e);
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
            logger.error("Error when loggin out", e);
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

    private static void broadcastGetLobbiesToAll() {
        StringBuilder sb = new StringBuilder();
        for (Lobby lobby : lobbyList) {
            sb.append(lobby.getCode())
                .append("=")
                .append(lobby.getMembers().size())
                .append("/")
                .append(Lobby.MAX_PLAYERS)
                .append(" [")
                .append(lobby.getState().toString().toLowerCase())
                .append("],");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        String message = "getlobbies:" + sb.toString();
        for (ClientHandler client : clientList) {
            client.sendMessage(message);
        }
    }

    private static void broadcastGlobalPlayerList() {
        StringBuilder sb = new StringBuilder();
        for (ClientHandler client : clientList) {
            sb.append(client.getNickname()).append(",");
        }
        if (sb.length() > 0) sb.setLength(sb.length() - 1);

        String update = "getplayers:" + sb.toString();
        for (ClientHandler client : clientList) {
            client.sendMessage(update);
        }
    }

    private void broadcastLobbyPlayerList() {
        if (currentLobby == null)
            return;

        StringBuilder sb = new StringBuilder();
        for (ClientHandler member : currentLobby.getMembers()) {
            sb.append(member.getNickname()).append(",");
        }
        if (sb.length() > 0) sb.setLength(sb.length() - 1);

        String update = "getlobbyplayers:" + sb.toString();
        for (ClientHandler member : currentLobby.getMembers()) {
            member.sendMessage(update);
        }
    }

    /**
     * Sends a UDP update message to this specific client.
     *
     * @param message The message string to send.
     */
    public void sendUpdate(String message) {
        // Skip sending if prerequisites aren't met
        if (serverUpdateSocket == null || udpPort == -1 || 
            clientSocket == null || clientSocket.isClosed()) {
            return;
        }
        
        try {
            byte[] buffer = message.getBytes();
            InetAddress clientAddress = clientSocket.getInetAddress();
            DatagramPacket packet = new DatagramPacket(
                buffer, buffer.length, 
                clientAddress, udpPort
            );

            //logger.info("Sending: {} to {} at {}:{}", message, nickname, clientAddress.getHostAddress(), udpPort);

            serverUpdateSocket.send(packet);
        } catch (IOException e) {
            logger.error("Send failed", e);
        }
    }

    /**
     * Handles the start game command from a client.
     * Only the lobby creator can start the game.
     */
    private void handleStartGame() {
        if (currentLobby == null) {
            sendError("Du bist in keiner Lobby");
            return;
        }

        boolean allReadyCheck = true;
        if (currentLobby.getMembers().isEmpty() || currentLobby.getMembers().size() < 1) {
            logger.info("Warning: Starting game with < 1 player in lobby {}", currentLobby.getCode());
            allReadyCheck = true;
        } else {
             for (ClientHandler member : currentLobby.getMembers()) {
                if (!member.isReady()) {
                    allReadyCheck = false;
                    break;
                }
            }
        }

        if (!allReadyCheck) {
             sendError("Nicht alle Spieler sind bereit.");
            logger.info("Start game requested for lobby {} but not all players are ready.", currentLobby.getCode());
            return;
        }

        if (currentLobby.getMembers().isEmpty() || currentLobby.getMembers().get(0) != this) {
             sendError("Nur der Lobby-Ersteller kann das Spiel starten.");
             logger.info("Start game requested by non-creator ({}) for lobby {}. Denied.", nickname, currentLobby.getCode());
             return;
        }

        logger.info("Starting game in lobby {} (Initiated by creator: {})", currentLobby.getCode(), nickname);

        currentLobby.setState(Lobby.GameState.IN_GAME);
        
        if (this.reportedTerminalCount >= 0) {
            currentLobby.setTotalTerminalsInMap(this.reportedTerminalCount);
            logger.info("Lobby {}: Set required terminals to {} (reported by client {})", 
                        currentLobby.getCode(), this.reportedTerminalCount, this.nickname);
        } else {
            logger.error("Lobby {}: Client {} did not report map info before game start. Using default terminal count 3.", 
                         currentLobby.getCode(), this.nickname);
            currentLobby.setTotalTerminalsInMap(8);
        }

        String gameStartedMessage = "game_started:";
        currentLobby.broadcastToLobby(gameStartedMessage);
        logger.info("Broadcasted: {}", gameStartedMessage);

        sendMessage(gameStartedMessage);
        
        // Reset available roles when game starts
        synchronized (availableRoles) {
            availableRoles.clear();
            availableRoles.addAll(Arrays.asList(INITIAL_ROLES[0], INITIAL_ROLES[1], INITIAL_ROLES[2], INITIAL_ROLES[3]));
        }
    }

    /**
     * Helper method to find a ClientHandler by nickname.
     * 
     * @param nickname The nickname to search for
     * @return The ClientHandler with the matching nickname, or null if not found
     */
    private static ClientHandler findClientHandlerByNickname(String nickname) {
        for (ClientHandler handler : clientList) {
            if (handler.nickname.equals(nickname)) {
                return handler;
            }
        }
        return null;
    }

    /**
     * Gets the UDP port for this client.
     * 
     * @return The UDP port registered for this client.
     */
    public int getUdpPort() {
        return udpPort;
    }

    private void handleGetRoles() {
        if (currentLobby == null) {
            sendError("Du bist in keiner Lobby");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (ClientHandler member : currentLobby.getMembers()) {
            Role role = roleMap.get(member.getNickname());
            if (role != null) {
                sb.append(member.getNickname()).append("=").append(role).append(",");
            }
        }
        if (!sb.isEmpty()) {
            sb.setLength(sb.length() - 1);
        }
        sendMessage("roles:" + sb.toString());
    }

    /**
     * Handles the activation of a terminal by a player.
     * Parses the terminal ID, activates it in the lobby, and broadcasts the event.
     * 
     * @param params The terminal ID sent by the client.
     */
    private void handleTerminalActivation(String params) {
        if (currentLobby == null) {
            sendError("You must be in a lobby to activate a terminal.");
            return;
        }

        try {
            int terminalId = Integer.parseInt(params);
            logger.info("Player {} attempting to activate terminal {} in lobby {}", nickname, terminalId, currentLobby.getCode());

            boolean newlyActivated = currentLobby.activateTerminal(terminalId);

            if (newlyActivated) {
                String activationMessage = "terminal:" + terminalId;
                currentLobby.broadcastToLobby(activationMessage);
                logger.info("Broadcasted terminal activation: {} to lobby {}", activationMessage, currentLobby.getCode());
            } else {
                sendMessage("chat:System:Terminal " + terminalId + " was already activated.");
            }

        } catch (NumberFormatException e) {
            sendError("Invalid terminal ID format: " + params);
            logger.warn("Invalid terminal ID received from {}: {}", nickname, params);
        } catch (Exception e) {
            sendError("Error processing terminal activation.");
            logger.error("Error during terminal activation for player {} in lobby {}", nickname, currentLobby.getCode(), e);
        }
    }

    /**
     * Handles the map information (e.g., terminal count) sent by the client.
     * 
     * @param params The parameters containing map info (currently just terminal count).
     */
    private void handleMapInfo(String params) {
        try {
            int count = Integer.parseInt(params);
            if (count >= 0) {
                this.reportedTerminalCount = count;
                logger.info("Received map info from {}: Terminal count = {}", nickname, count);
            } else {
                logger.warn("Received invalid terminal count from {}: {}", nickname, params);
            }
        } catch (NumberFormatException e) {
            logger.error("Invalid map info format received from {}: {}", nickname, params);
            sendError("Invalid map info format: " + params);
        }
    }
}
