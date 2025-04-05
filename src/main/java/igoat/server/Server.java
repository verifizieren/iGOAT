package igoat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server implementation for the iGoat chat application. Handles client connections and spawns
 * individual handlers for each client.
 */
public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private static final int DEFAULT_PORT = 61000;

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        
        startServer(port);
    }

    public static void startServer(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Server started on port {}", port);

            ClientHandler.startUdpListener();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("New connection from {}", clientSocket.getInetAddress().getHostAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                Thread client = new Thread(handler);
                client.setDaemon(true);
                client.start();
            }
        } catch (IOException e) {
            logger.error("Could not listen on port {}",port, e);
        } finally {
            ClientHandler.stopUdpListener();
        }
    }
}
