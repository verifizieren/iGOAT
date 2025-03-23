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
 * The ChatGUI class is responsible for creating and managing a graphical user interface for a chat
 * application. It handles user input, displays messages, and communicates with a server through the
 * provided ServerHandler instance.
 *
 * <p>This class implements the ActionListener interface to respond to user actions, such as
 * clicking the send button or pressing Enter.
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
   * Constructs an instance of the ChatGUI.
   *
   * @param serverHandler An instance of the ServerHandler used for communication with the server.
   * @param username The username of the user interacting with the chat GUI.
   */
  public ChatGUI(ServerHandler serverHandler, String username) {
    this.serverHandler = serverHandler;
    this.username = username;
  }

  /**
   * Configures and initializes the graphical user interface (GUI) for the chat application.
   *
   * <p>This method sets up various components of a basic chat GUI.
   *
   * <p>Additionally, it sets up event listeners: - The Enter key triggers the sending of messages
   * via the connected server handler. - Window closing triggers cleanup operations such as
   * disconnecting from the server and stopping any background threads.
   *
   * <p>A new application frame is instantiated and configured with a layout that includes the above
   * components, and it is then displayed. If a server connection exists, the method sends a status
   * message indicating the username's connection to the server.
   *
   * <p>Finally, the method starts a background thread to handle incoming messages from the server.
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
              sendMessage();
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
              serverHandler.sendMessage("ciao");
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
   * Starts a background thread responsible for receiving and processing messages from the server.
   * This method continuously listens for messages while the chat application is running and the
   * server connection is active.
   *
   * <p>The received messages are processed based on their type (e.g., "chat", "error", "info",
   * "whisper", "confirm") and handled accordingly by updating the chat GUI.
   *
   * <p>- For "chat" messages, the sender and content are extracted and appended to the chat area. -
   * For "whisper", a private message is displayed as a whisper in the chat area. - For "error" and
   * "info", relevant details are shown to the user. - For "confirm", specific confirmation messages
   * (e.g., username updates) are processed.
   *
   * <p>If a message is improperly formatted or empty, it is ignored. The thread sleeps briefly
   * between polling operations to prevent excessive resource usage.
   *
   * <p>In case of an interruption (e.g., stopping the application), the thread terminates
   * gracefully.
   *
   * <p>Preconditions: - The `serverHandler` instance must be properly initialized and connected to
   * the server. - The `running` flag should be true to start the thread.
   *
   * <p>Postconditions: - A thread for message reception is started and continuously processes new
   * messages until the chat application stops or the server disconnects.
   */
  private void startMessageReceiver() {
    while (running && serverHandler != null && serverHandler.isConnected()) {
      String message = serverHandler.getMessage();

      if (message == null || message.isEmpty()) {
        continue;
      }

      String[] parts = message.split(":");

      if (parts.length != 2) {
        continue;
      }

      String type = parts[0];
      String content = parts[1];

      switch (type) {
        case "chat":
          String[] chatParts = content.split(",", 2);
          if (chatParts.length == 2) {
            appendToChatArea(chatParts[0] + ": " + chatParts[1]);
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
            appendToChatArea("(Whisper from " + whisperParts[0] + "): " + whisperParts[1]);
          }
          break;
        case "confirm":
          if (content.startsWith("Username gesetzt zu ")) {
            appendToChatArea("Info: " + content);
          }
          break;
        case "lobby":
          // todo: implement lobby
          System.out.println("joined lobby " + content);
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        System.out.println(e.getMessage());
      }
    }
  }

  /**
   * Appends a given message to the chat area in the graphical user interface. This method ensures
   * that the appended message is displayed at the bottom of the chat area by scrolling to the most
   * recent entry.
   *
   * @param message The text message to append to the chat area. It represents the content to be
   *     displayed in the chat.
   */
  private void appendToChatArea(String message) {
    SwingUtilities.invokeLater(
        () -> {
          chat.append(message + "\n");
          chat.setCaretPosition(chat.getDocument().getLength());
        });
  }

  /**
   * Handles the action event triggered when an interactive component, such as a button, is
   * activated. This method checks the source of the event and performs the appropriate action.
   * Specifically, if the source of the event is the "enter" button, it invokes the `sendMessage`
   * method to process and send the user's input.
   *
   * @param e The ActionEvent object containing details about the event that occurred.
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == enter) {
      sendMessage();
    }
  }

  /**
   * Sends a message from the user input field to the server.
   *
   * <p>This method processes the text entered in the input field (`field`) and sends it to the
   * server through the `serverHandler` if a connection is active. The message can either be a
   * regular chat message or a private message (whisper).
   *
   * <p>Functionality: 1. Retrieves the text from the input field and trims any leading or trailing
   * spaces. 2. If the trimmed text is not empty and the `serverHandler` is connected, it proceeds
   * to process the text. 3. If the text starts with "/whisper ": a. Attempts to split the remainder
   * into a recipient and a private message. b. Sends a formatted private message
   * ("whisper:<recipient>,<message>") to the server. 4. Otherwise, sends the text as a regular chat
   * message ("chat:<message>"). 5. Clears the input field after processing.
   *
   * <p>Preconditions: - `serverHandler` must be initialized and connected for messages to be sent.
   * - Assumes the input field (`field`) contains user-entered text.
   *
   * <p>Postconditions: - If the input text is valid and the server connection is active, a message
   * is sent to the server. - The input field is cleared regardless of whether a message is sent.
   *
   * <p>Notes: - Private messages must follow the specific format "/whisper <recipient> <message>".
   * Messages failing to conform to this format are not sent. - No action is taken if the server is
   * disconnected or the input is empty.
   */
  private void sendMessage() {
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

  public static void main(String[] args) {
    ChatGUI chatGui = new ChatGUI(null, "TestUser");
    chatGui.guiSettings();
  }
}
