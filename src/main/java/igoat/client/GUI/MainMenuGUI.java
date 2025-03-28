package igoat.client.GUI;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.embed.swing.SwingNode;

public class MainMenuGUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Placeholder for the game name
        Text gameName = new Text("iGOAT");
        gameName.setStyle("-fx-font-size: 30px; -fx-font-weight: bold;");

        // Create buttons
        Button startButton = new Button("Start");
        Button settingsButton = new Button("Settings");
        Button exitButton = new Button("Exit");

        // Set button actions (for demonstration purposes)
        startButton.setOnAction(event -> System.out.println("Start button clicked"));
        settingsButton.setOnAction(event -> System.out.println("Settings button clicked"));
        exitButton.setOnAction(event -> primaryStage.close()); // Close the application

        // Create a VBox to arrange elements vertically
        VBox menuLayout = new VBox(20); // Spacing between elements
        menuLayout.setAlignment(Pos.CENTER);
        menuLayout.setPadding(new Insets(50)); // Padding around the menu

        // Add elements to the layout
        menuLayout.getChildren().addAll(gameName, startButton, settingsButton, exitButton);

        // Create the scene
        Scene scene = new Scene(menuLayout, 400, 300); // Width and height of the window

        // Set the title of the stage
        primaryStage.setTitle("Game Menu");

        // Set the scene and show the stage
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}