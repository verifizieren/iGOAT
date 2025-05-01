package igoat.client.GUI;

import igoat.client.SoundManager;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;

/**
 * A JavaFX button that plays a sound when pressed
 */
public class SoundButton extends Button {
    private static final SoundManager sound = SoundManager.getInstance();

    public SoundButton(String text) {
        super(text);
        this.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {sound.click.play();});
    }
}
