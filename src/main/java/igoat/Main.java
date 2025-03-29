package igoat;

import igoat.client.Client;
import igoat.client.GUI.MainMenuGUI;
import igoat.server.Server;
import javafx.application.Application;

/**
 * Main entry point for the iGoat application. Handles command-line arguments to start either the
 * GUI, server, or client mode.
 */
public class Main {

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
            // No arguments - launch GUI
            Application.launch(MainMenuGUI.class, args);
        } else {
            switch (args[0].toLowerCase()) {
                case "server":
                    if (args.length < 2) {
                        System.err.println("Usage: java -jar igoat.jar server <port>");
                        System.exit(1);
                    }
                    try {
                        int port = Integer.parseInt(args[1]);
                        Server.main(new String[]{String.valueOf(port)});
                    } catch (NumberFormatException e) {
                        System.err.println("Error: Invalid port number");
                        System.exit(1);
                    }
                    break;

                case "client":
                    if (args.length < 3) {
                        System.err.println("Usage: java -jar igoat.jar client <host> <port>");
                        System.exit(1);
                    }
                    try {
                        String host = args[1];
                        int port = Integer.parseInt(args[2]);
                        Client.main(new String[]{host, String.valueOf(port)});
                    } catch (NumberFormatException e) {
                        System.err.println("Error: Invalid port number");
                        System.exit(1);
                    } catch (Exception e) {
                        System.err.println("Error starting client: " + e.getMessage());
                        System.exit(1);
                    }
                    break;

                default:
                    System.err.println("Usage: java -jar igoat.jar [server <port> | client <host> <port>]");
                    System.err.println("       java -jar igoat.jar (for GUI mode)");
                    System.exit(1);
            }
        }
    }
}
