package igoat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server implementation for the iGoat game application.
 * <p>
 * This server: - Accepts TCP connections from game clients - Creates individual ClientHandler
 * threads for each connected client - Manages UDP communication for real-time game state updates -
 * Supports dynamic port configuration with a default fallback
 * </p>
 */
public class Server {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    /**
     * Default port number if none is specified via command line
     */
    private static final int DEFAULT_PORT = 61000;

    /**
     * Entry point for the server application. Starts the server on either a specified port or the
     * default port.
     *
     * @param args Command line arguments. If args[0] exists, it's used as the port number,
     *             otherwise DEFAULT_PORT is used
     */
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;

        startServer(port);
    }

    /**
     * Starts the game server on the specified port.
     * <p>
     * This method: - Creates a TCP server socket - Initializes the UDP listener for game state
     * updates - Accepts incoming client connections - Creates a new thread for each client
     * </p>
     *
     * @param port The port number to listen on
     */
    public static void startServer(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Server started on port {}", port);

            ClientHandler.startUdpListener();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("New connection from {}",
                    clientSocket.getInetAddress().getHostAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                Thread client = new Thread(handler);
                client.setDaemon(true);
                client.start();
            }
        } catch (IOException e) {
            logger.error("Could not listen on port {}", port, e);
        } finally {
            ClientHandler.stopUdpListener();
        }
    }
}
