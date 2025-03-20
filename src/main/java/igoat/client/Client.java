package igoat.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws InterruptedException {
        ServerHandler server = new ServerHandler();
        Scanner scanner = new Scanner(System.in);

        String in;
        while (true) {
            in = scanner.nextLine();
            if (in.equals("exit")) {
                break;
            }
            server.send(in);
            System.out.println(server.getMessage());
        }
        /*
        // check server connection and retry if not connected
        for (int i = 0; i < 3; i++) {
            if (server.isConnected()) {
                break;
            }
            Thread.sleep(1000); // wait 1s between retries

            if (i < 2) {
                log("Couldn't connect, retrying...");
            }
            else {
                log("Couldn't connect, exiting...");
            }
        }

        System.out.print("Enter your name: ");
        String username = scanner.nextLine().replaceAll("\\s+","");
        // log in with username
        server.send("connect " + username);

        // wait for server answer
        String[] msg = server.receive();

        while (msg.length == 0) {
            Thread.sleep(10);
            msg = server.receive();
        }



        try {
            Socket socket = new Socket("localhost", PORT);
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            Scanner scanner = new Scanner(System.in);
            boolean runloop = true;

            // establish formal connection with server
            while(runloop) {
                // get username & delete spaces


                String[] msg = reader.readLine().split("\\s");

                // we expect 2 "words" from the server
                if (msg.length != 2) {
                    log("Error: Communication failed");
                    continue;
                }

                switch (msg[0]) {
                    case "confirm": // use the username given by the server
                        username = msg[1];
                        runloop = false;
                        break;
                    case "error": // log the error message
                        log(msg[1]);
                        break;
                    default: // if the server breaks protocol
                        log("Error: Communication failed");
                }
            }
            log("Connection established");

            String in;
            runloop = true;
            // "game loop"
            while (runloop) {
                System.out.println("\nPossible commands:\n" +
                        "q - quit, l - join lobby, \\yourChatMessage - global chat");
                in = scanner.nextLine();
                // only handle non-empty inputs
                if (!in.isEmpty()) {
                    switch (in) {
                        case "q":
                            runloop = false;
                            break;
                        case "l":
                            System.out.print("Enter Lobby Code: ");
                            String code = scanner.nextLine();
                            if (joinLobby(code)) {
                                runloop = false;
                            }
                            else {
                                log("Error: failed to join lobby");
                            }
                            break;

                    }
                }



                if (in.equals("bye")) {
                    writer.println("ciao");
                    break;
                }



                out = reader.readLine();
                System.out.println("Received from server: " + out);
            }

            log("exiting program");
            scanner.close();
            socket.close();

        } catch (IOException e) {
            System.err.println(e.toString());
            System.exit(1);
        }*/
    }

    public static void log(String msg) {
        System.out.println("[Client] " + msg);
    }



    public static boolean joinLobby(String code) {
        return true;
    }
}
