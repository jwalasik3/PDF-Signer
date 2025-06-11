package com.example.pdfsignergui;

import javafx.application.Platform;

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
 * The `DriveCheck` class provides functionality to periodically scan the system's
 * root directories to detect if a USB drive is connected. It keeps track of the
 * detection status and the path to the detected USB drive.
 */
public class DriveCheck {
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
     * @brief Scans the system's root directories to detect the presence of a removable drive.
     *
     * This method iterates through all available file system roots, checks their type (e.g., FAT, exFAT)
     * and properties (readability, total space) to identify potential removable drives.
     * It updates the `isDetected` flag and `usbDrive` field based on the scan results.
     *
     * @throws Exception If an unexpected error occurs during file system access.
     * (Note: Expected inaccessible drive exceptions are caught internally).
     */
    private void checkForDrive() throws Exception {
        Boolean found = false;
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            try {
                FileStore store = Files.getFileStore(root);
                String type = store.type().toLowerCase();

                boolean isRemovable = store.toString().toLowerCase().contains("removable")
                        || type.contains("fat")
                        || type.contains("exfat");

                File rootFile = root.toFile();

                // Check if it's a removable drive, readable, and has available space
                if (isRemovable && rootFile.canRead() && rootFile.getTotalSpace() > 0) {
                    usbDrive = rootFile;
                    found = true;
                    break;
                }

            } catch (Exception e){
                // @brief Catching and ignoring exceptions for inaccessible drives (e.g., CD-ROMs without disc).
                // This prevents the application from crashing on system drives that are not ready.
            }
        }

        // Update detection status based on current scan result
        if (found && !isDetected) {
            isDetected = true;
            Platform.runLater(() ->  PdfSignerGui.setUsbLabel("Hardware key detected: " + usbDrive));
        } else if (!found && isDetected) {
            isDetected = false;
            usbDrive = null;
            Platform.runLater(() ->  PdfSignerGui.setUsbLabel("Hardware key not connected."));
        }
    }

    /**
     * @brief Starts a background thread to continuously monitor for USB drives.
     *
     * This method initializes a {@link java.util.Timer} and schedules a {@link java.util.TimerTask}
     * to execute the `checkForDrive()` method periodically (every 2 seconds).
     * The monitoring runs on a daemon thread, allowing the application to exit cleanly.
     */
    public void startDriveMonitoring() {
        Timer timer = new Timer(true); // Daemon thread
        timer.scheduleAtFixedRate(new TimerTask() {
            /**
             * @brief The action to be performed by this timer task.
             *
             * This method is executed repeatedly by the timer and calls `checkForDrive()`
             * to check for USB drive presence. Exceptions during this check are ignored
             * to ensure continuous monitoring.
             */
            @Override
            public void run() {
                try {
                    checkForDrive();
                } catch (Exception e) {
                    // Ignore exceptions for inaccessible drives
                }
            }
        }, 0, 2000);
    }

    /**
     * @brief Returns the {@link java.lang.String} representation of the detected USB drive's root path.
     *
     * @return The string representation of the {@link java.io.File} object for the USB drive's root,
     * or `null` if no USB drive is currently detected.
     */
    public String getUsbFile(){
        if (usbDrive != null) {
            return usbDrive.toString();
        }
        return null;
    }
}
