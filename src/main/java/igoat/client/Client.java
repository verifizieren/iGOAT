// THIS IS ONLY USED FOR TESTING PURPOSES
package igoat.client;

import java.util.Scanner;

/**
 * Command-line client for the iGoat chat application. Handles server connection, message
 * sending/receiving, and user commands.
 */
public class Client {

    /**
     * Handler for server communications
     */
    static ServerHandler server;

    /**
     * Flag to control the client's running state
     */
    static boolean run = true;

    /**
     * Entry point for the command-line client. Establishes server connection and handles user
     * input.
     *
     * @param args Command line arguments [host, port]
     * @throws InterruptedException If the message handler thread is interrupted
     */
    public static void main(String[] args) throws InterruptedException {
        if (args.length < 2) {
            System.err.println("Usage: client <host> <port>");
            System.exit(1);
        }

        String host = args[0];
        int port;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Error: Invalid port number");
            System.exit(1);
            return;
        }

        String defaultUsername = "user-" + System.currentTimeMillis();
        ServerHandler server = new ServerHandler(host, port, defaultUsername);
        Scanner scanner = new Scanner(System.in);

        Thread messageHandler = new Thread(() -> handleMessages(server));
        messageHandler.start();

        String in;

        while (true) {
            if (!server.isConnected()) {
                System.out.println("Can't connect to server. Reconnect? [y/n]");
                in = scanner.nextLine();
                if (in.equals("y")) {
                    server.reconnect();
                } else {
                    server.close();
                    run = false;
                    break;
                }
            }

            in = scanner.nextLine();

            if (in.equals("exit") || in.equals("logout")) {
                server.sendMessage("ciao");
                server.close();
                run = false;
                break;
            }
            if (in.startsWith("/nick ")) {
                server.sendMessage("username:" + in.substring(6));
            } else if (in.startsWith("/whisper ")) {
                String[] parts = in.substring(9).split(" ", 2);
                if (parts.length == 2) {
                    server.sendMessage("whisper:" + parts[0] + "," + parts[1]);
                }
            } else {
                server.sendMessage("chat:" + in);
            }
        }

        messageHandler.join();
        scanner.close();
    }

    /**
     * Handles incoming messages from the server. Runs in a separate thread to continuously process
     * server messages and updates.
     *
     * @param server The server handler to receive messages from
     */
    public static void handleMessages(ServerHandler server) {
        String msg;
        String update;

        while (run) {
            msg = server.getMessage();
            if (msg != null) {
                System.out.println("Received: " + msg);
            }
            update = server.getLastUpdate();
            if (!update.isEmpty()) {
                System.out.println(update);
            }
        }
    }
}
