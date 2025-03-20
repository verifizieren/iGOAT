package igoat.client;

import java.util.Scanner;

public class Client {
    static ServerHandler server;
    static boolean run = true;

    public static void main(String[] args) throws InterruptedException {
        server = new ServerHandler();
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
    }

    public static void handleMessages(ServerHandler server) {
        String msg;

        while (run) {
            msg = server.getMessage();
            if (msg != null) {
                System.out.println("Received: " + msg);
            }
        }
    }

    public static void log(String msg) {
        System.out.println("[Client] " + msg);
    }
}