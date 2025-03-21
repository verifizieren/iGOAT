package igoat.client.GUI;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JScrollPane;

public class ChatGUI implements ActionListener {
    private JTextField field;
    private JButton enter;
    private JTextArea chat;

    public void guiSettings() {
        JLabel label = new JLabel("iGoat");
        label.setFont(new Font("Nordique", Font.BOLD, 30));

        field = new JTextField();
        field.setPreferredSize(new Dimension(150, 30));

        enter = new JButton("Enter");
        enter.setPreferredSize(new Dimension(100, 30));
        enter.setFont(new Font("Arial", Font.BOLD, 20));
        enter.addActionListener(this);

        chat = new JTextArea(10, 30);
        chat.setFont(new Font("Arial", Font.PLAIN, 12));
        chat.setLineWrap(true);

        JScrollPane scrollchat = new JScrollPane(chat);
        scrollchat.setPreferredSize(new Dimension(350, 200));

        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent enter) {
                if (enter.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });

        JPanel panel = new JPanel();
        panel.add(label);
        panel.add(field);
        panel.add(enter);

        JFrame frame = new JFrame("Chat Menu");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 500);
        frame.setLayout(new BorderLayout());
        frame.add(scrollchat, BorderLayout.CENTER);
        frame.add(panel, BorderLayout.NORTH);
        frame.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == enter) {
            sendMessage();

        }
    }
    private void sendMessage(){
        String text = field.getText().trim();
        if (!text.isEmpty()) {
            chat.append("User: " + text + "\n");
            field.setText("");
        }
    }

    public static void main(String[] args) {
        ChatGUI chatGui = new ChatGUI();
        chatGui.guiSettings();
    }
}
