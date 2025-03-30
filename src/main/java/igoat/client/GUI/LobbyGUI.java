package igoat.client.GUI;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class LobbyGUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        Label label = new Label("Lobby");
        label.setFont(new Font("Arial", 30));

        Button joinButton = new Button("Join");
        Button createButton = new Button("Create Lobby");
        Button exitButton = new Button("Exit");
        Button nameButton = new Button("Enter Name");

        joinButton.setOnAction(event -> System.out.println("Join button clicked"));
        createButton.setOnAction(event -> System.out.println("Create Lobby button clicked"));
        nameButton.setOnAction(event -> System.out.println("Enter name button clicked"));
        exitButton.setOnAction(event -> primaryStage.close());

        VBox lobbyLayout = new VBox(20);
        lobbyLayout.setAlignment(Pos.CENTER);
        lobbyLayout.setPadding(new Insets(50));

        lobbyLayout.getChildren().addAll(label, joinButton, createButton, nameButton, exitButton);

        Scene scene = new Scene(lobbyLayout, 400, 300);

        primaryStage.setTitle("Lobby Menu");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

}

