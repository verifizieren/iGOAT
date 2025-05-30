package igoat.server;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import igoat.Role;
import igoat.client.LanguageManager;
import igoat.client.Map;
import igoat.client.Wall;
import igoat.server.Lobby.LobbyState;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles individual client connections in the game server. Each instance manages one client's TCP
 * connection and UDP communication, handling game state updates, chat messages, and player actions.
 * Implements Runnable to run in its own thread.
 */
public class ClientHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private static final LanguageManager lang = LanguageManager.getInstance();

    // Constants for UDP Auto-Registration
    /**
     * Fixed port that the server listens on for UDP client registration
     */
    public static final int SERVER_UDP_LISTENING_PORT = 61001;
    private static final String UDP_REGISTRATION_PREFIX = "register_udp:";
    private static final int UDP_BUFFER_SIZE = 512;
    private static final int MAX_NAME_LENGTH = 15;

    private final Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread pingThread;
    private volatile boolean running = true;
    private long lastPongTime;
    private static final long PING_INTERVAL = 2000; // 2 seconds
    private static final long TIMEOUT = 5000; // 5 seconds
    private int udpPort = -1; // Port the client is listening on for UDP updates

    protected static final List<ClientHandler> clientList = new CopyOnWriteArrayList<>();

    protected static final List<Lobby> lobbyList = new CopyOnWriteArrayList<>();
    protected Lobby currentLobby;
    private static int nextLobbyCode = 1000;
    protected boolean isReady = false;
    private boolean clientReady = false;

    private static DatagramSocket serverUpdateSocket;
    private static DatagramSocket udpListeningSocket;
    private static volatile boolean udpListenerRunning = false;
    private static Thread udpListenerThread;

    protected String nickname;
    protected Player player;

    protected boolean isSpectator = false;

    static {
        try {
            serverUpdateSocket = new DatagramSocket();
        } catch (SocketException e) {
            logger.error("Could not create server UDP sending socket", e);
            serverUpdateSocket = null;
        }
    }

    /**
     * Starts the static UDP listener thread if it's not already running. Should be called once
     * during server initialization.
     */
    public static synchronized void startUdpListener() {
        if (udpListenerRunning || udpListenerThread != null) {
            logger.warn("UDP Listener is already running or was not properly stopped.");
            return;
        }

        try {
            udpListeningSocket = new DatagramSocket(SERVER_UDP_LISTENING_PORT);
            udpListenerRunning = true;
            udpListenerThread = new Thread(ClientHandler::runUdpListenerLoop);
            udpListenerThread.setName("Server-UDP-Listener");
            udpListenerThread.setDaemon(true);
            udpListenerThread.start();

        } catch (SocketException e) {
            logger.error("Could not start UDP Listener on port {}", SERVER_UDP_LISTENING_PORT, e);
            udpListenerRunning = false;
            udpListeningSocket = null;
            udpListenerThread = null;
        }
    }

    /**
     * Stops the static UDP listener thread. Should be called once during server shutdown.
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
     * Static method to run a UDP listener loop for position updates. This is run in its own
     * thread.
     */
    private static void runUdpListenerLoop() {
        if (serverUpdateSocket == null) {
            logger.error("Cannot run UDP listener - socket is null");
            return;
        }

        if (udpListeningSocket == null) {
            logger.error("udpListeningSocket is null!");
            return;
        }

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
                String message = new String(packet.getData(), 0, packet.getLength());

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
     * Handles a position update from a client. Format: position:playerName:lobbyCode:x:y
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

            if (sender.currentLobby == null) {
                logger.warn("Player {} not in a lobby", senderName);
                return;
            }

            String currentLobbyCode = String.valueOf(sender.currentLobby.getCode());
            if (!currentLobbyCode.equals(lobbyCode)) {
                logger.warn("Lobby code mismatch for {}", senderName);
                return;
            }

            if (sender.currentLobby.getGameState() == null) {
                logger.warn("game state is null for player {}", senderName);
                return;
            }

            sender.clientReady = true;

            // check correct spawn/teleport location
            if (!sender.getPlayer().getPositionWasSet()) {
                if (x == sender.getPlayer().getX() && y == sender.getPlayer().getY()) {
                    sender.getPlayer().setPositionWasSet(true);
                } else {
                    x = (int) sender.getPlayer().getX();
                    y = (int) sender.getPlayer().getY();
                    sender.currentLobby.broadcastUpdateToLobby(
                        "player_position:" + senderName + ":" + x + ":" + y, null);
                }
            }

            // if there is a collision, we return the current coordinates
            if (sender.currentLobby.getMap() != null &&
                checkCollision(x, y, sender.getPlayer().getWidth(), sender.getPlayer().getHeight(),
                    sender.currentLobby.getMap(), sender.getPlayer()
                        .getRole() == Role.GOAT)) {
                x = (int) sender.getPlayer().getX();
                y = (int) sender.getPlayer().getY();
                logger.info("collision prevented");
                sender.currentLobby.broadcastUpdateToLobby(
                    "player_position:" + senderName + ":" + x + ":" + y, null);
            } else {
                sender.getPlayer().setX(x);
                sender.getPlayer().setY(y);
            }

            if (sender.getPlayer().getRole() == Role.GOAT &&
                !sender.currentLobby.getGameState().gameOver && sender.currentLobby.getGameState()
                .isDoorOpen() &&
                (sender.getPlayer().getX() < 0 || sender.getPlayer().getX() > 1500)) {
                logger.info("{} escaped, ending game", sender.getNickname());
                sender.endGame(false);
            }

            String broadcastMessage = "player_position:" + senderName + ":" + x + ":" + y;
            sender.currentLobby.broadcastUpdateToLobby(broadcastMessage, sender);

        } catch (NumberFormatException e) {
            logger.error("Invalid coordinates", e);
        } catch (Exception e) {
            logger.error("Error processing update: ", e);
        }
    }

    /**
     * checks for a collision with a wall
     */
    private static boolean checkCollision(int x, int y, double playerWidth, double playerHeight,
        Map map, boolean ignoreWindows) {
        for (Wall wall : map.getCollisionWalls()) {
            if (igoat.client.Player.collidesWithWall(x, y, playerWidth, playerHeight, wall)) {
                return true;
            }
        }
        if (!ignoreWindows) {
            for (Wall wall : map.getWindowCollisions()) {
                if (igoat.client.Player.collidesWithWall(x, y, playerWidth, playerHeight, wall)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Finds the ClientHandler associated with the given IP and nickname and updates its UDP port.
     *
     * @param clientIp            The IP address of the client.
     * @param nickname            The nickname reported by the client.
     * @param clientListeningPort The actual UDP port the client is listening on (from the
     *                            message).
     */
    private static void registerClientUdpPort(InetAddress clientIp, String nickname,
        int clientListeningPort) {
        boolean found = false;
        for (ClientHandler handler : clientList) {
            if (handler.nickname.equals(nickname)) {

                handler.udpPort = clientListeningPort;
                found = true;

                if (serverUpdateSocket != null) {
                    try {
                        String ackMsg = "udp_ack:";
                        byte[] ackBuf = ackMsg.getBytes();
                        DatagramPacket ackPacket = new DatagramPacket(ackBuf, ackBuf.length,
                            clientIp, clientListeningPort);
                        serverUpdateSocket.send(ackPacket);
                    } catch (IOException e) {
                        logger.error("Failed to send UDP ACK to {}", nickname, e);
                    }
                }
                break;
            }
        }
        if (!found) {
            logger.error("UDP registration failed for {} (Handler not found or IP/name mismatch)",
                nickname);
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

            // start ping thread
            pingThread = new Thread(this::runPingPong);
            pingThread.start();

            String message;
            while (running && (message = in.readLine()) != null) {
                if (message.equals("pong")) {
                    handlePong();
                    continue;
                } else if (message.equals("exit")) {
                    running = false;
                    break;
                }

                if (message.startsWith("spectate:")) {
                    handleSpectate(message.split(":"));
                    continue;
                }
                if (message.startsWith("leaveSpectate:")) {
                    handleLeaveSpectate(message.split(":"));
                    continue;
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
                disconnect();
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
    void handleCommand(String message) {
        try {
            int colonIndex = message.indexOf(':');
            if (colonIndex == -1) {
                sendError(lang.get("server.formatError"));
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
                        if (!handleCheatCode(params)) {
                            currentLobby.broadcastChatToLobby(nickname + ":" + params);
                        }
                    } else {
                        sendError(lang.get("server.noLobby"));
                    }
                    break;
                case "getplayers":
                    handleGetPlayers();
                    break;
                case "getlobbyplayers":
                    handleGetLobbyPlayers();
                    break;
                case "ready":
                    handleReady(true);
                    break;
                case "unready":
                    handleReady(false);
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
                case "station":
                    handleStation(params.trim());
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
                        sendError(lang.get("server.whisperError"));
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
                case "getresults":
                    handleGetResults();
                    break;
                case "gethighscores":
                    handleGetHighscores();
                    break;
                case "spectate":
                    handleSpectate(params.split(":"));
                    break;
                case "leavespectate":
                    handleLeaveSpectate(params.split(":"));
                    break;
                default:
                    sendError("Unknown command: " + command);
            }
        } catch (Exception e) {
            sendError(lang.get("server.commError") + ": " + e.getMessage());
            logger.error("Error when processing command {} : {}", message, e);
        }
    }

    /**
     * Performs checks for whether the player has the correct role and if they're in range and then
     * revives a caught iGOAT
     */
    private void handleStation(String params) {
        // role check
        if (player.getRole() != Role.GOAT) {
            return;
        }

        // cooldown check
        if (currentLobby.getStationCooldown().check()) {
            return;
        }

        double x = player.getX();
        double y = player.getY();

        // igoat station location
        double tx;
        double ty;

        if (sqrt(pow(50 - x, 2) + pow(120 - y, 2)) < 45.0) {
            tx = 50;
            ty = 120;
        } else if (sqrt(pow(640 - x, 2) + pow(1450 - y, 2)) < 45.0) {
            tx = 640;
            ty = 1450;
        } else {
            return;
        }

        int stationId = Integer.parseInt(params);

        for (ClientHandler client : currentLobby.getMembers()) {
            if (client.getPlayer().getRole() == Role.IGOAT && client.getPlayer().isCaught()
                && currentLobby.getGameState().activateStation(stationId)) {
                client.getPlayer().revive();
                client.getPlayer().teleport(tx + 20, ty + 40);
                currentLobby.broadcastToLobby("revive:" + client.getNickname());
                currentLobby.broadcastToLobby("activateStation:" + stationId);
                currentLobby.getStationCooldown().start();
                return;
            }
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
    void handleConnect(String[] params) {
        if (params.length < 1) {
            sendError(lang.get("server.noName"));
            return;
        }

        // sanitize string
        String requestedNickname = params[0].replaceAll("[\\s=:,]", "");

        if (requestedNickname.isEmpty()) {
            sendError(lang.get("server.noName"));
            return;
        }

        // cut off if too long
        requestedNickname =
            requestedNickname.length() > MAX_NAME_LENGTH ? requestedNickname.substring(0,
                MAX_NAME_LENGTH) : requestedNickname;

        this.nickname = generateUniqueNickname(requestedNickname);

        if (!requestedNickname.equals(this.nickname)) {
            sendMessage(
                "chat:" + lang.get("server.nicknameChosen") + this.nickname);
        }

        sendMessage("confirm:" + this.nickname);
        broadcast("chat:" + String.format(lang.get("server.connected"), this.nickname));
        broadcastGlobalPlayerList();

        // check if player was in a game
        for (Lobby lobby : lobbyList) {
            for (Player player : lobby.getPlayerList()) {
                if (nickname.equals(player.getNickname())) {
                    if (lobby.getState() != LobbyState.IN_GAME) {
                        return;
                    }
                    joinLobby(lobby);
                    reconnect();
                    return;
                }
            }
        }
    }

    /**
     * Connects the user to an existing game.
     */
    private void reconnect() {
        logger.info("Active game detected - reconnecting");
        player = currentLobby.getPlayer(nickname);

        if (player == null) {
            logger.error("Could not reconnect");
            leaveCurrentLobby();
            return;
        }
        player.setPositionWasSet(false);
        sendMessage("game_started:");

        logger.info("waiting for client...");
        while (!clientReady) {
            try {
                Thread.sleep(100);

            } catch (InterruptedException e) {
                logger.error("Error", e);
            }
        }

        // send gamestate event log
        int counter = 0;
        for (String event : currentLobby.getGameState().getEventLog()) {
            sendMessage(event);
            counter++;
        }
        // send player status
        for (Player player : currentLobby.getPlayerList()) {
            if (player.isCaught()) {
                sendMessage("catch:" + player.getNickname());
                counter++;
            }
        }
        // send station status
        boolean[] stations = currentLobby.getGameState().getStations();
        for (int i = 0; i < stations.length; i++) {
            if (stations[i]) {
                sendMessage("activateStation:" + i);
                counter++;
            }
        }

        logger.info("sent event log ({} items)", counter);
    }

    /**
     * Processes a chat message.
     *
     * @param params Array of parameters, where params[0] is the message
     */
    private void handleChat(String[] params) {
        if (isSpectator) {
            sendError(lang.get("server.noSpectator"));
            return;
        }
        if (params.length < 1 || params[0].isEmpty()) {
            sendError(lang.get("server.noMSG"));
            return;
        }
        if (handleCheatCode(params[0])) {
            return;
        }

        broadcast("chat:" + this.nickname + ":" + params[0]);
    }

    private boolean handleCheatCode(String message) {
        if (player == null || player.getRole() == null) {
            return false;
        }

        String input = message.toUpperCase().trim();
        Role role = player.getRole();
        boolean isGoat = (role == Role.GOAT || role == Role.IGOAT);
        boolean isGuard = (role == Role.GUARD);

        if (input.equals("FBI OPEN UP") && isGoat) {
            if (currentLobby != null && currentLobby.isCheatLocked()) {
                sendMessage("chat:Cheats are disabled. Doors have already been opened!");
                return true;
            }

            if (Math.random() < 0.25) {
                openDoorsCheat();
            } else {
                randomCatchCheat();
            }
            return true;
        }

        if (input.equals("ARE U SURE") && isGuard) {
            if (currentLobby != null && currentLobby.isCheatLocked()) {
                sendMessage("chat:Cheats are disabled. Doors have already been opened!");
                return true;
            }

            if (Math.random() < 0.75) {
                randomCatchCheat();
            } else {
                openDoorsCheat();
            }
            return true;
        }

        return false;
    }

    private void openDoorsCheat() {
        if (currentLobby != null && currentLobby.getGameState() != null) {
            currentLobby.getGameState().openDoors();
            currentLobby.broadcastToLobby("door");
            currentLobby.getMap().openDoors();
            broadcast("chat:" + lang.get("server.doorsCheat"));
            broadcast("chat:CHEAT ACTIVATED -> Doors opened!");

            currentLobby.lockCheats();
            currentLobby.broadcastChatToLobby("CHEATS LOCKED: No further cheat attempts allowed.");
        }
    }

    private void randomCatchCheat() {
        if (currentLobby != null && currentLobby.getGameState() != null) {
            List<ClientHandler> candidates = currentLobby.getMembers().stream()
                .filter(c -> {
                    Role r = c.getPlayer().getRole();
                    return (r == Role.GOAT || r == Role.IGOAT) && !c.getPlayer().isCaught();
                })
                .toList();

            if (!candidates.isEmpty()) {
                ClientHandler target = candidates.get((int) (Math.random() * candidates.size()));
                Role role = target.getPlayer().getRole();

                if (role == Role.GOAT) {
                    target.getPlayer().getSpawnProtection().update();
                    if (target.getPlayer().getSpawnProtection().getTime() < 5000) {
                        sendMessage("chat:" + lang.get("server.spawnProt"));
                        return;
                    }
                    target.getPlayer().teleport(1080, 800);
                }

                target.getPlayer().catchPlayer();
                currentLobby.broadcastToLobby("catch:" + target.getNickname());

                broadcast("chat:" + String.format(lang.get("server.doorsCheatFail"),
                    target.getNickname()));

                if (currentLobby.getGameState().isGuardWin()
                    && !currentLobby.getGameState().gameOver) {
                    endGame(true);
                }
            }
        }
    }

    /**
     * Processes a username change request. Format: username:newname
     *
     * @param params Array of parameters, where params[0] is the new username
     */
    private void handleUsername(String[] params) {
        if (params.length < 1) {
            sendError("no username provided");
            return;
        }

        String oldNickname = this.nickname;
        // sanitize string
        String requestedNickname = params[0].replaceAll("[\\s=:,]", "");
        if (requestedNickname.isEmpty()) {
            sendError("invalid username");
            return;
        }

        requestedNickname =
            requestedNickname.length() > MAX_NAME_LENGTH ? requestedNickname.substring(0,
                MAX_NAME_LENGTH) : requestedNickname;

        String newNickname = generateUniqueNickname(requestedNickname);

        if (!requestedNickname.equals(newNickname)) {
            sendMessage(
                "chat:" + lang.get("server.nicknameChosen")
                    + newNickname);
        }

        this.nickname = newNickname;
        sendMessage("confirm:" + this.nickname);
        broadcast(
            "chat:" + String.format(lang.get("server.nameChange"), oldNickname, newNickname));
        logger.info("User nickname changed");
        broadcastGlobalPlayerList();
        if (currentLobby != null) {
            broadcastLobbyPlayerList();
        }
    }

    /**
     * Processes a lobby join request. Format: lobby:code
     *
     * @param params Array of parameters, where params[0] is the lobby code
     */
    private void handleLobby(String[] params) {
        if (params.length < 1) {
            sendError(lang.get("server.codeError"));
            sendMessage("lobby:0");
            return;
        }

        int code;
        try {
            code = Integer.parseInt(params[0]);
        } catch (NumberFormatException e) {
            sendError(lang.get("server.codeError"));
            sendMessage("lobby:0");
            return;
        }

        if (currentLobby != null && code != 0) {
            sendError(lang.get("server.inLobbyError"));
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
            sendError(lang.get("server.codeError"));
            sendMessage("lobby:0");
            return;
        }

        if (lobbyToJoin.getState() == LobbyState.IN_GAME
            || lobbyToJoin.getState() == LobbyState.FINISHED) {
            sendError(lang.get("server.inProgressError"));
            return;
        }

        if (lobbyToJoin.isFull()) {
            sendError(String.format(lang.get("server.fullLobby"), code));
            sendMessage("lobby:0");
            return;
        }

        joinLobby(lobbyToJoin);
        for (ClientHandler member : lobbyToJoin.getMembers()) {
            String readyStatusMsg = "ready_status:" + member.getNickname() + "," + member.isReady();
            sendMessage(readyStatusMsg);
        }
    }

    private void handleNewLobby() {
        if (currentLobby != null) {
            sendError(lang.get("server.inLobbyError"));
            return;
        }

        int code = nextLobbyCode++;
        Lobby newLobby = new Lobby(code);
        lobbyList.add(newLobby);
        newLobby.broadcastChatToLobby(
            String.format(lang.get("server.createdLobby"), nickname, code));
        joinLobby(newLobby);
    }

    private void joinLobby(Lobby lobby) {
        leaveCurrentLobby();

        currentLobby = lobby;
        currentLobby.addMember(this);

        sendMessage("lobby:" + lobby.getCode());
        currentLobby.broadcastChatToLobby(
            String.format(lang.get("server.joinedLobby"), nickname, lobby.getCode()));

        broadcastGetLobbiesToAll();
        broadcastLobbyPlayerList();
    }

    private void leaveCurrentLobby() {
        if (currentLobby != null) {
            currentLobby.removeMember(this);
            currentLobby.broadcastChatToLobby(
                String.format(lang.get("server.leftLobby"), nickname));

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

        sendMessage("getlobbies:" + sb);
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

        String update = "getlobbyplayers:" + sb;
        for (ClientHandler member : currentLobby.getMembers()) {
            member.sendMessage(update);
        }
        for (ClientHandler spectator : currentLobby.getSpectators()) {
            spectator.sendMessage(update);
        }
    }

    private void handleReady(boolean status) {
        if (isSpectator) {
            sendError(lang.get("server.spectatorReady"));
            return;
        }
        if (currentLobby == null) {
            sendError(lang.get("server.noLobby"));
            setReady(false);
            return;
        }
        this.setReady(status);
        if (currentLobby.getState() == LobbyState.FINISHED) {
            currentLobby.resetState();
            broadcastGetLobbiesToAll();
        }
        String statusMessage = "ready_status:" + this.nickname + "," + this.isReady;
        currentLobby.broadcastToAll(statusMessage);
        if (isReady) {
            appendToLobbyChat(String.format(lang.get("server.ready"), nickname), false);
        } else {
            appendToLobbyChat(String.format(lang.get("server.unready"), nickname), false);
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

    private void handleRoleConfirmation(String roleString) {
        try {
            Role parsedRole = Role.valueOf(roleString);
            this.getPlayer().setRole(parsedRole);
            Lobby.roleMap.put(nickname, parsedRole);
            logger.info("role confirmed");
            appendToLobbyChat(
                String.format(lang.get("server.roleAssigned"), nickname, player.getRole()), false);
        } catch (NumberFormatException e) {
            logger.error("Invalid Role {}", roleString, e);
            sendError("Invalid role");
            handleGetRoles();
        }
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
     * Disconnects the client. Sends appropriate broadcast messages and closes resources.
     */
    private void disconnect() {
        try {
            leaveCurrentLobby();

            if (!running) {
                broadcast("chat:" + String.format(lang.get("server.disconnected"), nickname));
                logger.info("Client {} disconnected", nickname);
            } else {
                broadcast("chat:" + String.format(lang.get("server.disconnected"), nickname));
                logger.info("Client {} was disconnected", nickname);
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
    void sendError(String errorMessage) {
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

            serverUpdateSocket.send(packet);
        } catch (IOException e) {
            logger.error("Send failed", e);
        }
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    /**
     * Handles the start game command from a client. Only the lobby creator can start the game.
     */
    private void handleStartGame() {
        if (currentLobby == null) {
            sendError(lang.get("server.noName"));
            return;
        }

        boolean allReadyCheck = true;
        if (currentLobby.getMembers().isEmpty() /*|| currentLobby.getMembers().size() < 4*/) {
            logger.warn("Lobby not full, can't start game in {}", currentLobby.getCode());
            return;
        } else {
            for (ClientHandler member : currentLobby.getMembers()) {
                if (!member.isReady()) {
                    allReadyCheck = false;
                    break;
                }
            }
        }

        if (currentLobby.getState() == LobbyState.IN_GAME) {
            sendError(lang.get("server.inProgressError"));
            return;
        }

        if (!allReadyCheck) {
            sendError(lang.get("server.notAllReady"));
            logger.info("Start game requested for lobby {} but not all players are ready.",
                currentLobby.getCode());
            return;
        }

        if (currentLobby.getMembers().isEmpty()) {
            sendError("empty lobby");
        }

        for (ClientHandler member : currentLobby.getMembers()) {
            member.setReady(false);
        }

        logger.info("Starting game in lobby {} (Initiated by creator: {})", currentLobby.getCode(),
            nickname);

        currentLobby.startGame();
        player.getSpawnProtection().reset();
        broadcastGetLobbiesToAll();

        String gameStartedMessage = "game_started:";
        currentLobby.broadcastToAll(gameStartedMessage);
        currentLobby.setRoles();
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

    private void handleGetResults() {
        try {
            Path logPath = Paths.get("finished_games.log");
            if (!Files.exists(logPath)) {
                sendMessage("results:No past results available.");
                return;
            }

            List<String> lines = Files.readAllLines(logPath);
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                sb.append(line).append("\n");
            }
            sendMessage("results:" + sb);
        } catch (IOException e) {
            logger.error("Failed to read finished games", e);
            sendMessage("results:Error reading past results.");
        }
    }

    /**
     * Handles the gethighscores command. Retrieves and sends the highscores to the client.
     */
    private void handleGetHighscores() {
        try {
            HighscoreManager.initialize();

            String highscores = HighscoreManager.getHighscores();

            String formattedHighscores = highscores.replace("\n", "<br>");

            sendMessage("highscores:" + formattedHighscores);
            logger.info("Sent highscores to client");
        } catch (Exception e) {
            logger.error("Failed to retrieve highscores", e);
            sendMessage("highscores:Error retrieving highscores.");
        }
    }

    /**
     * ends the game and broadcasts the result
     *
     * @param result true if the guard won, false otherwise
     */
    void endGame(boolean result) {
        if (!currentLobby.getGameState().gameOver) {
            isReady = false;

            currentLobby.getGameState().gameOver = true;
            currentLobby.endGame();
            broadcastGetLobbiesToAll();
            currentLobby.broadcastToAll("gameover:" + result);
            long gameTime = currentLobby.getTimer().getTime();
            logger.info("Game time: {} ms", gameTime);
            logger.info("Game finished in {}", currentLobby.getTimer().toString());

            try {
                HighscoreManager.initialize();

                logger.info("Saving highscore with game time: {} ms", gameTime);

                if (result) {
                    for (ClientHandler m : currentLobby.getMembers()) {
                        if (m.getPlayer().getRole() == Role.GUARD) {
                            logger.info("Adding guard highscore for player: {}", m.nickname);
                            HighscoreManager.addGuardHighscore(m.nickname, gameTime);
                            break;
                        }
                    }
                } else {
                    StringBuilder goatNames = new StringBuilder();
                    for (ClientHandler m : currentLobby.getMembers()) {
                        if (m.getPlayer().getRole() == Role.GOAT
                            || m.getPlayer().getRole() == Role.IGOAT) {
                            if (!goatNames.isEmpty()) {
                                goatNames.append(", ");
                            }
                            goatNames.append(m.nickname);
                        }
                    }
                    logger.info("Adding goat highscore for players: {}", goatNames);
                    HighscoreManager.addGoatHighscore(goatNames.toString(), gameTime);
                }
            } catch (Exception e) {
                logger.error("Failed to save highscore", e);
            }

            try {
                Path logPath = Paths.get("finished_games.log");
                DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
                String timestamp = LocalDateTime.now().format(fmt);
                StringBuilder sb = new StringBuilder();
                sb.append("{\"timestamp\":\"").append(timestamp).append("\",");
                sb.append("\"lobby\":").append(currentLobby.getCode()).append(',');
                sb.append("\"result\":").append(result).append(',');
                sb.append("\"players\":[");
                for (ClientHandler m : currentLobby.getMembers()) {
                    boolean win = (result && m.getPlayer().getRole() == Role.GUARD) || (!result
                        && m.getPlayer().getRole() != Role.GUARD);
                    sb.append("{\"name\":\"").append(m.nickname).append("\",")
                        .append("\"role\":\"").append(m.getPlayer().getRole()).append("\",")
                        .append("\"outcome\":\"").append(win ? "Won" : "Lost").append("\"},");
                }
                if (sb.charAt(sb.length() - 1) == ',') {
                    sb.setLength(sb.length() - 1);
                }
                sb.append("]}\n");
                Files.write(logPath, sb.toString().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                logger.error("Failed to write finished game", e);
            }
        }
    }

    /**
     * Handles terminal activation request from client.
     *
     * @param params terminal id as string
     */
    private void handleTerminalActivation(String params) {
        if (currentLobby == null) {
            sendError(lang.get("server.noLobby"));
            return;
        }
        try {
            int terminalId = Integer.parseInt(params);
            boolean activated = currentLobby.getGameState().activateTerminal(terminalId);

            if (currentLobby.getGameState().isDoorOpen()) {
                currentLobby.broadcastChatToLobby(lang.get("game.openDoors"));
                currentLobby.broadcastToAll("door");
                currentLobby.getMap().openDoors();
                logger.info("Exits have been opened!");
            }

            if (activated) {
                currentLobby.broadcastToAll("terminal:" + terminalId);
                for (ClientHandler player : currentLobby.getMembers()) {
                    if (player.getPlayer().getRole() == Role.GOAT && player.getPlayer()
                        .isCaught()) {
                        player.getPlayer().revive();
                        player.getPlayer().teleport(920, 230);
                        currentLobby.broadcastToAll("revive:" + player.getNickname());
                        player.getPlayer().getSpawnProtection().reset();
                    }
                }
            } else {
                sendMessage("terminal:-1");
            }
        } catch (NumberFormatException e) {
            sendError(lang.get("game.termError") + ": " + params);
        }
    }

    /**
     * Processes a whisper message to a specific client. Format: whisper:recipient,message
     */
    private void handleWhisper(String[] params) {
        if (isSpectator) {
            sendError("Spectators cannot chat");
            return;
        }
        if (params.length < 2) {
            sendError("Empty whisper message");
            return;
        }
        String recipient = params[0];
        String msg = params[1];
        for (ClientHandler client : clientList) {
            if (client.nickname.equals(recipient)) {
                client.sendMessage("chat:[" + nickname + " whispered] " + msg);
                return;
            }
        }
        sendError("User " + recipient + " not found");
    }

    /**
     * Sends a message to all connected clients.
     */
    private void broadcast(String message) {
        for (ClientHandler client : clientList) {
            client.sendMessage(message);
        }
    }

    /**
     * Broadcasts the list of lobbies to all clients.
     */
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
        String msg = "getlobbies:" + sb;
        for (ClientHandler client : clientList) {
            client.sendMessage(msg);
        }
    }

    /**
     * Broadcasts the global player list to all clients.
     */
    private static void broadcastGlobalPlayerList() {
        StringBuilder sb = new StringBuilder();
        for (ClientHandler client : clientList) {
            sb.append(client.getNickname()).append(",");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        String update = "getplayers:" + sb;
        for (ClientHandler client : clientList) {
            client.sendMessage(update);
        }
    }

    /**
     * Broadcasts the list of players in the current lobby to its members.
     */
    private void broadcastLobbyPlayerList() {
        if (currentLobby == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (ClientHandler member : currentLobby.getMembers()) {
            sb.append(member.getNickname()).append(",");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        String update = "getlobbyplayers:" + sb;
        for (ClientHandler member : currentLobby.getMembers()) {
            member.sendMessage(update);
        }
    }

    private void handleCatch(String targetName) {
        ClientHandler target = findPlayer(targetName);
        if (target == null) {
            sendError(lang.get("server.invalidTarget"));
            return;
        }
        if (player.getRole() != Role.GUARD) {
            sendError(lang.get("server.wrongRole"));
            return;
        }
        if (!isInRange(this, target)) {
            sendError(lang.get("server.noRange"));
            return;
        }
        if (player.isCaught()) {
            sendError(lang.get("server.invalidTarget"));
            return;
        }

        if (target.getPlayer().getRole() == Role.GOAT) {
            target.getPlayer().getSpawnProtection().update();
            if (target.getPlayer().getSpawnProtection().getTime() < 5000) {
                return;
            }

            target.getPlayer().teleport(1080, 800);
        }

        target.getPlayer().catchPlayer();
        currentLobby.broadcastToAll("catch:" + targetName);

        if (currentLobby.getGameState().isGuardWin() && !currentLobby.getGameState().gameOver) {
            logger.info("guard won, ending game");
            endGame(true);
        }
    }

    private void handleRevive(String targetName) {
        ClientHandler target = findPlayer(targetName);
        if (target == null) {
            sendError(lang.get("server.invalidTarget"));
            return;
        }
        if (player.getRole() != Role.GOAT) {
            sendError(lang.get("server.wrongRole"));
            return;
        }
        if (player.isCaught()) {
            return;
        }
        if (target.getPlayer().getRole() != Role.IGOAT || !target.getPlayer().isCaught()) {
            sendError(lang.get("server.invalidTarget"));
            return;
        }
        if (!isInRange(this, target)) {
            sendError(lang.get("server.noRange"));
            return;
        }
        target.getPlayer().revive();
        currentLobby.broadcastToAll("revive:" + targetName);
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean ready) {
        this.isReady = ready;
    }

    public String getNickname() {
        return nickname;
    }

    private void handleGetRoles() {
        if (currentLobby == null) {
            sendError(lang.get("server.noLobby"));
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (ClientHandler member : currentLobby.getMembers()) {
            Role r = Lobby.roleMap.get(member.getNickname());
            if (r != null) {
                sb.append(member.getNickname()).append("=").append(r).append(",");
            }
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        sendMessage("roles:" + sb);
    }

    private void handleSpectate(String[] params) {
        if (params.length < 2) {
            sendError(lang.get("server.codeError"));
            sendMessage("lobby:0");
            return;
        }
        int code;
        try {
            code = Integer.parseInt(params[1]);
        } catch (NumberFormatException e) {
            sendError(lang.get("server.codeError"));
            sendMessage("lobby:0");
            return;
        }
        Lobby lobbyToSpectate = null;
        for (Lobby lobby : lobbyList) {
            if (lobby.getCode() == code) {
                lobbyToSpectate = lobby;
                break;
            }
        }
        if (lobbyToSpectate == null) {
            sendError(lang.get("server.codeError"));
            sendMessage("lobby:0");
            return;
        }
        this.isSpectator = true;
        this.currentLobby = lobbyToSpectate;
        lobbyToSpectate.addSpectator(this);
        if (lobbyToSpectate.getGameState() != null) {
            for (String event : lobbyToSpectate.getGameState().getEventLog()) {
                sendMessage(event);
            }
            for (Player player : lobbyToSpectate.getPlayerList()) {
                if (player.isCaught()) {
                    sendMessage("catch:" + player.getNickname());
                }
            }
            boolean[] stations = lobbyToSpectate.getGameState().getStations();
            for (int i = 0; i < stations.length; i++) {
                if (stations[i]) {
                    sendMessage("activateStation:" + i);
                }
            }
        }
    }

    private void handleLeaveSpectate(String[] params) {
        if (!isSpectator || currentLobby == null) {
            sendError("Not spectating a lobby");
            return;
        }
        if (params.length < 2) {
            sendError(lang.get("server.codeError"));
            return;
        }
        int code;
        try {
            code = Integer.parseInt(params[1]);
        } catch (NumberFormatException e) {
            sendError(lang.get("server.codeError"));
            return;
        }
        if (currentLobby.getCode() != code) {
            sendError(lang.get("server.codeError"));
            return;
        }
        currentLobby.removeSpectator(this);
        this.currentLobby = null;
        this.isSpectator = false;
        sendMessage("lobby:0");
    }
}