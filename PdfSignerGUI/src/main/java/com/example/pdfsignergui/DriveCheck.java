package com.example.pdfsignergui;

import javafx.application.Platform;

import java.io.File;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;

public class DriveCheck {
    private boolean isDetected = false;
    private File usbDrive = null;

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

                if (isRemovable && rootFile.canRead() && rootFile.getTotalSpace() > 0) {
                    usbDrive = rootFile;
                    found = true;
                    break;
                }

            } catch (Exception e){
                // Ignoring inaccessible drives
            }
        }

        if (found && !isDetected) {
            isDetected = true;
            Platform.runLater(() ->  PdfSignerGui.setUsbLabel("Hardware key detected: " + usbDrive));
        } else if (!found && isDetected) {
            isDetected = false;
            usbDrive = null;
            Platform.runLater(() ->  PdfSignerGui.setUsbLabel("Hardware key not connected."));
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

    public String getUsbFile(){
        if (usbDrive != null) {
            return usbDrive.toString();
        }
        return null;
    }
}
