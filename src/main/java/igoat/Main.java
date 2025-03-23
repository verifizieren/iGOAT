package igoat;

import igoat.client.Client;
import igoat.client.GUI.MainMenuGUI;
import igoat.server.Server;

/**
 * Main entry point for the iGoat application.
 * Handles command-line arguments to start either the GUI, server, or client mode.
 */
public class Main {

  /**
   * Main entry point that processes command-line arguments and launches appropriate mode.
   * - No arguments: Launches GUI mode
   * - "server" argument: Starts server mode (requires port)
   * - "client" argument: Starts client mode (requires host, port, username)
   *
   * @param args Command line arguments:
   *             - [] (empty): Launch GUI
   *             - ["server", port]
   *             - ["client", host, port, username]
   */
  public static void main(String[] args) {
    if (args.length == 0) {
      // No arguments - launch GUI
      javax.swing.SwingUtilities.invokeLater(() -> new MainMenuGUI());
    } else {
      // Handle existing client/server logic
      if (args[0].equals("server")) {
        if (args.length < 2) {
          System.out.println("Usage: server <port>");
          return;
        }
        int port = Integer.parseInt(args[1]);
        Server.startServer(port);
      } else if (args[0].equals("client")) {
        if (args.length < 4) {
          System.out.println("Usage: client <host> <port> <username>");
          return;
        }
        String host = args[1];
        int port = Integer.parseInt(args[2]);
        String username = args[3];
        try {
          Client.main(new String[] {host, String.valueOf(port)});
        } catch (InterruptedException e) {
          System.err.println("Client wurde unterbrochen: " + e.getMessage());
        }
      } else {
        System.out.println("Unbekannte Befehle. Benutze 'client' oder 'server'");
      }
    }
  }
}
