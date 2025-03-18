package igoat.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

// Usage: java SocketTest <host>
public class SocketTest {
    public static void main(String[] args) {
        try {
            Socket sock = new Socket(args[0], 9876);
            InputStream in = sock.getInputStream();
            int len;
            byte[] b = new byte[100];

            while ((len = in.read(b)) != -1) {
                System.out.write(b, 0, len);
            }
            in.close();
            sock.close();
        } catch (IOException e) {
            System.err.println(e.toString());
            System.exit(1);
        }
    }
}