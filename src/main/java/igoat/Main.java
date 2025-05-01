package igoat;

import igoat.client.GUI.MainMenuGUI;
import igoat.server.Server;
import javafx.application.Application;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.stage.Stage;

/**
 * Main entry point for the iGoat application. Handles command-line arguments to start either the
 * GUI, server, or client mode.
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     * Main entry point that processes command-line arguments and launches appropriate mode.
     * - No arguments: Launches GUI mode
     * - "server" argument: Starts server mode (requires port)
     * - "client" argument: Starts client mode (requires host, port)
     *
     * @param args Command line arguments:
     *            - [] (empty): Launch GUI
     *            - ["server", port]
     *            - ["client", host, port]
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            try {
                Platform.startup(() -> {});
                Application.launch(MainMenuGUI.class, args);
            } catch (Exception e) {
                logger.error("Error launching GUI", e);
                System.exit(1);
            }
        } else {
            switch (args[0].toLowerCase()) {
                case "server":
                    if (args.length < 2) {
                        logger.warn("Usage: java -jar igoat.jar server <port>");
                        System.exit(1);
                    }
                    try {
                        int port = Integer.parseInt(args[1]);
                        Server.main(new String[]{String.valueOf(port)});
                    } catch (NumberFormatException e) {
                        logger.error("Invalid port number", e);
                        System.exit(1);
                    }
                    break;

                case "client":
                    if (args.length < 2) {
                        logger.warn("Usage: java -jar igoat.jar client <host:port> [username]");
                        System.exit(1);
                    }
                    try {
                        String[] hostPort = args[1].split(":");
                        if (hostPort.length != 2) {
                            logger.warn("Invalid format. Use host:port");
                            System.exit(1);
                        }
                        String host = hostPort[0];
                        int port = Integer.parseInt(hostPort[1]);
                        String username = args.length > 2 ? args[2] : System.getProperty("user.name");
                        // sanitize string
                        username = username.replaceAll("[\\s=:]", "");
                        if (username.isEmpty()) {
                            logger.error("invalid username");
                            System.exit(1);
                        }
                        logger.info("Using username: {}", username);

                        String finalUsername = username;
                        Platform.startup(() -> {
                            try {
                                Stage stage = new Stage();
                                MainMenuGUI mainMenu = new MainMenuGUI();
                                mainMenu.start(stage);
                                mainMenu.join(host, port, finalUsername);

                            } catch (Exception e) {
                                logger.error("Couldn't start client: ", e);
                                System.exit(1);
                            }
                        });
                    } catch (NumberFormatException e) {
                        logger.error("Invalid port number", e);
                        System.exit(1);
                    } catch (Exception e) {
                        logger.error("Couldn't start client: ", e);
                        System.exit(1);
                    }
                    break;

                default:
                    logger.warn("Usage: java -jar igoat.jar [server <port> | client <host> <port>]");
                    logger.warn("       java -jar igoat.jar (for GUI mode)");
                    System.exit(1);
            }
        }
    }
}
