package igoat.client;

import igoat.Role;
import igoat.Timer;
import igoat.client.GUI.Banner;
import igoat.client.GUI.LobbyGUI;
import igoat.client.GUI.MainMenuGUI;
import igoat.client.GUI.SettingsWindow;
import igoat.client.GUI.SoundButton;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spectator mode for iGoat: allows joining an existing game as a spectator.
 */
public class GameSpectator extends Application {
    private static final Logger logger = LoggerFactory.getLogger(GameSpectator.class);
    private static final double CAMERA_ZOOM = 3;
    private static final double VISION_RADIUS = 1000000000;
    private final String style = getClass().getResource("/CSS/UI.css").toExternalForm();
    private ResourceBundle translations;

    private Pane gamePane;
    private Pane uiOverlay;
    private Stage stage;
    private double windowWidth;
    private double windowHeight;
    private LobbyGUI lobby;
    private igoat.client.Map gameMap;
    private Camera camera;
    private ServerHandler serverHandler;
    private String lobbyCode;
    private boolean gameStarted = false;
    private final LinkedHashMap<String, Player> otherPlayers = new LinkedHashMap<>();
    private final HashMapCycler<String, Player> spectatingPlayer = new HashMapCycler<>(otherPlayers);
    private final Map<String, Role> pendingRoles = new ConcurrentHashMap<>();
    private boolean initializedViewport = false;
    private Timer timer = new Timer();
    private String time = "";
    private Text timeText;
    private Banner caughtBanner;
    private Banner reviveBanner;
    private Banner allTerminalsBanner;
    private Banner terminalActivationBanner;
    private Banner noActivationBanner;
    private HBox spectatorInfoBar;
    private VBox playerListBox;
    private VBox playerInfoBox;

    private static class InterpolatedPosition {
        double lastX, lastY, targetX, targetY;
        long lastUpdateTime, targetTime;
        InterpolatedPosition(double x, double y) {
            lastX = targetX = x;
            lastY = targetY = y;
            lastUpdateTime = targetTime = System.currentTimeMillis();
        }
    }
    private final Map<String, InterpolatedPosition> playerPositions = new ConcurrentHashMap<>();
    private final SoundManager sound = SoundManager.getInstance();

    /**
     * Constructs a new GameSpectator instance.
     * @param lobby The LobbyGUI instance to return to when exiting spectator mode.
     */
    public GameSpectator(LobbyGUI lobby) {
        this.lobby = lobby;
    }

    /**
     * Initializes the spectator mode with server connection and lobby code.
     * Starts background threads for message and UDP update processing.
     * @param handler The ServerHandler for communication with the server.
     * @param code The lobby code being spectated.
     */
    public void initialize(ServerHandler handler, String code) {
        this.serverHandler = handler;
        this.lobbyCode = code;
        this.gameStarted = true;
        startMessageProcessor();
        startUdpUpdateProcessor();
        if (this.serverHandler != null && this.serverHandler.isConnected()) {
            this.serverHandler.sendMessage("getroles:");
        }
    }

