package igoat.client.GUI;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class JavaFXChatGUI extends Application {
    private TextField field;
    private TextArea chat;

    @Override
    public void start(Stage primaryStage) {
        Label label = new Label("iGoat");
        label.setFont(new Font("Arial", 30));

        field = new TextField();
        field.setPrefSize(200, 30);
        field.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                sendMessage();
            }
        });

        Button enter = new Button("Enter");
        enter.setFont(new Font("Arial", 14));
        enter.setOnAction(e -> sendMessage());

        chat = new TextArea();
        chat.setFont(new Font("Arial", 12));
        chat.setWrapText(true);
        chat.setEditable(false);

        ScrollPane scrollchat = new ScrollPane(chat);
        scrollchat.setFitToWidth(true);
        scrollchat.setPrefHeight(300);

        HBox inputBox = new HBox(10, label, field, enter);
        inputBox.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(inputBox);
        root.setCenter(scrollchat);

        Scene scene = new Scene(root, 500, 400);
        primaryStage.setTitle("Chat Menu");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void sendMessage() {
        String text = field.getText().trim();
        if (!text.isEmpty()) {
            chat.appendText("User: " + text + "\n");
            field.clear();
        }
    }
}


