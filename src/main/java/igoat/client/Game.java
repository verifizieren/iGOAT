package igoat.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;




public class Game extends Application {

    private static final double PLAYER_WIDTH = 32;
    private static final double PLAYER_HEIGHT = 32;
    private static final double MOVEMENT_SPEED = 300;
    private static final double CAMERA_ZOOM = 1.0;
    
    private Pane gamePane;
    private Set<KeyCode> activeKeys;
    private long lastUpdate;
    private long lastPositionUpdate = 0;
    private static final long POSITION_UPDATE_INTERVAL = 16; // Send updates every 16ms (approximately 60fps)
    
    private Player player;
    private igoat.client.Map gameMap;
    private Camera activeCamera;
    private ServerHandler serverHandler;
    private String playerName;
    private String lobbyCode;
    
    private boolean gameStarted = false;
    
    private Map<String, Player> otherPlayers = new HashMap<>();
    
    /**
     * Initializes the game with necessary data from the lobby.
     *
     * @param handler The active ServerHandler.
     * @param name The player's confirmed name.
     * @param code The lobby code.
     * @param initialPlayerNames List of players already in the lobby.
     */
    public void initialize(ServerHandler handler, String name, String code, List<String> initialPlayerNames) {
        this.serverHandler = handler;
        this.playerName = name;
        this.lobbyCode = code;
        this.gameStarted = true;

        startMessageProcessor();
        startUdpUpdateProcessor();

        Platform.runLater(() -> {
            String confirmedNickname = serverHandler.getConfirmedNickname();
            if (confirmedNickname == null) {
                System.err.println("[Game_INIT] Cannot initialize - confirmed nickname is null");
                return;
            }
            
            for (String pName : initialPlayerNames) {
                if (!pName.equals(confirmedNickname)) {
                    createVisualForRemotePlayer(pName, 100, 100);
                }
            }
        });

        if (this.serverHandler != null) {
            // muss vlt angepasst werden
            // serverHandler.sendUdpRegistrationPacketIfNeeded(this.playerName);
        }
    }