    /**
     * Starts the JavaFX application for spectator mode.
     * Sets up the UI overlays, camera, and event handlers.
     * @param primaryStage The primary stage for this spectator window.
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            sound.playSoundtrack();
            
            translations = ResourceBundle.getBundle("lang.text");
            stage = primaryStage;
            stage.getIcons().add(MainMenuGUI.icon);
            stage.setOnCloseRequest(event -> exit());
            stage.setFullScreenExitKeyCombination(null);
            SettingsWindow settings = SettingsWindow.getInstance();
            stage.setFullScreen(false);
            settings.setGameStage(stage);
            if (serverHandler == null || lobbyCode == null) {
                showAlert(Alert.AlertType.ERROR, translations.getString("game.noConnectionError"));
                returnToLobby();
                return;
            }
            serverHandler.sendMessage("ready:");
            gameMap = new igoat.client.Map(false);
            gamePane = new Pane();
            gamePane.setMinSize(gameMap.getWidth(), gameMap.getHeight());
            gamePane.setMaxSize(gameMap.getWidth(), gameMap.getHeight());
            gamePane.setPrefSize(gameMap.getWidth(), gameMap.getHeight());
            gamePane.setClip(new Rectangle(0, 0, gameMap.getWidth(), gameMap.getHeight()));
            Sprite floor = new Sprite("sprites/floor_tile01.png");
            gamePane.setBackground(floor.getBackground());
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            double screenWidth = screenBounds.getWidth() * 0.8;
            double screenHeight = screenBounds.getHeight() * 0.8;
            double mapAspectRatio = (double)gameMap.getWidth() / gameMap.getHeight();
            if (screenWidth / screenHeight > mapAspectRatio) {
                windowHeight = screenHeight;
                windowWidth = screenHeight * mapAspectRatio;
            } else {
                windowWidth = screenWidth;
                windowHeight = screenWidth / mapAspectRatio;
            }
            stage.setWidth(windowWidth);
            stage.setHeight(windowHeight);
            stage.setFullScreenExitHint("");
            Pane container = new Pane();
            container.setStyle("-fx-background-color: black;");
            container.getChildren().add(gamePane);
            uiOverlay = new Pane();
            uiOverlay.getStylesheets().add(style);
            uiOverlay.setMouseTransparent(true);
            uiOverlay.prefWidthProperty().bind(stage.widthProperty());
            uiOverlay.prefHeightProperty().bind(stage.heightProperty());
            container.getChildren().add(uiOverlay);
            timeText = new Text();
            timeText.setX(10);
            timeText.setY(20);
            timeText.setFill(Color.WHITE);
            timeText.setFont(new Font("Jersey 10", 25));
            container.getChildren().add(timeText);
            playerListBox = new VBox(10);
            playerListBox.setPadding(new Insets(20, 10, 20, 10));
            playerListBox.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-background-radius: 10;");
            playerListBox.setPrefWidth(180);
            playerListBox.setAlignment(Pos.TOP_LEFT);
            playerListBox.setLayoutX(10);
            playerListBox.layoutYProperty().bind(uiOverlay.heightProperty().multiply(0.15));
            uiOverlay.getChildren().add(playerListBox);
            playerInfoBox = new VBox(15);
            playerInfoBox.setPadding(new Insets(20, 10, 20, 10));
            playerInfoBox.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-background-radius: 10;");
            playerInfoBox.setPrefWidth(220);
            playerInfoBox.setAlignment(Pos.TOP_RIGHT);
            playerInfoBox.layoutYProperty().bind(uiOverlay.heightProperty().multiply(0.15));
            playerInfoBox.layoutXProperty().bind(uiOverlay.widthProperty().subtract(playerInfoBox.getPrefWidth()).subtract(10));
            uiOverlay.getChildren().add(playerInfoBox);
            spectatorInfoBar = new HBox();
            spectatorInfoBar.setAlignment(Pos.CENTER);
            spectatorInfoBar.setSpacing(20);
            spectatorInfoBar.setPadding(new Insets(10));
            spectatorInfoBar.setStyle("-fx-background-color: rgba(0,0,0,0.85); -fx-border-radius: 10; -fx-background-radius: 10;");
            spectatorInfoBar.prefWidthProperty().bind(stage.widthProperty());
            spectatorInfoBar.setPrefHeight(40);
            spectatorInfoBar.setMouseTransparent(true);
            spectatorInfoBar.setLayoutY(10);
            uiOverlay.getChildren().add(spectatorInfoBar);
            terminalActivationBanner = Banner.terminalActivation(uiOverlay);
            allTerminalsBanner = Banner.allTerminals(uiOverlay);
            noActivationBanner = Banner.noActivation(uiOverlay);
            reviveBanner = Banner.revive(uiOverlay);
            caughtBanner = Banner.caught(uiOverlay);
            for (Node wall : gameMap.getVisualWalls()) {
                gamePane.getChildren().add(wall);
            }
            for (ImageView decor : gameMap.getDecorItems()) {
                gamePane.getChildren().add(decor);
            }
            camera = new Camera(gamePane, primaryStage.getWidth(), primaryStage.getHeight(), CAMERA_ZOOM, false);
            Scene scene = new Scene(container);
            scene.setFill(Color.BLACK);
            String windowTitle = String.format(translations.getString("spectator.title"), lobbyCode);
            primaryStage.setTitle(windowTitle);
            primaryStage.setScene(scene);
            primaryStage.show();
            scene.widthProperty().addListener((obs, oldVal, newVal) -> {
                camera.updateViewport(newVal.doubleValue(), scene.getHeight());
                windowWidth = newVal.doubleValue();
                updateVisuals();
            });
            scene.heightProperty().addListener((obs, oldVal, newVal) -> {
                camera.updateViewport(scene.getWidth(), newVal.doubleValue());
                windowHeight = newVal.doubleValue();
                updateVisuals();
            });
            primaryStage.fullScreenProperty().addListener((obs, oldVal, newVal) -> {
                Platform.runLater(() -> {
                    camera.updateViewport(scene.getWidth(), scene.getHeight());
                });
            });
            AnimationTimer mainLoop = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    updateVisuals();
                    if (serverHandler == null || !serverHandler.isConnected()) {
                        logger.error("Connection was closed");
                        stop();
                        exit();
                    }
                }
            };
            mainLoop.start();
            timer.reset(0);
            time = "0:0";
            scene.setOnKeyPressed(event -> {
                switch (event.getCode()) {
                    case RIGHT, D, TAB -> rotateSpectator(1);
                    case LEFT, A -> rotateSpectator(-1);
                    case ESCAPE -> returnToLobby();
                }
            });
        } catch (Exception e) {
            logger.error("Error starting spectator mode", e);
        }
    }

    /**
     * Rotates the spectator view to the next or previous player.
     * @param direction +1 for next, -1 for previous
     */
    private void rotateSpectator(int direction) {
        logger.info("rotateSpectator called, direction: {}", direction);
        if (otherPlayers.isEmpty()) {
            logger.warn("No players to spectate");
            updateVisuals();
            return;
        }
        if (direction > 0) {
            do {
                spectatingPlayer.nextValue();
            } while (spectatingPlayer.getCurrentValue() == null);
        } else {
            do {
                spectatingPlayer.nextValue();
            } while (spectatingPlayer.getCurrentValue() == null);
        }
        logger.info("Now spectating: {}", spectatingPlayer.getCurrentValue() != null ? spectatingPlayer.getCurrentValue().getUsername() : "null");
        updateVisuals();
    }

