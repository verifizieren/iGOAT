package igoat.client;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;
import net.java.games.input.Component;
import java.util.HashMap;
import java.util.Map;
import igoat.client.GUI.SettingsWindow;

/**
 * Handles game controller input for the iGoat game.
 * This class manages controller button states and analog stick values,
 * providing a simplified interface for controller input handling.
 * It supports various controller types by checking multiple button identifiers
 * and axis names for compatibility.
 */
public class GameControllerInput {
    private Controller controller;
    private Map<String, Boolean> buttonStates = new HashMap<>();
    private Map<String, Float> analogValues = new HashMap<>();

    private static final String[] INTERACT_BUTTONS = {
        "button 0", "0", "a",           
        "button 2", "2", "x", "3"
    };
    
    private static final String[] RIGHT_STICK_X_IDENTIFIERS = {
        "rx", "x rotation", "z rotation", "z axis", "z"  
    };
    
    private static final String[] RIGHT_STICK_Y_IDENTIFIERS = {
        "ry", "y rotation", "rz", "z rotation", "rz"
    };

    private static final String DPAD_UP = "dpad up";
    private static final String DPAD_DOWN = "dpad down";
    private static final String DPAD_LEFT = "dpad left";
    private static final String DPAD_RIGHT = "dpad right";
    
    private static final String BUTTON_A = "button a";

    static {
        String libraryPath = System.getProperty("java.library.path");
        String jinputPath = System.getProperty("net.java.games.input.librarypath");
        
        System.out.println("JInput Native Library Paths:");
        System.out.println("java.library.path: " + libraryPath);
        System.out.println("jinput.librarypath: " + jinputPath);
    }

    /**
     * Creates a new GameControllerInput instance for the specified controller.
     * 
     * @param controller The controller to use for input. Can be null if no controller is available.
     */
    public GameControllerInput(Controller controller) {
        this.controller = controller;
        if (controller == null) {
            System.out.println("Warning: No controller provided!");
        }
    }

    /**
     * Updates the controller state by polling for new events.
     * This method should be called every frame to maintain current input states.
     * Processes button presses and analog stick movements, updating internal state maps.
     */
    public void update() {
        if (controller != null && controller.poll()) {
            Event event = new Event();
            EventQueue queue = controller.getEventQueue();
            
            while (queue.getNextEvent(event)) {
                Component comp = event.getComponent();
                String compName = comp.getName().toLowerCase();
                String compIdentifier = comp.getIdentifier().toString().toLowerCase();
                float value = event.getValue();
                
                boolean isInteractButton = false;
                for (String buttonPattern : INTERACT_BUTTONS) {
                    if (compName.equals(buttonPattern) || compIdentifier.equals(buttonPattern)) {
                        isInteractButton = true;
                        break;
                    }
                }
                
                if (isInteractButton) {
                    boolean isPressed = value > 0.5f;
                    buttonStates.put("3", isPressed); 
                }
                
                boolean isRightStickX = false;
                boolean isRightStickY = false;
                
                for (String pattern : RIGHT_STICK_X_IDENTIFIERS) {
                    if (compName.equals(pattern) || compIdentifier.equals(pattern)) {
                        isRightStickX = true;
                        break;
                    }
                }
                
                for (String pattern : RIGHT_STICK_Y_IDENTIFIERS) {
                    if (compName.equals(pattern) || compIdentifier.equals(pattern)) {
                        isRightStickY = true;
                        break;
                    }
                }
                
                if (isRightStickX) {
                    analogValues.put("rightX", value);
                }
                else if (isRightStickY) {
                    analogValues.put("rightY", value);
                }
                else if (comp.isAnalog() && !isRightStickX && !isRightStickY) {
                    handleMovementInput(compName, value);
                }
            }
        }
    }

    /**
     * Processes movement input from analog sticks or d-pad.
     * Converts analog values to digital button states for consistent movement handling.
     * 
     * @param compName The name of the input component
     * @param value The input value (-1.0 to 1.0 for analog inputs)
     */
    private void handleMovementInput(String compName, float value) {
        final float THRESHOLD = 0.5f;
        
        if (compName.contains("x") && !compName.contains("rx") && !compName.contains("z")) {
            if (value > THRESHOLD) {
                buttonStates.put(DPAD_RIGHT, true);
                buttonStates.put(DPAD_LEFT, false);
            } else if (value < -THRESHOLD) {
                buttonStates.put(DPAD_LEFT, true);
                buttonStates.put(DPAD_RIGHT, false);
            } else {
                buttonStates.put(DPAD_LEFT, false);
                buttonStates.put(DPAD_RIGHT, false);
            }
        }
        else if (compName.contains("y") && !compName.contains("ry")) {
            if (value > THRESHOLD) {
                buttonStates.put(DPAD_DOWN, true);
                buttonStates.put(DPAD_UP, false);
            } else if (value < -THRESHOLD) {
                buttonStates.put(DPAD_UP, true);
                buttonStates.put(DPAD_DOWN, false);
            } else {
                buttonStates.put(DPAD_UP, false);
                buttonStates.put(DPAD_DOWN, false);
            }
        }
    }

    /**
     * Checks if a specific button is currently pressed.
     * 
     * @param buttonName The name/identifier of the button to check
     * @return true if the button is pressed, false otherwise
     */
    public boolean isButtonPressed(String buttonName) {
        if (buttonName == null) return false;
        return buttonStates.getOrDefault(buttonName.toLowerCase(), false);
    }

    /**
     * Gets the X-axis value of the right analog stick.
     * Includes deadzone handling to prevent drift.
     * 
     * @return The X-axis value (-1.0 to 1.0), or 0.0 if within deadzone
     */
    public float getRightStickX() {
        float value = analogValues.getOrDefault("rightX", 0.0f);
        return Math.abs(value) > 0.15f ? value : 0.0f; 
    }

    /**
     * Gets the Y-axis value of the right analog stick.
     * Includes deadzone handling to prevent drift.
     * 
     * @return The Y-axis value (-1.0 to 1.0), or 0.0 if within deadzone
     */
    public float getRightStickY() {
        float value = analogValues.getOrDefault("rightY", 0.0f);
        return Math.abs(value) > 0.15f ? value : 0.0f; 
    }

    /**
     * Checks if the right analog stick is being actively used.
     * Used to determine if the guard's flashlight/vision circle should be controlled by the stick.
     * 
     * @return true if the right stick is outside the deadzone, false otherwise
     */
    public boolean isUsingFlashlight() {
        float x = getRightStickX();
        float y = getRightStickY();
        return Math.abs(x) > 0.15f || Math.abs(y) > 0.15f; 
    }
} 