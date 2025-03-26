package igoat.client.GUI;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import igoat.client.ServerHandler;

/**
 * Graphical user interface for the iGoat chat client. Provides a window-based interface for sending
 * and receiving chat messages, handling whispers, and managing server connections.
 * <p>
 * Features: - Real-time chat message display - Support for whisper commands (/whisper username
 * message) - Automatic server connection management - Message history in scrollable text area
 * <p>
 * Message Format: - Regular chat: chat:message - Whisper: whisper:recipient,message - Server
 * commands: command:parameters
 *
 * @see ServerHandler For the underlying network communication
 * @see ActionListener For handling button events
 */
public class ChatGUI implements ActionListener {

    private static final Logger log = LoggerFactory.getLogger(ChatGUI.class);
    private JTextField field;
    private JButton enter;
    private JTextArea chat;
    private ServerHandler serverHandler;
    private final String username;
    private volatile boolean running = true;

    /**
     * Creates a new ChatGUI instance.
     *
     * @param serverHandler The handler for server communications
     * @param username      The username for this chat client
     */
    public ChatGUI(ServerHandler serverHandler, String username) {
        this.serverHandler = serverHandler;
        this.username = username;
    }

    /**
     * Initializes and configures the GUI components. Sets up the chat window, input field, and
     * message handling.
     */
    public void guiSettings() {
        JLabel label = new JLabel("iGoat");
        label.setFont(new Font("Times", Font.BOLD, 30));

        field = new JTextField();
        field.setPreferredSize(new Dimension(150, 30));

        enter = new JButton("Enter");
        enter.setPreferredSize(new Dimension(100, 30));
        enter.setFont(new Font("Arial", Font.BOLD, 20));
        enter.addActionListener(this);

        chat = new JTextArea(10, 30);
        chat.setFont(new Font("Arial", Font.PLAIN, 12));
        chat.setLineWrap(true);
        chat.setEditable(false);

        JScrollPane scrollbar = new JScrollPane(chat);
        scrollbar.setPreferredSize(new Dimension(350, 200));

        field.addKeyListener(
            new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent enter) {
                    if (enter.getKeyCode() == KeyEvent.VK_ENTER) {
                        sendChat();
                    }
                }
            });

        JPanel panel = new JPanel();
        panel.add(label);
        panel.add(field);
        panel.add(enter);

        JFrame frame = new JFrame("Chat Menu");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(500, 500);
        frame.setLayout(new BorderLayout());
        frame.add(scrollbar, BorderLayout.CENTER);
        frame.add(panel, BorderLayout.NORTH);

        frame.addWindowListener(
            new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    running = false;
                    if (serverHandler != null) {
                        serverHandler.sendMessage("exit");
                        serverHandler.close();
                    }
                }
            });

        frame.setVisible(true);

        if (serverHandler != null && serverHandler.isConnected()) {
            serverHandler.sendMessage("connect:" + username);
        }

        startMessageReceiver();
    }

    /**
     * Starts the message receiving thread. Continuously checks for new messages and processes them
     * based on their type.
     */
    private void startMessageReceiver() {
        while (running && serverHandler != null && serverHandler.isConnected()) {
            String message = serverHandler.getMessage();

            if (message == null || message.isEmpty()) {
                continue;
            }

            int colonIndex = message.indexOf(':');
            if (colonIndex == -1) {
                continue;
            }

            String type = message.substring(0, colonIndex);
            String content = message.substring(colonIndex + 1);

            switch (type) {
                case "chat":
                    int commaIndex = content.indexOf(',');
                    if (commaIndex != -1) {
                        String sender = content.substring(0, commaIndex);
                        String chatMessage = content.substring(commaIndex + 1);
                        appendToChatArea(sender + ": " + chatMessage);
                    } else {
                        appendToChatArea(content);
                    }
                    break;
                case "error":
                    appendToChatArea("Error: " + content);
                    break;
                case "whisper":
                    String[] whisperParts = content.split(",", 2);
                    if (whisperParts.length == 2) {
                        appendToChatArea(
                            "(Whisper from " + whisperParts[0] + "): " + whisperParts[1]);
                    }
                    break;
                case "confirm":
                    if (content.startsWith("Username gesetzt zu ")) {
                        appendToChatArea("Info: " + content);
                    }
                    break;
                case "lobby":
                    if (content.equals("0")) {
                        appendToChatArea("Info: Du hast die Lobby verlassen.");
                    } else {
                        appendToChatArea("Info: Du bist Lobby " + content + " beigetreten.");
                    }
                    break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    /**
     * Appends a message to the chat area in a thread-safe manner.
     *
     * @param message The message to append to the chat area
     */
    private void appendToChatArea(String message) {
        SwingUtilities.invokeLater(
            () -> {
                chat.append(message + "\n");
                chat.setCaretPosition(chat.getDocument().getLength());
            });
    }

    /**
     * Handles button click events.
     *
     * @param e The action event triggered by the user
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == enter) {
            sendChat();
        }
    }

    /**
     * Processes and sends the message from the input field. Handles both regular chat messages and
     * whisper commands.
     */
    private void sendChat() {
        String text = field.getText().trim();
        if (!text.isEmpty()) {
            if (serverHandler != null && serverHandler.isConnected()) {
                if (text.startsWith("/whisper ")) {
                    String[] parts = text.substring(9).split(" ", 2);
                    if (parts.length == 2) {
                        serverHandler.sendMessage("whisper:" + parts[0] + "," + parts[1]);
                    }
                } else {
                    serverHandler.sendMessage("chat:" + text);
                }
            }
            field.setText("");
        }
    }

    /**
     * Main method for testing the GUI independently.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        ChatGUI chatGui = new ChatGUI(null, "TestUser");
        chatGui.guiSettings();
    }
}
