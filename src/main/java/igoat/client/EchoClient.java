package igoat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class EchoClient {

  public static void main(String[] args) {
    try {
      Socket socket = new Socket("localhost", 8888);
      OutputStream output = socket.getOutputStream();
      PrintWriter writer = new PrintWriter(output, true);

      InputStream input = socket.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(input));

      Scanner scanner = new Scanner(System.in);
      String in;
      String out;

      while (true) {
        in = scanner.nextLine();

        if (in.equals("bye")) {
          writer.println("ciao");
          break;
        }

        if (!in.isEmpty()) {
          writer.println(in);
        }

        out = reader.readLine();
        System.out.println("Received from server: " + out);
      }

      System.out.println("server exit");
      scanner.close();
      socket.close();

    } catch (IOException e) {
      System.err.println(e.toString());
      System.exit(1);
    }
  }
}
