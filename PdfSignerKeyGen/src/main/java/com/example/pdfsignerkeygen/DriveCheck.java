package com.example.pdfsignerkeygen;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;

public class DriveCheck {
    private final MainController controller;
    private boolean isDetected = false;
    private File usbDrive = null;

    public DriveCheck(MainController controller) {
        this.controller = controller;
    }

    private void checkForDrive() throws Exception {
        Boolean found = false;

        System.out.println("Looking for a removable drive...");
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            try {
                FileStore store = Files.getFileStore(root);
                String type = store.type().toLowerCase();

                boolean isRemovable = store.toString().toLowerCase().contains("removable")
                        || type.contains("fat")
                        || type.contains("exfat");

                File rootFile = root.toFile();

                if (isRemovable && rootFile.canRead() && rootFile.getTotalSpace() > 0) {
                    usbDrive = rootFile;
                    System.out.println("Found drive: " + usbDrive.toString());
                    found = true;
                    break;
                }

            } catch (Exception e){
                // Ignoring inaccessible drives
            }
        }

        if (found && !isDetected) {
            isDetected = true;
            System.out.println("Inputting drive into the application.");
            controller.setUsbPath(usbDrive.toString());
        } else if (!found && isDetected) {
            isDetected = false;
            System.out.println("Clearing text field.");
            controller.setUsbPath("");
        }
    }

    public void startDriveMonitoring() {
        Timer timer = new Timer(true); // Daemon thread
        timer.scheduleAtFixedRate(new TimerTask() {
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
}
