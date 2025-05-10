package igoat;

import igoat.client.GUI.MainMenuGUI;
import igoat.client.GUI.SettingsWindow;
import igoat.client.LanguageManager;
import igoat.server.Server;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Properties;
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
    private static final String CONFIG_FILENAME = "igoat_settings.properties";

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
        LanguageManager.init("lang.text", retrieveLanguageSetting());

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
                        username = username.replaceAll("[\\s=:,]", "");
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

    /**
     * Retrieves the language setting from the settings file. Default is English
     */
    private static Locale retrieveLanguageSetting() {
        Properties props = new Properties();

        Path configPath = Paths.get(System.getProperty("user.dir"), CONFIG_FILENAME);
        Locale locale = null;

        if (Files.exists(configPath)) {
            try (InputStream in = new FileInputStream(configPath.toFile())) {
                props.load(in);
                String language = props.getProperty("language", "");
                locale = SettingsWindow.AVAILABLE_LANGUAGES.get(language);
            } catch (IOException e) {
                logger.error("Error opening settings file", e);
            }
        }

        if (locale == null) {
            locale = Locale.ENGLISH;
            props = new Properties();
            try {
                Files.createDirectories(configPath.getParent());

                if (Files.exists(configPath)) {
                    try (InputStream in = new FileInputStream(configPath.toFile())) {
                        props.load(in);
                    }
                }

                props.setProperty("language", "English");

                try (OutputStream out = new FileOutputStream(configPath.toFile())) {
                    props.store(out, null);
                }

            } catch (IOException e) {
                logger.error("Failed to add language to settings", e);
            }
        }
        return locale;
    }
}
