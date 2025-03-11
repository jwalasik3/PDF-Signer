package org.example.bskproject;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class PDFSigner extends Application {

    private static final String PRIVATE_KEY_PATH = "F:/qwertz/usb_private_key.enc";

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
                checkForDrive();
            }
        }, 0, 2000);
    }

    private void checkForDrive() {
        File drive = new File(PRIVATE_KEY_PATH);
        boolean isCurrentlyPresent = drive.exists();

        SharedState sharedState = SharedState.getInstance();
        if (isCurrentlyPresent && !sharedState.isDriveFound()) {
            sharedState.setDriveFound(true);
            sharedState.setPrivateKey(drive);
        } else if (!isCurrentlyPresent && sharedState.isDriveFound()) {
            sharedState.setDriveFound(false);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}