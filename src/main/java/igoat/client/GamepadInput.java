package igoat.client;

import org.hid4java.*;
import org.hid4java.event.HidServicesEvent;

import java.util.Arrays;

public class GamepadInput implements HidServicesListener {
    private HidDevice gamepad;
    private volatile byte[] lastReport = new byte[0];
    private Thread pollThread;
    private boolean running = false;

    /**
     * Enumerate all attached HID devices and print their vendor/product IDs and product names.
     */
    public static void enumerateDevices() {
        HidServices hidServices = HidManager.getHidServices();
        for (HidDevice device : hidServices.getAttachedHidDevices()) {
            System.out.printf("VID: %04x PID: %04x %s\n", device.getVendorId(), device.getProductId(), device.getProduct());
        }
    }

    /**
     * Open a gamepad by vendorId and productId.
     * @param vendorId USB vendor ID
     * @param productId USB product ID
     */
    public GamepadInput(int vendorId, int productId) {
        HidServicesSpecification spec = new HidServicesSpecification();
        spec.setAutoStart(false);
        HidServices hidServices = HidManager.getHidServices(spec);
        hidServices.addHidServicesListener(this);
        hidServices.start();

        for (HidDevice device : hidServices.getAttachedHidDevices()) {
            if (device.getVendorId() == vendorId && device.getProductId() == productId) {
                gamepad = device;
                break;
            }
        }

        if (gamepad != null) {
            try {
                gamepad.open();
            } catch (Exception e) {
                System.err.println("Failed to open device: " + e.getMessage());
                return;
            }
        }

        if (isOpen()) {
            running = true;
            pollThread = new Thread(() -> {
                while (running) {
                    try {
                        Byte[] dataBoxed = gamepad.read(64, 100);
                        if (dataBoxed != null && dataBoxed.length > 0) {
                            byte[] data = new byte[dataBoxed.length];
                            for (int i = 0; i < dataBoxed.length; i++) {
                                data[i] = dataBoxed[i];
                            }
                            lastReport = Arrays.copyOf(data, data.length);
                        }
                    } catch (IllegalStateException e) {
                        System.err.println("Device not open: " + e.getMessage());
                        running = false;
                    }
                    try { Thread.sleep(10); } catch (InterruptedException ignored) {}
                }
            });
            pollThread.setDaemon(true);
            pollThread.start();
        }
    }

    /**
     * Stop polling and release resources.
     */
    public void close() {
        running = false;
        if (pollThread != null) {
            try { pollThread.join(200); } catch (InterruptedException ignored) {}
        }
        if (gamepad != null) {
            gamepad.close();
        }
    }

    /**
    Get X axis
     */
    public int getXAxis() {
        if (lastReport.length < 2) return 0;  // Return 0 if data is insufficient
        int rawX = Byte.toUnsignedInt(lastReport[1]);
        return (rawX - 128) / 128;  // Normalize to -1 to 1
    }

    /**
     Get Y axis
     */
    public int getYAxis() {
        if (lastReport.length < 3) return 0;
        int rawY = Byte.toUnsignedInt(lastReport[2]);
        return (rawY - 128) / 128;  // Normalize to -1 to 1, invert if needed
    }

    /**
     * Print the latest raw report (for debugging/mapping).
     */
    public void printLastReport() {
        System.out.println(Arrays.toString(lastReport));
    }

    public boolean isOpen() {
        return gamepad != null && gamepad.isOpen();
    }

    // Updated methods for Xbox One controller
    public boolean isButtonPressed(int buttonBit) {
        if (lastReport.length < 15) return false;
        int byteIndex = 14;  // Buttons at index 14
        return (lastReport[byteIndex] & (1 << buttonBit)) != 0;  // Bitwise check for the specified bit
    }

