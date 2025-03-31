package igoat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Server implementation for the iGoat chat application. Handles client connections and spawns
 * individual handlers for each client.
 */
public class Server {

    private static final int DEFAULT_PORT = 61000;

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            
            ClientHandler.startUdpListener();
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection from " + clientSocket.getInetAddress().getHostAddress());
                
                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + port);
            System.err.println(e.getMessage());
        } finally {
            ClientHandler.stopUdpListener();
        }
    }

    public static void startServer(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);

            ClientHandler.startUdpListener();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection from " + clientSocket.getInetAddress().getHostAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + port);
            System.err.println(e.getMessage());
        } finally {
            ClientHandler.stopUdpListener();
        }
    }


}
