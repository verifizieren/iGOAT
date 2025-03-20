package igoat;

import igoat.client.Client;
import igoat.server.Server;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        String mode = args[0].toLowerCase();
        
        try {
            switch (mode) {
                case "server":
                    startServer(args);
                    break;
                case "client":
                    startClient(args);
                    break;
                default:
                    printUsage();
                    System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Fehler: " + e.getMessage());
            printUsage();
            System.exit(1);
        }
    }

    private static void startServer(String[] args) {
        if (args.length < 2) {
            System.err.println("Fehler: Port für Server erforderlich");
            printUsage();
            System.exit(1);
        }

        try {
            int port = Integer.parseInt(args[1]);
            Server.startServer(port);
        } catch (NumberFormatException e) {
            System.err.println("Fehler: Ungültiger Port");
            System.exit(1);
        }
    }

    private static void startClient(String[] args) {
        if (args.length < 2) {
            System.err.println("Fehler: Server adresse für client erforderlich");
            printUsage();
            System.exit(1);
        }

        String[] address = args[1].split(":");
        if (address.length != 2) {
            System.err.println("Fehler: Ungültige server adresse");
            printUsage();
            System.exit(1);
        }

        try {
            String host = address[0];
            int port = Integer.parseInt(address[1]);
            String[] clientArgs = {host, String.valueOf(port)};
            Client.main(clientArgs);
        } catch (NumberFormatException e) {
            System.err.println("Fehler: Ungültiger Port");
            System.exit(1);
        } catch (InterruptedException e) {
            System.err.println("Fehler: Client wurde unterbrochen");
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -jar igoat.jar server <port>");
        System.out.println("  java -jar igoat.jar client <host>:<port>");
        System.out.println();
        System.out.println("Beispiele:");
        System.out.println("  java -jar igoat.jar server 8888");
        System.out.println("  java -jar igoat.jar client localhost:8888");
    }
} 