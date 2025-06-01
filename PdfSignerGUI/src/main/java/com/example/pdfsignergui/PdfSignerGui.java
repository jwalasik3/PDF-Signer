package com.example.pdfsignergui;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;

public class PdfSignerGui extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        initAppView(stage);
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
                    // Ignore exceptions for inaccessible drives
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

            } catch (Exception e){
                // Ignoring inaccessible drives
            }
        }
        SharedState sharedState = SharedState.getInstance();

        if (usbDrive != null && !sharedState.isDriveFound()) {
            sharedState.setDriveFound(true);
            File usbPath = new File(usbDrive, "private_key.enc");
            sharedState.setPrivateKey(usbPath);
        } else if (usbDrive == null && sharedState.isDriveFound()) {
            sharedState.setDriveFound(false);
        }
    }

    private void initAppView(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(PdfSignerGui.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);
        stage.setTitle("PDF Signature Tool");
        stage.setScene(scene);
        VBox root = (VBox) scene.getRoot();
        Label welcomeLabel = new Label("Welcome to the PDF Signature Tool!\n What do you want to do?");

        RadioButton signPdfRadio = new RadioButton("Sign a PDF");
        RadioButton verifyPdfRadio = new RadioButton("Verify a PDF Signature");
        ToggleGroup toggleGroup = new ToggleGroup();
        signPdfRadio.setToggleGroup(toggleGroup);
        verifyPdfRadio.setToggleGroup(toggleGroup);

        Button signButton = new Button("Sign PDF");
        signButton.setOnAction(event -> {
            // Action for signing a PDF
            System.out.println("Signing PDF...");
        });
        signButton.setVisible(false);

        Button verifyButton = new Button("Verify PDF Signature");
        verifyButton.setOnAction(event -> {
            // Action for verifying a PDF signature
            System.out.println("Verifying PDF Signature...");
        });
        verifyButton.setVisible(false);

        Button choosePdf = getButton(stage);
        Label pdfName = new Label("No PDF selected");
        pdfName.setId("pdfName");

        toggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>()
        {
            public void changed(ObservableValue<? extends Toggle> ob,
                                Toggle o, Toggle n)
            {

                RadioButton rb = (RadioButton)toggleGroup.getSelectedToggle();

                if (rb != null) {
                    String s = rb.getText();
                    if (s.equals("Sign a PDF")) {
                        signButton.setVisible(true);
                        verifyButton.setVisible(false);
                    }
                    else if (s.equals("Verify a PDF Signature")) {
                        verifyButton.setVisible(true);
                        signButton.setVisible(false);
                    }
                }
            }
        });
        root.getChildren().addAll(welcomeLabel, signPdfRadio, verifyPdfRadio, choosePdf, pdfName, signButton, verifyButton);
        stage.show();
    }

    private static Button getButton(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a PDF File");
        Button choosePdf = new Button("Choose PDF File");
        choosePdf.setOnAction(event -> {
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                System.out.println("Selected PDF: " + file.getAbsolutePath());
                SharedState sharedState = SharedState.getInstance();
                sharedState.setSelectedPdf(file);
                Label pdfName = (Label) stage.getScene().lookup("#pdfName");
                if (pdfName != null) {
                    pdfName.setText("Selected PDF: " + file.getName());
                }
            }
        });
        return choosePdf;
    }

    public static void main(String[] args) {
        launch();
    }
}