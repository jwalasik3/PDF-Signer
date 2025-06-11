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

/**
 * @brief JavaFX application for signing and verifying PDF files.
 * This application provides a graphical user interface for digital signature operations
 * on PDF documents. It includes features to monitor for a USB drive containing a private key,
 * allow users to select a PDF file, and then either sign the PDF or verify an existing signature.
 *
 * @author Jakub Walasik
 * @version 1.0
 */
public class PdfSignerGui extends Application {
    /**
     * @brief An instance of DriveCheck to monitor USB drive presence.
     */
    DriveCheck checker;
    /**
     * @brief Stores the path to the private key file on the USB drive.
     * Initialized when a USB drive is detected and a signing operation is requested.
     */
    File usbPath = null;
    /**
     * @brief Stores the currently selected PDF file by the user.
     * This file is used for both signing and verification operations.
     */
    static File pdfFile = null;

    /**
     * @brief The main entry point for the JavaFX application.
     * This method initializes the application's user interface, sets up the primary stage,
     * and starts the background monitoring for USB drives.
     *
     * @param stage The primary {@link javafx.stage.Stage} for this application, onto which
     * the scene is set.
     * @throws IOException If the FXML file (main-view.fxml) cannot be loaded.
     */
    @Override
    public void start(Stage stage) throws IOException {
        initAppView(stage);

        checker = new DriveCheck();
        checker.startDriveMonitoring();
    }

    /**
     * @brief Initializes the main application view (GUI elements and their listeners).
     * This private helper method loads the FXML layout, sets up the scene, and
     * configures the various UI controls such as radio buttons for selecting modes,
     * password fields, and action buttons for signing and verifying PDFs.
     * It also defines listeners for UI element interactions.
     *
     * @param stage The primary {@link javafx.stage.Stage} where the UI elements will be displayed.
     * @throws IOException If the FXML file (main-view.fxml) cannot be loaded.
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

        PasswordField pinField = new PasswordField();
        pinField.setPromptText("Pin");
        pinField.setMaxWidth(60);
        pinField.setVisible(false);

        Button signButton = new Button("Sign PDF");
        /**
         * @brief Defines the action to be performed when the "Sign PDF" button is clicked.
         *
         * This lambda expression checks for the presence of a USB drive and a selected PDF file,
         * retrieves the PIN, and then calls {@link PdfSignerUtil#signPdf} to sign the document.
         * Provides feedback to the console if conditions are not met (e.g., no USB, no PDF, no PIN).
         */
        signButton.setOnAction(event -> {
            if (checker.getUsbFile() != null) {
                // Get private key file
                usbPath = new File(checker.getUsbFile(), ".keys\\private_key.enc");

                // Check for pin
                if (pinField.getText() == null || pinField.getText().isEmpty()) { // Added isEmpty() check
                    System.out.println("Pin not provided.");
                    return;
                }

                // Checking for PDF file
                if (pdfFile == null) {
                    System.out.println("No PDF selected.");
                    return;
                }

                File destPdfFile = new File(pdfFile.getParent(), "signed_" + pdfFile.getName());

                // Signing the PDF file
                try {
                    PdfSignerUtil.signPdf(pdfFile, destPdfFile, usbPath, pinField.getText());
                    System.out.println("PDF signed successfully: " + destPdfFile.getAbsolutePath()); // Added success message
                } catch (Exception e) {
                    System.err.println("Error signing PDF: " + e.getMessage()); // Use err for errors
                    // Optionally, show an alert to the user
                    // new Alert(Alert.AlertType.ERROR, "Error signing PDF: " + e.getMessage()).showAndWait();
                    throw new RuntimeException("Failed to sign PDF", e); // Re-throw with more context
                }

            } else {
                System.out.println("No USB drive found.");
            }

        });
        signButton.setVisible(false);