    /**
     * Updates the top info bar with the currently spectated player's info.
     */
    private void updateSpectatorInfoBar() {
        spectatorInfoBar.getChildren().clear();
        Player spectated = spectatingPlayer.getCurrentValue();
        if (otherPlayers.isEmpty() || spectated == null) {
            Label info = new Label(translations.getString("spectator.noPlayers"));
            info.setTextFill(Color.WHITE);
            info.setFont(Font.font("Jersey 10", FontWeight.BOLD, 18));
            info.setStyle("-fx-font-family: 'Jersey 10', 'Courier New', monospace; -fx-text-fill: white !important;");
            spectatorInfoBar.getChildren().add(info);
            updatePlayerListBox();
            updatePlayerInfoBox(null);
            return;
        }
        String name = spectated.getUsername();
        String role = spectated.getRole() != null ? spectated.getRole().name() : translations.getString("spectator.unknown");
        Label info = new Label(String.format(translations.getString("spectator.spectating"), name, role));
        info.setTextFill(Color.WHITE);
        info.setFont(Font.font("Jersey 10", FontWeight.BOLD, 18));
        info.setStyle("-fx-font-family: 'Jersey 10', 'Courier New', monospace; -fx-text-fill: white !important;");
        Label hint = new Label(translations.getString("spectator.switchHint"));
        hint.setTextFill(Color.LIGHTGRAY);
        hint.setFont(Font.font("Jersey 10", 14));
        hint.setStyle("-fx-font-family: 'Jersey 10', 'Courier New', monospace; -fx-text-fill: #cccccc !important;");
        spectatorInfoBar.getChildren().addAll(info, hint);
        updatePlayerListBox();
        updatePlayerInfoBox(spectated);
    }

