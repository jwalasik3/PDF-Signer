package com.example.pdfsignerkeygen;

import java.io.File;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @brief Utility class for monitoring the presence of removable (USB) drives.
 *
 * The `DriveCheck` class continuously scans the system's root directories to detect
 * if a USB drive is connected. When a drive is detected or removed, it updates
 * the associated {@link MainController} with the USB drive's path, allowing the UI
 * to reflect the current status.
 */
public class DriveCheck {
    /**
     * @brief Reference to the {@link MainController} to update UI elements.
     */
    private final MainController controller;
    /**
     * @brief A boolean flag indicating whether a USB drive is currently detected.
     * `true` if a USB drive is found, `false` otherwise.
     */
    private boolean isDetected = false;
    /**
     * @brief Stores the {@link java.io.File} object representing the detected USB drive's root directory.
     * Null if no USB drive is currently detected.
     */
    private File usbDrive = null;

    /**
     * @brief Constructs a new `DriveCheck` instance.
     *
     * @param controller The {@link MainController} instance that this `DriveCheck`
     * will communicate with to update the UI regarding USB drive status.
     */
    public DriveCheck(MainController controller) {
        this.controller = controller;
    }

    /**
     * @brief Scans the system's root directories to detect the presence of a removable drive.
     *
     * This private helper method iterates through all available file system roots,
     * checks their type (e.g., FAT, exFAT) and properties (readability, total space)
     * to identify potential removable drives. It updates the `isDetected` flag,
     * `usbDrive` field, and notifies the {@link MainController} of any changes.
     *
     * @throws Exception If an unexpected error occurs during file system access.
     * (Note: Expected exceptions for inaccessible drives are caught internally).
     */
    private void checkForDrive() throws Exception {
        Boolean found = false;

        System.out.println("Looking for a removable drive...");
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            try {
                FileStore store = Files.getFileStore(root);
                String type = store.type().toLowerCase();

                // Criteria for identifying a removable drive
                boolean isRemovable = store.toString().toLowerCase().contains("removable")
                        || type.contains("fat")
                        || type.contains("exfat");

                File rootFile = root.toFile();

                // Check if it's a removable drive, readable, and has available space
                if (isRemovable && rootFile.canRead() && rootFile.getTotalSpace() > 0) {
                    usbDrive = rootFile;
                    System.out.println("Found drive: " + usbDrive.toString());
                    found = true;
                    break; // Found a suitable drive, no need to check further
                }

            } catch (Exception e){
                // @brief Catching and ignoring exceptions for inaccessible drives (e.g., disconnected network drives, empty CD-ROMs).
                // This prevents the monitoring process from crashing on system drives that are not ready or are restricted.
            }
        }

        // Update detection status and notify the controller if status changes
        if (found && !isDetected) {
            isDetected = true;
            System.out.println("Inputting drive into the application.");
            controller.setUsbPath(usbDrive.toString()); // Update UI with detected USB path
        } else if (!found && isDetected) {
            isDetected = false;
            usbDrive = null; // Clear the USB drive reference
            System.out.println("Clearing text field.");
            controller.setUsbPath(""); // Clear UI field as drive is no longer detected
        }
    }

    /**
     * @brief Starts a background thread to continuously monitor for USB drives.
     *
     * This method initializes a {@link java.util.Timer} and schedules a {@link java.util.TimerTask}
     * to execute the `checkForDrive()` method periodically (every 2 seconds).
     * The monitoring runs on a daemon thread, allowing the application to exit cleanly
     * without waiting for the timer to complete.
     */
    public void startDriveMonitoring() {
        Timer timer = new Timer(true); // Daemon thread ensures the timer doesn't prevent JVM exit
        timer.scheduleAtFixedRate(new TimerTask() {
            /**
             * @brief The action to be performed by this timer task.
             *
             * This method is executed repeatedly by the timer and calls `checkForDrive()`
             * to check for USB drive presence. Exceptions during this check are caught
             * and ignored to ensure continuous monitoring.
             */
            @Override
            public void run() {
                try {
                    checkForDrive();
                } catch (Exception e) {
                    // @brief Ignoring exceptions that might occur during drive check,
                    // to prevent interruption of the monitoring process.
                    System.err.println("Error during drive check: " + e.getMessage()); // Log unexpected errors
                }
            }
        }, 0, 2000); // Start immediately (0 delay), repeat every 2000 milliseconds (2 seconds)
    }
}