        Button verifyButton = new Button("Verify PDF Signature");
        /**
         * @brief Defines the action to be performed when the "Verify PDF Signature" button is clicked.
         *
         * This lambda expression checks for a selected PDF file and then calls
         * {@link PdfSignerUtil#verifySignature} to check the integrity and authenticity of the signature.
         * Provides feedback to the console regarding the signature's validity.
         */
        verifyButton.setOnAction(event -> {
            if (pdfFile == null) {
                System.out.println("No PDF selected.");
                return;
            }

            try {
                System.out.println("Verifying PDF signature...");
                boolean isValid = PdfSignerUtil.verifySignature(pdfFile);
                if (isValid) {
                    System.out.println("The PDF signature is valid.");
                } else {
                    System.out.println("The PDF signature is invalid.");
                }
            } catch (Exception e) {
                System.err.println("Error during PDF signature verification: " + e.getMessage()); // Use err for errors
                // Optionally, show an alert to the user
                // new Alert(Alert.AlertType.ERROR, "Error during PDF signature verification: " + e.getMessage()).showAndWait();
            }
        });
        verifyButton.setVisible(false);

        Button choosePdf = getButton(stage);
        Label pdfName = new Label("No PDF selected");
        pdfName.setId("pdfName"); // ID for lookup

        /**
         * @brief Listener for changes in the selected radio button within the ToggleGroup.
         *
         * This ChangeListener manages the visibility of the PIN field, "Sign PDF" button,
         * and "Verify PDF Signature" button based on the user's selection
         * (either "Sign a PDF" or "Verify a PDF Signature").
         */
        toggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>()
        {
            /**
             * @brief Called when the selected toggle in the {@link ToggleGroup} changes.
             *
             * Adjusts the visibility of UI elements based on whether the user wants to sign or verify a PDF.
             *
             * @param ob The {@link ObservableValue} that changed (the ToggleGroup's selectedToggleProperty).
             * @param o The old (previously selected) Toggle.
             * @param n The new (currently selected) Toggle.
             */
            public void changed(ObservableValue<? extends Toggle> ob,
                                Toggle o, Toggle n)
            {

                RadioButton rb = (RadioButton)toggleGroup.getSelectedToggle();

                if (rb != null) {
                    String s = rb.getText();
                    if (s.equals("Sign a PDF")) {
                        pinField.setVisible(true);
                        signButton.setVisible(true);
                        verifyButton.setVisible(false);
                    }
                    else if (s.equals("Verify a PDF Signature")) {
                        pinField.setVisible(false);
                        verifyButton.setVisible(true);
                        signButton.setVisible(false);
                    }
                }
            }
        });
        Label stateLabel = new Label("");
        stateLabel.setId("stateLabel"); // ID for lookup (e.g., by DriveCheck to update USB status)
        root.getChildren().addAll(welcomeLabel, signPdfRadio, verifyPdfRadio, choosePdf, pdfName, pinField, signButton, verifyButton, stateLabel);
        stage.show();
    }

    /**
     * @brief Creates and configures a button for selecting a PDF file.
     *
     * This static helper method sets up a {@link javafx.stage.FileChooser} to allow the user
     * to browse and select a PDF file from their system. Upon selection, it updates
     * the static `pdfFile` variable and the `pdfName` label in the GUI.
     *
     * @param stage The primary {@link javafx.stage.Stage} that owns the file chooser dialog.
     * @return A {@link javafx.scene.control.Button} instance configured to open a PDF file chooser.
     */
    private static Button getButton(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a PDF File");
        // Optionally, add file filters if you want to restrict file types
         fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        Button choosePdf = new Button("Choose PDF File");
        /**
         * @brief Event handler for the "Choose PDF File" button.
         *
         * Opens a file selection dialog. If a file is selected, it updates the
         * `pdfFile` static variable and the `pdfName` label in the UI to reflect
         * the chosen file's name.
         */
        choosePdf.setOnAction(event -> {
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                System.out.println("Selected PDF: " + file.getAbsolutePath());
                pdfFile = file;
                Label pdfName = (Label) stage.getScene().lookup("#pdfName");
                if (pdfName != null) {
                    pdfName.setText("Selected PDF: " + file.getName());
                }
            }
        });
        return choosePdf;
    }

    /**
     * @brief The main method that launches the JavaFX application.
     *
     * This is the standard entry point for all JavaFX applications.
     *
     * @param args Command line arguments passed to the application. Not directly used in this application.
     */
    public static void main(String[] args) {
        launch();
    }
}