    /**
     * Updates the player list overlay on the left.
     */
    private void updatePlayerListBox() {
        playerListBox.getChildren().clear();
        Player spectated = spectatingPlayer.getCurrentValue();
        for (Player p : otherPlayers.values()) {
            Label label = new Label(p.getUsername());
            label.setFont(Font.font("Jersey 10", FontWeight.BOLD, 16));
            if (spectated != null && p.getUsername().equals(spectated.getUsername())) {
                label.setTextFill(Color.YELLOW);
                label.setStyle("-fx-font-family: 'Jersey 10', 'Courier New', monospace; -fx-background-color: #333333; -fx-border-color: yellow; -fx-border-width: 2; -fx-background-radius: 8; -fx-border-radius: 8; -fx-text-fill: yellow !important;");
            } else {
                label.setTextFill(Color.WHITE);
                label.setStyle("-fx-font-family: 'Jersey 10', 'Courier New', monospace; -fx-background-color: transparent; -fx-text-fill: white !important;");
            }
            playerListBox.getChildren().add(label);
        }
    }

    /**
     * Updates the player info overlay on the right for the currently spectated player.
     * @param spectated The player being spectated, or null.
     */
    private void updatePlayerInfoBox(Player spectated) {
        playerInfoBox.getChildren().clear();
        if (spectated == null) return;
        Label name = new Label(String.format(translations.getString("spectator.name"), spectated.getUsername()));
        name.setFont(Font.font("Jersey 10", FontWeight.BOLD, 18));
        name.setStyle("-fx-font-family: 'Jersey 10', 'Courier New', monospace; -fx-text-fill: white !important;");
        String roleStr = spectated.getRole() != null ? spectated.getRole().name() : translations.getString("spectator.unknown");
        Label role = new Label(String.format(translations.getString("spectator.role"), roleStr));
        role.setFont(Font.font("Jersey 10", FontWeight.BOLD, 16));
        role.setStyle("-fx-font-family: 'Jersey 10', 'Courier New', monospace; -fx-text-fill: #ffffcc !important;");
        String statusStr = spectated.isDown() ? translations.getString("spectator.caught") : translations.getString("spectator.active");
        Label status = new Label(String.format(translations.getString("spectator.status"), statusStr));
        status.setFont(Font.font("Jersey 10", FontWeight.BOLD, 16));
        status.setStyle(spectated.isDown() ? "-fx-font-family: 'Jersey 10', 'Courier New', monospace; -fx-text-fill: #ff4444 !important;" : "-fx-font-family: 'Jersey 10', 'Courier New', monospace; -fx-text-fill: #00ff00 !important;");
        playerInfoBox.getChildren().addAll(name, role, status);
    }

