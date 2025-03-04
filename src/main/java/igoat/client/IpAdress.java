import java.net.*;
public class IpAddress {
    // Usage: java IpAddress <host>
    public static void main(String[] args) {
        try {
            // Get requested address
            InetAddress addr = InetAddress.getByName(args[0]);
            System.out.println(addr.getHostName());
            System.out.println(addr.getHostAddress());
        } catch (UnknownHostException e) {
            System.err.println(e.toString()); System.exit(1);
        }
    }
}