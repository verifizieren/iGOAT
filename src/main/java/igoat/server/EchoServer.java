// THIS IS ONLY USED FOR TESTING PURPOSES

package igoat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoServer {

  public static boolean run = true;
  static long lastMSG;

  public static void main(String[] args) {
    final int PORT = 8888;

    try (ServerSocket server = new ServerSocket(PORT)) {
      System.out.println("server laeuf auf Port " + PORT);
      Socket client = server.accept();

      Thread ping = new Thread(() -> pingPong(client));
      ping.start();

      lastMSG = System.currentTimeMillis();

      while (run) {
        try {
          BufferedReader reader =
              new BufferedReader(new InputStreamReader(client.getInputStream()));
          PrintWriter writer = new PrintWriter(client.getOutputStream(), true);

          String message = reader.readLine();
          lastMSG = System.currentTimeMillis();
          if (message.equals(
              "ciao")) { // Auf linux mit "echo "nachricht" | nc localhost 9876" aufrufen
            System.out.println("server beendet ciao 👋");
            run = false;
          }
          System.out.println("nachricht erhalten: " + message);
          if (!message.equals("pong")) {
            writer.println(message);
            System.out.println("sent: " + message);
          }
        } catch (IOException e) {
          System.out.println("Fehler: " + e.getMessage());
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void pingPong(Socket client) {
    long timer = System.currentTimeMillis();
    System.out.println("Ping pong start");

    while (run) {
      if (System.currentTimeMillis() - timer > 3000) {
        timer = System.currentTimeMillis();
        try {
          PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
          writer.println("ping");
          System.out.println("ping sent");
        } catch (IOException e) {
          System.out.println("Fehler: " + e.getMessage());
        }
      }
      if (System.currentTimeMillis() - lastMSG > 7000) {
        run = false;
      }
    }
  }
}
