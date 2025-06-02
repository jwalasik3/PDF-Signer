package com.example.pdfsignergui;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
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

/**
 * JavaFX application for signing and verifying PDF files.
 * This application monitors for a USB drive containing a private key,
 * allows users to select a PDF file, and provides options to sign or verify the PDF.
 */
public class PdfSignerGui extends Application {

    /**
     * Initializes the JavaFX application.
     *
     * @param stage the primary stage for this application
     * @throws IOException if the FXML file cannot be loaded
     */
    @Override
    public void start(Stage stage) throws IOException {
        initAppView(stage);
        startDriveMonitoring();
    }

    /**
     * Starts monitoring for a USB drive containing the private key.
     * This method runs in a background thread and checks every 2 seconds.
     */
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

    /**
     * Checks for a USB drive that contains the private key file.
     * If found, updates the shared state with the drive information.
     * If not found, updates the shared state to indicate no drive is present.
     *
     * @throws Exception if an error occurs while checking drives, but it is ignored since the method is called in a background thread.
     */
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

    /**
     * Initializes the main application view.
     * Loads the FXML layout, sets up the scene, and adds controls for signing and verifying PDFs.
     *
     * @param stage the primary stage for this application
     * @throws IOException if the FXML file cannot be loaded
     */
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

    /**
     * Creates a button that allows the user to select a PDF file.
     * When clicked, it opens a file chooser dialog and updates the shared state with the selected PDF file.
     *
     * @param stage the primary stage for this application
     * @return the button for selecting a PDF file
     */
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

    /**
     * The main method to launch the JavaFX application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch();
    }
}