    /**
     * Initializes and starts the game VIEW.
     * Assumes initialize() has been called previously.
     * Sets up the game window, player visuals, map, and input handling.
     * Also initializes the game loop using AnimationTimer.
     *
     * @param primaryStage The primary stage of the JavaFX application
     */
    @Override
    public void start(Stage primaryStage) {
        if (serverHandler == null || playerName == null || lobbyCode == null) {
            showError("Initialization Error", "Game cannot start without server connection details.");
            Platform.exit();
            return;
        }

        String confirmedNickname = serverHandler.getConfirmedNickname();
        if (confirmedNickname == null) {
            showError("Initialization Error", "Cannot start game without confirmed nickname from server.");
            Platform.exit();
            return;
        }

        gameMap = new igoat.client.Map();
        primaryStage.setWidth(gameMap.getWidth());
        primaryStage.setHeight(gameMap.getHeight());
        primaryStage.setFullScreenExitHint("");

        gamePane = new Pane();
        gamePane.setPrefSize(gameMap.getWidth(), gameMap.getHeight());
        gamePane.setMinSize(gameMap.getWidth(), gameMap.getHeight());
        gamePane.setMaxSize(gameMap.getWidth(), gameMap.getHeight());
        gamePane.setStyle("-fx-background-color: #f0f0f0;");

        Scene scene = new Scene(gamePane);
        
        String windowTitle = "iGoat Game - Lobby " + lobbyCode + " - Player: " + confirmedNickname;
        primaryStage.setTitle(windowTitle);
        
        primaryStage.setScene(scene);

        primaryStage.show();

        for (javafx.scene.Node wall : gameMap.getVisualWalls()) {
            gamePane.getChildren().add(wall);
        }
        
        double startX = 100;
        double startY = 100;
        
        player = new Player(gamePane, scene.getWidth(), scene.getHeight(), CAMERA_ZOOM,
                          (int)startX, (int)startY, (int)PLAYER_WIDTH, (int)PLAYER_HEIGHT, Color.RED, confirmedNickname);
        
        player.setSpectated(false);
        activeCamera = player.getCamera();
        
        for (Player other : otherPlayers.values()) {
            if (!gamePane.getChildren().contains(other.getVisualRepresentation())) {
                gamePane.getChildren().add(other.getVisualRepresentation());
            }
        }
        
        scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            activeCamera.updateViewport(newVal.doubleValue(), scene.getHeight());
        });
        
        scene.heightProperty().addListener((obs, oldVal, newVal) -> {
            activeCamera.updateViewport(scene.getWidth(), newVal.doubleValue());
        });
        
        primaryStage.fullScreenProperty().addListener((obs, oldVal, newVal) -> {
            javafx.application.Platform.runLater(() -> {
                activeCamera.updateViewport(scene.getWidth(), scene.getHeight());
            });
        });
        
        activeKeys = new HashSet<>();
        scene.setOnKeyPressed(event -> {
            activeKeys.add(event.getCode());
            if (event.getCode() == KeyCode.ESCAPE) {
                primaryStage.setFullScreen(false);
            }
        });
        
        scene.setOnKeyReleased(event -> {
            activeKeys.remove(event.getCode());
        });
        
        gamePane.setFocusTraversable(true);
        gamePane.requestFocus();
        
        lastUpdate = System.nanoTime();
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                double deltaTime = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;
                update(deltaTime);
            }
        }.start();
    }
    
    /**
     * Shows an error dialog.
     */
    private void showError(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
    
    /**
     * Shows an info dialog.
     */
    private void showInfo(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
    
    /**
     * Starts a thread to process incoming messages from the server.
     * Uses the ServerHandler passed during initialization.
     */
    private void startMessageProcessor() {
        if (serverHandler == null) return;

        Thread messageProcessor = new Thread(() -> {
            while (gameStarted && serverHandler.isConnected()) {
                try {
                    String message = serverHandler.getMessage();
                    if (message != null) {
                        processServerMessage(message);
                    }
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Message processor interrupted.");
                    break;
                } catch (Exception e) {
                     System.err.println("Error in message processor: " + e.getMessage());
                     break;
                 }
            }
             System.out.println("Message processor stopped.");
        });
        messageProcessor.setDaemon(true);
        messageProcessor.start();
    }
    
    /**
     * Processes messages received from the server.
     *
     * @param message The server message
     */
    private void processServerMessage(String message) {
        if (message.startsWith("getlobbyplayers:")) {
            String playersData = message.substring("getlobbyplayers:".length());
            if (!playersData.isEmpty()) {
                String[] playerNames = playersData.split(",");
                Set<String> currentPlayers = new HashSet<>(List.of(playerNames));
                currentPlayers.add(this.playerName);

                Set<String> playersToRemove = new HashSet<>(otherPlayers.keySet());
                playersToRemove.removeAll(currentPlayers);
                for (String nameToRemove : playersToRemove) {
                    removeRemotePlayer(nameToRemove);
                }

                 for (String name : playerNames) {
                     if (!name.equals(this.playerName) && !otherPlayers.containsKey(name)) {
                         createVisualForRemotePlayer(name, 100, 100);
                     }
                 }
            }
        } else if (message.startsWith("chat:")) {
            String chatMessage = message.substring("chat:".length());
             System.out.println("CHAT: " + chatMessage);
        } else if (message.equals("game_started")) {
            handleGameStarted();
        } else if (message.startsWith("catch:")) {
            String caughtPlayer = message.substring("catch:".length());
            System.out.println(caughtPlayer + " was caught!");
        } else if (message.startsWith("revive:")) {
            String revivedPlayer = message.substring("revive:".length());
            System.out.println(revivedPlayer + " was revived!");
        } else if (message.startsWith("player_left:")) {
            String leftPlayer = message.substring("player_left:".length());
            if (!leftPlayer.equals(this.playerName)) {
                removeRemotePlayer(leftPlayer);
            }
        }
    }
    
    /**
     * Starts a thread to process incoming UDP updates from the server.
     * Uses the ServerHandler passed during initialization.
     */
    private void startUdpUpdateProcessor() {
        if (serverHandler == null) return;

        sendPlayerPositionUpdate();
        
        Thread updateProcessor = new Thread(() -> {
            while (gameStarted && serverHandler.isConnected()) {
                try {
                    String update = serverHandler.getLastUpdate();
                    if (update != null && !update.isEmpty()) {
                        processUdpUpdate(update);
                    }
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("[UDP_CLIENT] Processor error: " + e.getMessage());
                    break;
                }
            }
        });
        updateProcessor.setDaemon(true);
        updateProcessor.start();
    }
    
    /**
     * Processes UDP updates received from the server.
     *
     * @param update The UDP update message
     */
    private void processUdpUpdate(String update) {
        //System.out.println("[Game_UDP_PROCESS] Processing update: '" + update + "'");
        
        if (update.startsWith("player_position:")) {
            String[] parts = update.split(":");
            if (parts.length != 4) {
                System.err.println("[UDP_CLIENT] Invalid position update: " + update);
                return;
            }

            String remotePlayerName = parts[1];
            try {
                int x = Integer.parseInt(parts[2]);
                int y = Integer.parseInt(parts[3]);

                //System.out.println("[Game_UDP_PROCESS] Parsed position update for '" + remotePlayerName + 
                                 //"' at (" + x + "," + y + ")");

                String confirmedNickname = serverHandler.getConfirmedNickname();
                if (confirmedNickname == null) {
                    System.err.println("[Game_UDP_PROCESS] Cannot process update - confirmed nickname is null");
                    return;
                }
                if (remotePlayerName.equals(confirmedNickname)) {
                    return;
                }
                if (otherPlayers.containsKey(remotePlayerName)) {
                    updateRemotePlayerPosition(remotePlayerName, x, y);
                    return;
                }
                createVisualForRemotePlayer(remotePlayerName, x, y);
            } catch (NumberFormatException e) {
                System.err.println("[UDP_CLIENT] Invalid coordinates: " + e.getMessage());
            }
        } else if (update.startsWith("udp_ack:")) {
            System.out.println("[Game_UDP_RECV] Received UDP acknowledgment from server");
        } else {
            System.out.println("[Game_UDP_RECV] Unrecognized UDP message format: '" + update + "'");
        }
    }
    
    /**
     * Updates the position of a remote player.
     *
     * @param playerName The name of the remote player
     * @param x The new x-coordinate
     * @param y The new y-coordinate
     */
    private void updateRemotePlayerPosition(String playerName, int x, int y) {
        //System.out.println("[Game_RemotePlayer] Attempting to update player '" + playerName + "' to position (" + x + "," + y + ")");
        
        if (otherPlayers.containsKey(playerName)) {
            Player remotePlayer = otherPlayers.get(playerName);
            Platform.runLater(() -> {
                remotePlayer.updatePosition(x, y);
                //System.out.println("[Game_RemotePlayer] Updated existing player '" + playerName + "' to position (" + x + "," + y + ")");
            });
        } else {
            System.out.println("[Game_DEBUG] Player '" + playerName + "' not found visually, creating at (" + x + ", " + y + ").");
            createVisualForRemotePlayer(playerName, x, y);
        }
    }
    
    /**
     * Creates a visual representation for a remote player.
     *
     * @param playerName The name of the remote player
     * @param x The x-coordinate
     * @param y The y-coordinate
     */
    private void createVisualForRemotePlayer(String playerName, int x, int y) {
        System.out.println("Creating visual for player: " + playerName);
        
        if (otherPlayers.containsKey(playerName)) {
            System.out.println("[Game_RemotePlayer] Player '" + playerName + "' already exists, updating position instead");
            updateRemotePlayerPosition(playerName, x, y);
            return;
        }
        
        Color playerColor = Color.ORANGE;
        
        try {
            final Player[] remotePlayerRef = new Player[1];
            Platform.runLater(() -> {
                if (!otherPlayers.containsKey(playerName)) { 
                    remotePlayerRef[0] = new Player(gamePane, gamePane.getPrefWidth(), gamePane.getPrefHeight(), CAMERA_ZOOM,
                                               x, y, (int)PLAYER_WIDTH, (int)PLAYER_HEIGHT, playerColor, playerName);
                    
                    otherPlayers.put(playerName, remotePlayerRef[0]);
                    
                    System.out.println("[Game_RemotePlayer] Added visual for player: " + playerName + " with username: " + playerName);
                }
            });
            
            Thread.sleep(50);
            
            if (remotePlayerRef[0] != null) {
                Platform.runLater(() -> {
                    if (!gamePane.getChildren().contains(remotePlayerRef[0].getVisualRepresentation())) {
                        gamePane.getChildren().add(remotePlayerRef[0].getVisualRepresentation());
                    }
                    if (!gamePane.getChildren().contains(remotePlayerRef[0].getUsernameLabel())) {
                        gamePane.getChildren().add(remotePlayerRef[0].getUsernameLabel());
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("[Game_RemotePlayer] Error creating visual for player: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Removes the visual representation of a remote player.
     *
     * @param remotePlayerName The name of the player to remove.
     */
    private void removeRemotePlayer(String remotePlayerName) {
        String confirmedNickname = serverHandler.getConfirmedNickname();
        if (confirmedNickname == null) {
            System.err.println("[Game_REMOVE] Cannot remove player - confirmed nickname is null");
            return;
        }
        
        if (remotePlayerName.equals(confirmedNickname)) {
            System.out.println("[Game_REMOVE] Ignoring removal of ourselves");
            return;
        }
        
        Player removedPlayer = otherPlayers.remove(remotePlayerName);
        if (removedPlayer != null) {
            Platform.runLater(() -> {
                gamePane.getChildren().remove(removedPlayer.getVisualRepresentation());
                gamePane.getChildren().remove(removedPlayer.getUsernameLabel());
            });
            System.out.println("Removed visual for player: " + remotePlayerName);
        }
    }

    /**
     * Updates the game state based on the elapsed time since the last update.
     * Handles player movement based on active keys and checks for collisions.
     * Sends local player position updates to the server.
     *
     * @param deltaTime The time elapsed since the last update in seconds
     */
    private void update(double deltaTime) {
        if (player == null || !gameStarted) return;

        double currentX = player.getX();
        double currentY = player.getY();
        double newX = currentX;
        double newY = currentY;

        double dx = 0;
        double dy = 0;

        if (activeKeys.contains(KeyCode.W) || activeKeys.contains(KeyCode.UP)) {
            dy -= MOVEMENT_SPEED * deltaTime;
        }
        if (activeKeys.contains(KeyCode.S) || activeKeys.contains(KeyCode.DOWN)) {
            dy += MOVEMENT_SPEED * deltaTime;
        }
        if (activeKeys.contains(KeyCode.A) || activeKeys.contains(KeyCode.LEFT)) {
            dx -= MOVEMENT_SPEED * deltaTime;
        }
        if (activeKeys.contains(KeyCode.D) || activeKeys.contains(KeyCode.RIGHT)) {
            dx += MOVEMENT_SPEED * deltaTime;
        }

        double potentialX = currentX + dx;
        double potentialY = currentY + dy;

        boolean canMoveX = true;
        if (dx != 0) {
            for (Wall wall : gameMap.getCollisionWalls()) {
                if (player.collidesWithWall((int)potentialX, (int)currentY, wall)) {
                    canMoveX = false;
                    break;
                }
            }
            if (canMoveX) {
                newX = potentialX;
            }
        }

        boolean canMoveY = true;
        if (dy != 0) {
            for (Wall wall : gameMap.getCollisionWalls()) {
                if (player.collidesWithWall((int)newX, (int)potentialY, wall)) {
                    canMoveY = false;
                    break;
                }
            }
            if (canMoveY) {
                newY = potentialY;
            }
        }

        boolean positionChanged = Math.abs(newX - currentX) > 0.01 || Math.abs(newY - currentY) > 0.01;
        if (positionChanged) {
            player.updatePosition(newX, newY);
        }

        long currentTime = System.currentTimeMillis();
        if (positionChanged || (currentTime - lastPositionUpdate > POSITION_UPDATE_INTERVAL)) {
            sendPlayerPositionUpdate();
            lastPositionUpdate = currentTime;
        }

        if (activeCamera != null) {
            activeCamera.centerOn(player.getX() + (player.getWidth() / 2), player.getY() + (player.getHeight() / 2));
        }
    }
    
    /**
     * Sends the player's current position to the server via UDP.
     * The update includes the player name, lobby code, and x,y coordinates.
     */
    private void sendPlayerPositionUpdate() {
        if (serverHandler != null && serverHandler.isConnected() && player != null) {
            int x = (int)player.getX();
            int y = (int)player.getY();
            
            String confirmedNickname = serverHandler.getConfirmedNickname();
            if (confirmedNickname == null) {
                System.err.println("[Game_UDP_SEND] Cannot send position update - confirmed nickname is null");
                return;
            }
            
            String updateMessage = String.format("position:%s:%s:%d:%d",
                                               confirmedNickname, lobbyCode, x, y);
            //System.out.println("[Game_UDP_SEND] Sending position update: " + updateMessage);
            serverHandler.sendUpdate(updateMessage);
        }
    }
    
    /**
     * Handles the game started event (defensive).
     * Mainly ensures player is controllable.
     */
    private void handleGameStarted() {
        gameStarted = true;
        Platform.runLater(() -> {
            if(player != null) {
                 player.setSpectated(false);
            }
        });
    }
    
    /**
     * The main entry point for launching the Game application *directly*.
     * This is generally NOT used; Game should be launched via LobbyGUI.
     * Kept for potential testing purposes.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        System.err.println("Warning: Launching Game directly via main() requires manual initialization setup.");
        launch(args);
    }
} 