    /**
     * Updates all visuals, including interpolated player positions, overlays, and camera.
     * Called on every animation frame.
     */
    private void updateVisuals() {
        Player spectated = spectatingPlayer.getCurrentValue();
        updateSpectatorInfoBar();
        if (spectated == null) {
            spectatorInfoBar.toFront();
            return;
        }
        // Interpolate all remote players
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Player> entry : otherPlayers.entrySet()) {
            String name = entry.getKey();
            Player p = entry.getValue();
            InterpolatedPosition interp = playerPositions.get(name);
            if (interp != null) {
                double t = Math.min(1.0, (now - interp.lastUpdateTime) / (double)(interp.targetTime - interp.lastUpdateTime));
                double ix = interp.lastX + (interp.targetX - interp.lastX) * t;
                double iy = interp.lastY + (interp.targetY - interp.lastY) * t;
                p.updatePosition(ix, iy);
            }
        }
        double centerX = spectated.getX() + (spectated.getWidth() / 2.0);
        double centerY = spectated.getY() + (spectated.getHeight() / 2.0);
        for (Player other : otherPlayers.values()) {
            other.getVisual().setClip(null);
            other.getUsernameLabel().setClip(null);
        }
        for (Terminal terminal : gameMap.getTerminalList()) {
            terminal.setClip(null);
        }
        for (IgoatStation station : gameMap.getStationList()) {
            station.setClip(null);
        }
        camera.update(centerX, centerY);
        timer.update();
        if (!time.isEmpty() && !time.equals(timer.toString())) {
            time = timer.toString();
            timeText.setText(time);
        }
    }

    /**
     * Returns to the lobby screen, notifies the server, and cleans up spectator state.
     */
    private void returnToLobby() {
        gameStarted = false;
        sound.stopAll();
        if (serverHandler != null && serverHandler.isConnected() && lobbyCode != null) {
            serverHandler.sendMessage("leaveSpectate:" + lobbyCode);
        }
        stage.close();
        lobby.getStage().show();
        lobby.initializeServerCommunication();
    }

    /**
     * Exits spectator mode, notifies the server, and closes the window.
     */
    private void exit() {
        gameStarted = false;
        if (serverHandler != null && serverHandler.isConnected() && lobbyCode != null) {
            serverHandler.sendMessage("leaveSpectate:" + lobbyCode);
        }
        stage.close();
        lobby.exit();
    }

    private void showAlert(Alert.AlertType type, String content) {
        Platform.runLater(() -> {
            logger.error("Error in spectator mode: {}", content);
            Alert alert = new Alert(type);
            alert.setTitle(translations.getString("game.initError"));
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
            if (type == Alert.AlertType.ERROR) {
                returnToLobby();
            }
        });
    }

    /**
     * Starts a background thread to process TCP messages from the server.
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
                    logger.error("Message processor interrupted.");
                    break;
                } catch (Exception e) {
                    logger.error("Error in message processor", e);
                    break;
                }
            }
        });
        messageProcessor.setDaemon(true);
        messageProcessor.start();
    }

    /**
     * Processes a single TCP message from the server.
     * Handles player list, chat, game events, etc.
     * @param message The message from the server
     */
    private void processServerMessage(String message) {
        if (message.startsWith("error:")) {
            String errorMsg = message.substring(6);
            if (errorMsg.equals("server.inProgressError")) {
                showAlert(Alert.AlertType.ERROR, translations.getString("server.inProgressError"));
            } else {
                showAlert(Alert.AlertType.ERROR, translations.getString(errorMsg));
            }
            return;
        }
        if (message.startsWith("timer:")) {
            String[] parts = message.split(":");
            if (parts.length == 3) {
                String code = parts[1];
                String msStr = parts[2];
                if (lobbyCode != null && lobbyCode.equals(code)) {
                    try {
                        long ms = Long.parseLong(msStr);
                        Platform.runLater(() -> {
                            timer.reset(ms);
                            time = timer.toString();
                            timeText.setText(time);
                        });
                    } catch (NumberFormatException e) {
                        logger.error("Invalid timer value: {}", msStr);
                    }
                }
            }
            return;
        }
        if (message.startsWith("getlobbyplayers:")) {
            String playersData = message.substring("getlobbyplayers:".length());
            if (!playersData.isEmpty()) {
                String[] playerNames = playersData.split(",");
                Set<String> currentPlayers = Set.of(playerNames);
                Set<String> playersToRemove = Set.copyOf(otherPlayers.keySet());
                playersToRemove.removeAll(currentPlayers);
                for (String nameToRemove : playersToRemove) {
                    removeRemotePlayer(nameToRemove);
                }
                for (String name : playerNames) {
                    if (!otherPlayers.containsKey(name)) {
                        createVisualForRemotePlayer(name, 100, 100);
                    }
                }
            }
        } else if (message.startsWith("catch:")) {
            String caughtPlayerName = message.substring("catch:".length());
            caughtBanner.showAnimation(caughtPlayerName + " was caught!", 2);
        } else if (message.startsWith("revive:")) {
            String revivedPlayerName = message.substring("revive:".length());
            reviveBanner.showAnimation(revivedPlayerName + " was freed!", 2);
        } else if (message.startsWith("player_left:")) {
            String leftPlayer = message.substring("player_left:".length());
            removeRemotePlayer(leftPlayer);
        } else if (message.startsWith("terminal:")) {
            String[] parts = message.split(":");
            if (parts.length == 2) {
                activateTerminal(Integer.parseInt(parts[1]));
            }
        } else if (message.startsWith("activateStation:")) {
            String[] parts = message.split(":");
            if (parts.length == 2) {
                activateStation(Integer.parseInt(parts[1]));
            }
        } else if (message.startsWith("role:")) {
            String[] parts = message.split(":");
            if (parts.length == 3) {
                String playerName = parts[1];
                try {
                    Role role = Role.valueOf(parts[2]);
                    Platform.runLater(() -> {
                        Player other = otherPlayers.get(playerName);
                        if (other != null) {
                            other.setRole(role);
                        } else {
                            pendingRoles.put(playerName, role);
                        }
                    });
                } catch (IllegalArgumentException e) {
                    logger.error("Invalid role value in message: {}", message);
                }
            }
        } else if (message.startsWith("roles:")) {
            String rolesData = message.substring("roles:".length());
            if (!rolesData.isEmpty()) {
                String[] roleEntries = rolesData.split(",");
                for (String entry : roleEntries) {
                    String[] parts = entry.split("=");
                    if (parts.length == 2) {
                        String playerName = parts[0];
                        try {
                            Role role = Role.valueOf(parts[1]);
                            Platform.runLater(() -> {
                                Player other = otherPlayers.get(playerName);
                                if (other != null) {
                                    other.setRole(role);
                                } else {
                                    pendingRoles.put(playerName, role);
                                }
                            });
                        } catch (IllegalArgumentException e) {
                            logger.error("Invalid role value in roles message entry: {}", entry);
                        }
                    }
                }
            }
        } else if (message.equals("door")) {
            Platform.runLater(this::handleDoorsOpen);
        } else if (message.startsWith("gameover:")) {
            String[] parts = message.split(":");
            if (parts.length == 2) {
                endGame(parts[1].equals("true"));
            }
        }
    }

    /**
     * Ends the game and shows the win/loss screen.
     * @param result true if guard won, false otherwise
     */
    private void endGame(boolean result) {
        time = "";
        showWinLossScreen(result);
    }

    /**
     * Shows the win/loss screen with a scoreboard for all players.
     * @param guardWon true if guard won, false otherwise
     */
    private void showWinLossScreen(boolean guardWon) {
        Platform.runLater(() -> {
            Text title = new Text(translations.getString("spectator.gameOver"));
            title.setFont(Font.font("Jersey 10", 40));
            title.setFill(Color.BLUE);
            GridPane grid = new GridPane();
            grid.setAlignment(Pos.CENTER);
            grid.setHgap(20);
            grid.setVgap(10);
            Label headerName = new Label(translations.getString("hs.player"));
            headerName.setFont(Font.font("Jersey 10", FontWeight.BOLD, 14));
            headerName.setStyle("-fx-background-color: #343a40; -fx-text-fill: white; -fx-padding: 5;");
            Label headerRole = new Label(translations.getString("hs.role"));
            headerRole.setFont(Font.font("Jersey 10", FontWeight.BOLD, 14));
            headerRole.setStyle("-fx-background-color: #343a40; -fx-text-fill: white; -fx-padding: 5;");
            Label headerResult = new Label(translations.getString("hs.outcome"));
            headerResult.setFont(Font.font("Jersey 10", FontWeight.BOLD, 14));
            headerResult.setStyle("-fx-background-color: #343a40; -fx-text-fill: white; -fx-padding: 5;");
            grid.add(headerName, 0, 0);
            grid.add(headerRole, 1, 0);
            grid.add(headerResult, 2, 0);
            List<Player> allPlayers = new ArrayList<>(otherPlayers.values());
            allPlayers.sort(Comparator.comparing(Player::getUsername));
            for (int i = 0; i < allPlayers.size(); i++) {
                Player p = allPlayers.get(i);
                if (p.getRole() == null) continue;
                boolean isWinner = (guardWon && p.getRole() == Role.GUARD) || (!guardWon && p.getRole() != Role.GUARD);
                Label nameLabel = new Label(p.getUsername());
                Label roleLabel = new Label(p.getRole().name());
                Label resultLabel = new Label(isWinner ? translations.getString("hs.won") : translations.getString("hs.lost"));
                String rowStyle = isWinner
                    ? "-fx-background-color: #d4edda; -fx-text-fill: #155724; -fx-padding: 5;"
                    : "-fx-background-color: #f8d7da; -fx-text-fill: #721c24; -fx-padding: 5;";
                nameLabel.setStyle(rowStyle);
                roleLabel.setStyle(rowStyle);
                resultLabel.setStyle(rowStyle);
                grid.add(nameLabel, 0, i + 1);
                grid.add(roleLabel, 1, i + 1);
                grid.add(resultLabel, 2, i + 1);
            }
            SoundButton exitButton = new SoundButton(translations.getString("spectator.exitSpectator"));
            exitButton.setOnAction(e -> returnToLobby());
            VBox layout = new VBox(20, title, grid, exitButton);
            layout.setAlignment(Pos.CENTER);
            layout.setStyle("-fx-background-color: #dff0d8; -fx-padding: 20;");
            Scene scene = new Scene(layout);
            scene.getStylesheets().add(style);
            stage.setScene(scene);
            stage.sizeToScene();
            stage.show();
        });
    }

    /**
     * Handles the event when all doors are opened.
     */
    private void handleDoorsOpen() {
        if (gameMap != null) {
            gameMap.openDoors();
        }
        allTerminalsBanner.showAnimation("All Terminals Activated! Exits Open!", 4);
    }

    /**
     * Starts a background thread to process UDP updates from the server.
     */
    private void startUdpUpdateProcessor() {
        if (serverHandler == null) return;
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
                    logger.error("Processor error", e);
                }
            }
        });
        updateProcessor.setDaemon(true);
        updateProcessor.start();
    }

    /**
     * Processes a single UDP update from the server (e.g., player position).
     * @param update The UDP update message
     */
    private void processUdpUpdate(String update) {
        if (update.startsWith("player_position:")) {
            String[] parts = update.split(":");
            if (parts.length != 4) {
                logger.error("Invalid position update: {}", update);
                return;
            }
            String playerName = parts[1];
            try {
                int x = Integer.parseInt(parts[2]);
                int y = Integer.parseInt(parts[3]);
                if (otherPlayers.containsKey(playerName)) {
                    updateRemotePlayerPosition(playerName, x, y);
                } else {
                    createVisualForRemotePlayer(playerName, x, y);
                }
            } catch (NumberFormatException e) {
                logger.error("Invalid coordinates in update: {}", update, e);
            }
        } else if (update.equals("door")) {
            Platform.runLater(this::handleDoorsOpen);
        }
    }

    /**
     * Updates the interpolated position for a remote player.
     * @param playerName The player's username
     * @param x The new x coordinate
     * @param y The new y coordinate
     */
    private void updateRemotePlayerPosition(String playerName, int x, int y) {
        if (otherPlayers.containsKey(playerName)) {
            Player remotePlayer = otherPlayers.get(playerName);
            // Interpolate movement
            InterpolatedPosition interp = playerPositions.computeIfAbsent(playerName, k -> new InterpolatedPosition(x, y));
            interp.lastX = remotePlayer.getX();
            interp.lastY = remotePlayer.getY();
            interp.targetX = x;
            interp.targetY = y;
            interp.lastUpdateTime = System.currentTimeMillis();
            interp.targetTime = interp.lastUpdateTime + 100; // 100ms to reach target
        } else {
            createVisualForRemotePlayer(playerName, x, y);
        }
    }

    /**
     * Creates a visual for a remote player if not already present.
     * @param playerName The player's username
     * @param x The x coordinate
     * @param y The y coordinate
     */
    private void createVisualForRemotePlayer(String playerName, int x, int y) {
        logger.info("createVisualForRemotePlayer: {} at {},{}", playerName, x, y);
        Platform.runLater(() -> {
            Player remotePlayer = otherPlayers.get(playerName);
            if (remotePlayer != null) {
                remotePlayer.updatePosition(x, y);
                Role pendingRole = pendingRoles.remove(playerName);
                if (pendingRole != null) {
                    remotePlayer.setRole(pendingRole);
                }
            } else {
                remotePlayer = new Player(gamePane, x, y, playerName);
                otherPlayers.put(playerName, remotePlayer);
                playerPositions.put(playerName, new InterpolatedPosition(x, y));
                if (otherPlayers.size() == 1) {
                    spectatingPlayer.nextValue();
                    logger.info("First player added, now spectating: {}", playerName);
                }
                Role pendingRole = pendingRoles.remove(playerName);
                if (pendingRole != null) {
                    remotePlayer.setRole(pendingRole);
                } else {
                    if (serverHandler != null && serverHandler.isConnected()) {
                        serverHandler.sendMessage("getroles:");
                    }
                }
                updateVisuals();
            }
        });
    }

    /**
     * Activates a terminal and shows a banner.
     * @param id The terminal ID
     */
    private void activateTerminal(int id) {
        if (id == -1) {
            noActivationBanner.showAnimation("Can't activate Terminal", 1.5);
            noActivationBanner.shake();
            return;
        }
        terminalActivationBanner.showAnimation("Terminal " + id + " Activated!", 2.5f);
        for (Terminal terminal : gameMap.getTerminalList()) {
            if (terminal.getTerminalID() == id) {
                terminal.activate();
                return;
            }
        }
    }

    /**
     * Activates a station and shows a banner.
     * @param id The station ID
     */
    private void activateStation(int id) {
        for (IgoatStation station : gameMap.getStationList()) {
            if (station.getStationID() == id) {
                station.activate(station.getX(), station.getY());
                return;
            }
        }
    }

    /**
     * Removes a remote player from the view and cleans up their state.
     * @param remotePlayerName The player's username
     */
    private void removeRemotePlayer(String remotePlayerName) {
        Player p = otherPlayers.get(remotePlayerName);
        if (p != null) {
            p.setIdle();
            otherPlayers.remove(remotePlayerName);
            playerPositions.remove(remotePlayerName);
            if (spectatingPlayer.getCurrentValue() == null || spectatingPlayer.getCurrentValue().getUsername().equals(remotePlayerName)) {
                spectatingPlayer.nextValue();
            }
            updateSpectatorInfoBar();
        }
    }

    /**
     * Main entry point for launching the spectator mode directly (for testing).
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        logger.warn("Warning: Launching GameSpectator directly via main() requires manual initialization setup.");
        launch(args);
    }

    @Override
    public void stop() {
        sound.stopAll();
    }

    public void updateSoundtrackVolume(double volume) {
        sound.setSoundtrackVolume(volume);
    }
} 