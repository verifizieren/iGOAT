package igoat.client;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket client;

    public ClientHandler(Socket client) {
        this.client = client;

        try{
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            String message;
            while(message = in.readLine() != null){
                System.out.println(message);
                out.println(message);
            }
        }catch(IOException e){
            System.err.println("Problem: " + e.getMessage());
        }

    }

}

