package igoat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private static volatile boolean running = true;
    
    public static void startServer(int port) {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Server läuft auf Port " + port);

            while (running) {
                try {
                    Socket clientSocket = server.accept();
                    System.out.println("Neuer Client verbunden: " + clientSocket.getInetAddress().getHostAddress());

                    ClientHandler handler = new ClientHandler(clientSocket);
                    new Thread(handler).start();
                } catch (IOException e) {
                    if (running) {
                        System.out.println("Fehler: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("ServerFehler: " + e.getMessage());
            System.exit(1);
        }
    }
    
    public static void stopServer() {
        running = false;
    }
}
