package igoat.client;

public class DoorHandler {
    private int activatedTerminals = 0;
    private boolean doorOpen = false;
    private Map gameMap;

    public DoorHandler(Map map) {
        this.gameMap = map;
    }

    public void activateTerminal() {
        activatedTerminals++;
        checkDoorCondition();
    }

    private void checkDoorCondition() {
        if (activatedTerminals == 3 && !doorOpen) {
            openDoor();
        }
    }

    private void openDoor() {
        doorOpen = true;
        gameMap.removeDoor();
    }
}