    // Updated axis methods
    public int getLeftXAxis() { return (lastReport.length > 1) ? Byte.toUnsignedInt(lastReport[1]) : 0; }  // Index 1
    public int getLeftYAxis() { return (lastReport.length > 2) ? Byte.toUnsignedInt(lastReport[2]) : 0; }  // Index 2
    public int getRightXAxis() { return (lastReport.length > 5) ? Byte.toUnsignedInt(lastReport[5]) : 0; }  // Index 5
    public int getRightYAxis() { return (lastReport.length > 6) ? Byte.toUnsignedInt(lastReport[6]) : 0; }  // Index 6

    // Updated trigger methods
    public int getLeftTrigger() { return (lastReport.length > 9) ? Byte.toUnsignedInt(lastReport[9]) : 0; }  // Index 9 for accurate value
    public int getRightTrigger() { return (lastReport.length > 11) ? Byte.toUnsignedInt(lastReport[11]) : 0; }  // Index 11 for accurate value

    // New method for DPAD (assuming a byte with directional bits, e.g., bitmask)
    public int getDPAD() { return (lastReport.length > 13) ? Byte.toUnsignedInt(lastReport[13]) : 0; }  // Index 13

    // New DPAD direction methods based on user's values
    public boolean isDPADUp() { int value = getDPAD(); return value == 2 || value == 1 || value == 8; }  // Values 2, 1, or 8 for Up
    public boolean isDPADDown() { return getDPAD() == 6; }  // Value 6 for Down
    public boolean isDPADLeft() { return getDPAD() == 8; }  // Value 8 for Left
    public boolean isDPADRight() { return getDPAD() == 4; }  // Value 4 for Right

    // New methods for stick clicks (assuming bits in index 15)
    public boolean isLeftStickClickPressed() { return (lastReport.length > 15) && ((lastReport[15] & 0x20) != 0); }  // Bit 5 for value 32
    public boolean isRightStickClickPressed() { return (lastReport.length > 15) && ((lastReport[15] & 0x40) != 0); }  // Bit 6 for value 64

    // Updated specific button methods with accurate bits
    public boolean isAButtonPressed() { return isButtonPressed(0); }  // Bit 0 for A (value 1)
    public boolean isBButtonPressed() { return isButtonPressed(1); }  // Bit 1 for B (value 2)
    public boolean isXButtonPressed() { return isButtonPressed(3); }  // Bit 3 for X (value 8)
    public boolean isYButtonPressed() { return isButtonPressed(4); }  // Bit 4 for Y (value 16)
    public boolean isLeftUpperTriggerPressed() { return isButtonPressed(6); }  // Bit 6 for left upper (value 64)
    public boolean isRightUpperTriggerPressed() { return isButtonPressed(7); }  // Bit 7 for right upper (value -128 or 128 unsigned)

    // Specific button methods with index 14
    public boolean isBackButtonPressed() { return isButtonPressed(8); }  // Bit 8 for Back
    public boolean isStartButtonPressed() { return isButtonPressed(9); }  // Bit 9 for Start

    // --- HidServicesListener methods (required for hid4java 0.8.0) ---
    @Override public void hidDeviceAttached(HidServicesEvent event) {}
    @Override public void hidDeviceDetached(HidServicesEvent event) {}
    @Override public void hidFailure(HidServicesEvent event) {}
    @Override public void hidDataReceived(HidServicesEvent event) {}

    public java.util.Map<String, Object> getFullState() {
        java.util.HashMap<String, Object> state = new java.util.HashMap<>();
        state.put("A", isAButtonPressed());
        state.put("B", isBButtonPressed());
        state.put("X", isXButtonPressed());
        state.put("Y", isYButtonPressed());
        state.put("LeftXAxis", getLeftXAxis());
        state.put("LeftYAxis", getLeftYAxis());
        state.put("RightXAxis", getRightXAxis());
        state.put("RightYAxis", getRightYAxis());
        state.put("LeftTrigger", getLeftTrigger());
        state.put("RightTrigger", getRightTrigger());
        state.put("DPAD", getDPAD());
        state.put("LeftStickClick", isLeftStickClickPressed());
        state.put("RightStickClick", isRightStickClickPressed());
        return state;
    }
} 