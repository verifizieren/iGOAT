package igoat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) {
        final int PORT = 8888;

        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("server laeuft auf Port " + PORT);

            while (true) {
                try  {
                    Socket clientSocket = server.accept();
                    System.out.println("client " + clientSocket);

                    ClientHandler handler = new ClientHandler(clientSocket);
                    new Thread(handler).start();
                } catch (IOException e) {
                        System.out.println("Fehler: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Fehler: " + e.getMessage());
        }
    }
}
