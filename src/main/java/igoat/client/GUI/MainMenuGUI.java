package igoat.client.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainMenuGUI extends JFrame {

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
                String username = JOptionPane.showInputDialog(null, "Enter your username:");
                JOptionPane.showMessageDialog(null, "Username set to: " + username);
            }
        });
        panel.add(usernameButton);

        panel.add(Box.createVerticalStrut(15));

        JButton createServerButton = new JButton("Create Server");
        createServerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        createServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Server.startServer(5555);
                    }
                }).start();
            }
        });
        panel.add(createServerButton);

        panel.add(Box.createVerticalStrut(15));

        JButton joinServerButton = new JButton("Join Server");
        joinServerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        joinServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String serverIP = JOptionPane.showInputDialog(null, "Enter server IP:");
                JOptionPane.showMessageDialog(null, "Joining server at: " + serverIP);
            }
        });
        panel.add(joinServerButton);

        panel.add(Box.createVerticalStrut(15));

        JButton exitButton = new JButton("Exit");
        exitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                System.exit(0);
            }
        });
        panel.add(exitButton);

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MainMenuGUI();
            }
        });
    }
}
