package igoat.client;

import java.util.Scanner;

public class GamepadInputTest {

    public static void main(String[] args) {
        // Step 1: List all connected HID devices
        System.out.println("Enumerating HID devices...");
        GamepadInput.enumerateDevices();
        System.out.println("\nEdit this file to set the correct vendorId and productId for your gamepad.");

        // Step 2: Set your gamepad's vendorId and productId here
        int vendorId = 0x045e; // Example: Microsoft (change to your controller)
        int productId = 0x028e; // Example: Xbox 360 Controller (change to your controller)

        // Optionally, prompt user for IDs
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter vendorId (hex, e.g. 045e): ");
        vendorId = Integer.parseInt(scanner.nextLine(), 16);
        System.out.print("Enter productId (hex, e.g. 028e): ");
        productId = Integer.parseInt(scanner.nextLine(), 16);

        GamepadInput gamepad = new GamepadInput(vendorId, productId);


        if (!gamepad.isOpen()) {
            System.err.println("Failed to open gamepad device. Please check the vendorId/productId and device connection.");
            return;
        }

        System.out.println("\nReading gamepad input. Press Ctrl+C to exit.");
        while (true) {
            java.util.Map<String, Object> state = gamepad.getFullState();
            for (java.util.Map.Entry<String, Object> entry : state.entrySet()) {
                System.out.print(entry.getKey() + ": " + entry.getValue() + " | ");
            }
            System.out.println();  // New line for readability
            gamepad.printLastReport();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
        // Clean up
        gamepad.close();
    }
} 