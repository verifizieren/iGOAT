package igoat.client;

import javafx.scene.shape.Rectangle;

public class DoorHandler {
    private int activatedTerminals = 0;
    private boolean doorOpen = false;
    private Map gameMap;

    public DoorHandler(Map map) {
        this.gameMap = map;
    }

    public void activateTerminal(Rectangle terminal) {
        if (!doorOpen && gameMap.getTerminalList().contains(terminal) && terminal.getFill() == javafx.scene.paint.Color.RED) {
            terminal.setFill(javafx.scene.paint.Color.YELLOW);
            activatedTerminals++;
            checkDoorCondition();
        }
    }

    private void checkDoorCondition() {
        if (activatedTerminals >= 3) {
            openDoor();
        }
    }

    private void openDoor() {
        doorOpen = true;
        gameMap.removeDoor();
    }
}


