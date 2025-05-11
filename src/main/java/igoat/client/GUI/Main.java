/**
 * Main entry point for the iGoat GUI client. Starts the main menu GUI.
 *
 * @author Marvin, Max, Nicolas, and Jonas
 */
package igoat.client.GUI;

import javafx.application.Application;

/**
 * Class for launching the client GUI application
 */
public class Main {

    /**
     * Starts the main menu GUI.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        Application.launch(MainMenuGUI.class, args);
    }
}
