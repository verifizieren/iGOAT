package igoat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;

    private static List<ClientHandler> clientList = new CopyOnWriteArrayList<>();

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        clientList.add(this);
    }

    @Override
    public void run() {

        try{
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            String message;
            while((message = in.readLine()) != null) {
                System.out.println(message);
                broadcast(message);
            }
        }catch(IOException e){
            System.err.println("Problem: " + e.getMessage());
        }finally {
            try{
                if(in != null) in.close();
                if(out != null) out.close();
                if(clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
                System.out.println("client closed");
            }catch(IOException e){
                System.err.println("Problem: " + e.getMessage());
            }
        }
    }

    private void broadcast(String message) {
        for (ClientHandler client : clientList) {
            if(client != this) client.out.println(message);
        }
    }

}

