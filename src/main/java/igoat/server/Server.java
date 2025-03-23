package igoat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Server implementation for the iGoat chat application. Handles client connections and spawns
 * individual handlers for each client.
 */
public class Server {

    /**
     * Flag to control the server's running state
     */
    private static volatile boolean running = true;

    /**
     * Starts the server on the specified port. Creates a new thread for each connecting client.
     *
     * @param port The port number to listen on
     */
    public static void startServer(int port) {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Server läuft auf Port " + port);

            while (running) {
                try {
                    Socket clientSocket = server.accept();
                    System.out.println(
                        "Neuer Client verbunden: " + clientSocket.getInetAddress()
                            .getHostAddress());

                    // Each client is handled in its own thread
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

    /**
     * Stops the server gracefully. Sets the running flag to false, which will terminate the main
     * server loop.
     */
    public static void stopServer() {
        running = false;
    }
}
