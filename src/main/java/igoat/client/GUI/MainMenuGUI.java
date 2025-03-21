package igoat.client.GUI;

import igoat.client.ServerHandler;
import igoat.server.Server;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainMenuGUI extends JFrame {
    private ServerHandler handler;

    // Store the chosen username (default empty)
    private String username = "";

    public MainMenuGUI() {
        setTitle("IGOAT");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 350);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        add(panel);

        JLabel titleLabel = new JLabel("IGOAT");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);

        panel.add(Box.createVerticalStrut(20));

        JButton startButton = new JButton("Start");
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, "Starting game...");
            }
        });
        panel.add(startButton);

        panel.add(Box.createVerticalStrut(15));

        JButton usernameButton = new JButton("Choose Username");
        usernameButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        usernameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Prompt the user for a username and store it
                String input = JOptionPane.showInputDialog(null, "Enter your username:");
                if (input != null && !input.trim().isEmpty()) {
                    username = input.trim();
                    JOptionPane.showMessageDialog(null, "Username set to: " + username);
                } else {
                    JOptionPane.showMessageDialog(null, "Invalid username.");
                }
            }
        });
        panel.add(usernameButton);

        panel.add(Box.createVerticalStrut(15));

        JButton createServerButton = new JButton("Create Server");
        createServerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        createServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Start the server on port 5555
                new Thread(() -> Server.startServer(5555)).start();
            }
        });
        panel.add(createServerButton);

        panel.add(Box.createVerticalStrut(15));

        JButton joinServerButton = new JButton("Join Server");
        joinServerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        joinServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Check that the user has chosen a username first
                if(username.isEmpty()){
                    JOptionPane.showMessageDialog(null, "Please choose a username first.");
                    return;
                }
                // Prompt for the server IP
                String serverIP = JOptionPane.showInputDialog(null, "Enter server IP:");
                if(serverIP != null && !serverIP.trim().isEmpty()){
                    new Thread(() -> {
                        // Create a new ServerHandler to connect to the server at the given IP and port 5555
                        handler = new ServerHandler(serverIP.trim(), 5555);
                        if(handler.isConnected()){
                            // Launch the ChatGUI and pass the connection and username
                            ChatGUI chatGUI =   new ChatGUI(handler, username);
                            chatGUI.guiSettings();
                        } else {
                            JOptionPane.showMessageDialog(null, "Failed to connect to server at: " + serverIP);
                        }
                    }).start();
                } else {
                    JOptionPane.showMessageDialog(null, "Invalid server IP.");
                }
            }
        });
        panel.add(joinServerButton);

        panel.add(Box.createVerticalStrut(15));

        JButton exitButton = new JButton("Exit");
        exitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handler.close();
                System.exit(0);

            }
        });
        panel.add(exitButton);

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainMenuGUI());
    }
}
