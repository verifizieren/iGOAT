package igoat;

import igoat.client.GUI.SettingsWindow;
import igoat.client.GUI.SplashScreen;
import igoat.client.LanguageManager;
import igoat.server.Server;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javafx.application.Application;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the iGoat application. Handles command-line arguments to start either the
 * GUI, server, or client mode.
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String CONFIG_FILENAME = "igoat_settings.properties";

    static {
        try {
            loadNativeLibraries();
        } catch (Exception e) {
            System.err.println("Failed to load native libraries: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void loadNativeLibraries() throws Exception {
        String osName = System.getProperty("os.name").toLowerCase();
        String nativesDir;

        if (osName.contains("mac")) {
            nativesDir = "natives-mac";
        } else if (osName.contains("windows")) {
            nativesDir = "natives-windows";
        } else {
            nativesDir = "natives-linux";
        }

        Path tempDir = Files.createTempDirectory("igoat-natives");
        tempDir.toFile().deleteOnExit();

        URL jarUrl = Main.class.getProtectionDomain().getCodeSource().getLocation();
        String jarPath = jarUrl.getPath();
        if (jarPath.startsWith("/") && osName.contains("windows")) {
            jarPath = jarPath.substring(1);
        }

        if (jarPath.endsWith(".jar")) {
            try (JarFile jar = new JarFile(jarPath)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().startsWith(nativesDir + "/") && !entry.isDirectory()) {
                        String fileName = new File(entry.getName()).getName();
                        Path targetPath = tempDir.resolve(fileName);

                        try (InputStream in = jar.getInputStream(entry)) {
                            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                            if (!osName.contains("windows")) {
                                targetPath.toFile().setExecutable(true);
                            }
                        }
                        logger.info("Extracted native library: " + fileName);
                    }
                }
            }
        }

        String libraryPath = tempDir.toString();
        String existingPath = System.getProperty("java.library.path", "");
        String newLibraryPath = libraryPath + File.pathSeparator + existingPath;

        System.setProperty("java.library.path", newLibraryPath);
        System.setProperty("net.java.games.input.librarypath", libraryPath);

        if (osName.contains("windows")) {
            File[] files = tempDir.toFile().listFiles((dir, name) ->
                name.startsWith("jinput") && name.endsWith(".dll"));

            if (files != null) {
                for (File file : files) {
                    try {
                        System.load(file.getAbsolutePath());
                        logger.info("Loaded native library: " + file.getName());
                    } catch (Exception e) {
                        logger.warn("Failed to load " + file.getName(), e);
                    }
                }
            }
        }
    }

    /**
     * Main entry point that processes command-line arguments and launches appropriate mode. - No
     * arguments: Launches GUI mode - "server" argument: Starts server mode (requires port) -
     * "client" argument: Starts client mode (requires host, port)
     *
     * @param args Command line arguments: - [] (empty): Launch GUI - ["server", port] - ["client",
     *             host, port]
     */
    public static void main(String[] args) {
        LanguageManager.init("lang.text", retrieveLanguageSetting());

        if (args.length == 0) {
            try {
                Platform.startup(() -> {
                });
                Application.launch(SplashScreen.class, args);
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
                        String username =
                            args.length > 2 ? args[2] : System.getProperty("user.name");
                        // sanitize string
                        username = username.replaceAll("[\\s=:,]", "");
                        if (username.isEmpty()) {
                            logger.error("invalid username");
                            System.exit(1);
                        }
                        logger.info("Using username: {}", username);

                        String finalUsername = username;
                        try {
                            Platform.startup(() -> {
                            });
                            SplashScreen.configure(host, port, finalUsername);
                            Application.launch(SplashScreen.class, args);
                        } catch (Exception e) {
                            logger.error("Error launching GUI", e);
                            System.exit(1);
                        }
                    } catch (NumberFormatException e) {
                        logger.error("Invalid port number", e);
                        System.exit(1);
                    } catch (Exception e) {
                        logger.error("Couldn't start client: ", e);
                        System.exit(1);
                    }
                    break;

                default:
                    logger.warn(
                        "Usage: java -jar igoat.jar [server <port> | client <host> <port>]");
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
