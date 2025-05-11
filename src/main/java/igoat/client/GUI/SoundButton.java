package igoat.client.GUI;

import igoat.client.SoundManager;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.input.MouseEvent;

/**
 * A JavaFX button that plays a sound when pressed
 */
public class SoundButton extends Button {
    private static final SoundManager sound = SoundManager.getInstance();

    public SoundButton(String text) {
        super(text);
        this.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> { sound.click.play();});
    }

    /**
     * Adds sound to button elements in a dialog
     */
    public static void addDialogSound(Dialog<?> dialog) {
        dialog.setOnShown(e -> {
            DialogPane pane = dialog.getDialogPane();
            for (ButtonType buttonType : pane.getButtonTypes()) {
                Node buttonNode = pane.lookupButton(buttonType);
                if (buttonNode != null) {
                    buttonNode.addEventHandler(MouseEvent.MOUSE_PRESSED, ev -> sound.click.play());
                }
            }
        });
    }
}
