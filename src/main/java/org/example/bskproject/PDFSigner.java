package org.example.bskproject;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;

public class PDFSigner extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(PDFSigner.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);
        stage.setTitle("PDF Signature Tool");
        stage.setScene(scene);
        stage.show();

        startDriveMonitoring();
    }

    private void startDriveMonitoring() {
        Timer timer = new Timer(true); // Daemon thread
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    checkForDrive();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }, 0, 2000);
    }

    private void checkForDrive() throws Exception {
        File usbDrive = null;

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
                    break;
                }

            } catch (Exception e) {
                // Ignoring inaccessible drives
            }
        }

        SharedState sharedState = SharedState.getInstance();

        if (usbDrive != null && !sharedState.isDriveFound()) {
            sharedState.setDriveFound(true);
            File usbPath = new File(usbDrive, "qwertz/usb_private_key.enc");
            sharedState.setPrivateKey(usbPath);
        } else if (usbDrive == null && sharedState.isDriveFound()) {
            sharedState.setDriveFound(false);
        }

        // System.out.println("Is there a drive connected: " + sharedState.isDriveFound());
        // if (sharedState.isDriveFound()) {
        //     assert usbDrive != null;
        //     System.out.println("Drive is: " + usbDrive.getAbsolutePath());
        // }
    }

    public static void main(String[] args) {
        launch();
    }
}