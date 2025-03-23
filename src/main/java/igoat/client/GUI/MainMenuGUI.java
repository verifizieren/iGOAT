package igoat.client.GUI;

import igoat.client.ServerHandler;
import igoat.server.Server;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Main menu GUI for the iGoat application. Provides options for starting the game, setting
 * username, creating/joining servers, and exiting.
 */
public class MainMenuGUI extends JFrame {

    /**
     * Handler for server communications
     */
    private ServerHandler handler;

    /**
     * Current username for the client
     */
    private String username = "";

    /**
     * Creates and initializes the main menu window. Sets up all UI components and their respective
     * action listeners.
     */
    public MainMenuGUI() {
        setTitle("iGOAT");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 350);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        add(panel);

        // Title
        JLabel titleLabel = new JLabel("iGOAT");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);

        panel.add(Box.createVerticalStrut(20));

        // Start Game Button
        JButton startButton = new JButton("Start");
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JOptionPane.showMessageDialog(null, "Starting game...");
                }
            });
        panel.add(startButton);

        panel.add(Box.createVerticalStrut(15));

        // Username Button
        JButton usernameButton = new JButton("Choose Username");
        usernameButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        usernameButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String input = JOptionPane.showInputDialog(null, "Enter your username:");
                    if (input != null && !input.trim().isEmpty()) {
                        username = input.trim();
                        JOptionPane.showMessageDialog(null, "Username set to: " + username);

                        if (handler != null) {
                            handler.sendMessage("username: " + username);
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "Invalid username.");
                    }
                }
            });
        panel.add(usernameButton);

        panel.add(Box.createVerticalStrut(15));

        // Create Server Button
        JButton createServerButton = new JButton("Create Server");
        createServerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        createServerButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new Thread(() -> Server.startServer(5555)).start();
                }
            });
        panel.add(createServerButton);

        panel.add(Box.createVerticalStrut(15));

        // Join Server Button
        JButton joinServerButton = new JButton("Join Server");
        joinServerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        joinServerButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (username.isEmpty()) {
                        username = getSystemName();
                    }
                    String serverIP = JOptionPane.showInputDialog(null, "Enter server IP:");
                    if (serverIP != null && !serverIP.trim().isEmpty()) {
                        new Thread(
                            () -> {
                                handler = new ServerHandler(serverIP.trim(), 5555);
                                if (handler.isConnected()) {
                                    ChatGUI chatGUI = new ChatGUI(handler, username);
                                    chatGUI.guiSettings();
                                } else {
                                    JOptionPane.showMessageDialog(
                                        null,
                                        "Failed to connect to server at: " + serverIP);
                                }
                            })
                            .start();
                    } else {
                        JOptionPane.showMessageDialog(null, "Invalid server IP.");
                    }
                }
            });
        panel.add(joinServerButton);

        panel.add(Box.createVerticalStrut(15));

        // Exit Button
        JButton exitButton = new JButton("Exit");
        exitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (handler != null) {
                        handler.close();
                        System.exit(0);
                    }
                    System.exit(0);
                }
            });
        panel.add(exitButton);

        setVisible(true);
    }

    /**
     * Retrieves the system's username as a fallback when no username is set.
     *
     * @return The system username from system properties
     */
    public static String getSystemName() {
        return System.getProperty("user.name");
    }

    /**
     * Entry point of the application. Creates and displays the main menu in the Event Dispatch
     * Thread.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainMenuGUI());
    }
}
