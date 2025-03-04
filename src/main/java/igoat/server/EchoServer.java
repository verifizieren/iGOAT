package igoat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoServer {
    public static void main(String[] args) {
        final int PORT = 9876;

        try {
            try (ServerSocket server = new ServerSocket(PORT)) {
                System.out.println("server laeuf auf Port " + PORT);
                
                while (true) {
                    try (Socket client = server.accept()) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                        PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
                        
                        String message = reader.readLine();
                        if (message.equals("ciao")) { // Auf linux mit "echo "nachricht" | nc localhost 9876" aufrufen
                            System.out.println("server beendet ciao 👋");
                            break;
                        }
                        System.out.println("nachricht erhalten: " + message);
                        writer.println(message);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Fehler: " + e.getMessage());
        }
    }
}