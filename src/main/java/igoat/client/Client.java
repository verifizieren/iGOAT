package igoat.client;

import java.util.Scanner;

public class Client {
    static ServerHandler server;
    static boolean run = true;

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

        server = new ServerHandler(host, port);
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

            if (in.equals("exit")) {
                server.send("ciao");
                server.close();
                run = false;
                break;
            }

            server.send(in);
        }

        messageHandler.join();
        scanner.close();
    }

    public static void handleMessages(ServerHandler server) {
        String msg;

        while (run) {
            msg = server.getMessage();
            if (msg != null) {
                // Automatische Antwort auf Ping
                if (msg.equals("ping")) {
                    server.send("pong");
                    continue;
                }
                System.out.println("Received: " + msg);
            }
        }
    }

    public static void log(String msg) {
        System.out.println("[Client] " + msg);
    }
}