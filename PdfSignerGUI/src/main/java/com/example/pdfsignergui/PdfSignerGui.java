package com.example.pdfsignergui;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.util.Timer;
import java.util.TimerTask;

/**
 * JavaFX application for signing and verifying PDF files.
 * This application monitors for a USB drive containing a private key,
 * allows users to select a PDF file, and provides options to sign or verify the PDF.
 */
public class PdfSignerGui extends Application {
    private static Scene scene;
    DriveCheck checker;
    File usbPath = null;
    static File pdfFile = null;
    static File pubKeyFile = null;

    /**
     * Initializes the JavaFX application.
     *
     * @param stage the primary stage for this application
     * @throws IOException if the FXML file cannot be loaded
     */
    @Override
    public void start(Stage stage) throws IOException {
        initAppView(stage);

        checker = new DriveCheck();
        checker.startDriveMonitoring();
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
        scene = new Scene(fxmlLoader.load(), 600, 600);
        stage.setTitle("PDF Signature Tool");
        stage.setScene(scene);
        VBox root = (VBox) scene.getRoot();
        Label welcomeLabel = new Label("Welcome to the PDF Signature Tool!\n What do you want to do?");

        RadioButton signPdfRadio = new RadioButton("Sign a PDF");
        RadioButton verifyPdfRadio = new RadioButton("Verify a PDF Signature");
        ToggleGroup toggleGroup = new ToggleGroup();
        signPdfRadio.setToggleGroup(toggleGroup);
        verifyPdfRadio.setToggleGroup(toggleGroup);

        Label usbLabel = new Label("Hardware key not connected.");
        usbLabel.setId("usbLabel");
        usbLabel.setVisible(false);

        PasswordField pinField = new PasswordField();
        pinField.setPromptText("Pin");
        pinField.setMaxWidth(60);
        pinField.setVisible(false);

        Button signButton = new Button("Sign PDF");
        signButton.setOnAction(event -> {
            if (checker.getUsbFile() != null) {
                // Get private key file
                usbPath = new File(checker.getUsbFile(), ".keys\\private_key.enc");

                // Check for pin
                if (pinField.getText() == null) {
                    System.out.println("Pin not provided.");
                    PdfSignerGui.setStateLabel("Pin not provided.");
                    return;
                }

                // Checking for PDF file
                if (pdfFile == null) {
                    System.out.println("No PDF selected.");
                    PdfSignerGui.setStateLabel("No PDF selected.");
                    return;
                }

                File destPdfFile = new File(pdfFile.getParent(), "signed_" + pdfFile.getName());

                // Signing the PDF file
                try {
                    PdfSignerUtil.signPdf(pdfFile, destPdfFile, usbPath, pinField.getText());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            } else {
                System.out.println("No USB drive found.");
                PdfSignerGui.setStateLabel("No USB drive found.");
            }

        });
        signButton.setVisible(false);

        Button verifyButton = new Button("Verify PDF Signature");
        verifyButton.setOnAction(event -> {
            if (pdfFile == null) {
                System.out.println("No PDF selected.");
                PdfSignerGui.setStateLabel("No PDF selected.");
                return;
            }

            try {
                System.out.println("Verifying PDF signature...");
                if (pubKeyFile == null){
                    PdfSignerGui.setStateLabel("No Public Key provided.");
                } else {
                    if(pdfFile == null){
                        PdfSignerGui.setStateLabel("No Pdf provided.");
                    } else {
                        boolean isValid = PdfSignerUtil.verifySignature(pdfFile, PdfSignerUtil.readKeyFromFile(pubKeyFile.getAbsolutePath()));
                        if (isValid) {
                            System.out.println("The PDF signature is valid.");
                            setStateLabel("The PDF signature is valid.");
                        } else {
                            System.out.println("The PDF signature is invalid.");
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Error during PDF signature verification: " + e.getMessage());
                PdfSignerGui.setStateLabel("Error during PDF signature verification: " + e.getMessage());
            }
        });
        verifyButton.setVisible(false);

        Button choosePdf = getButton(stage);
        Label pdfName = new Label("No PDF selected");
        pdfName.setId("pdfName");

        Button choosePem = getPemButton(stage);
        Label pemName = new Label("No public key selected for verification.");
        pemName.setId("pemName");
        choosePem.setVisible(false);
        pemName.setVisible(false);

        toggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>()
        {
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
                        choosePem.setVisible(false);
                        pemName.setVisible(false);
                        usbLabel.setVisible(true);
                    }
                    else if (s.equals("Verify a PDF Signature")) {
                        pinField.setVisible(false);
                        verifyButton.setVisible(true);
                        signButton.setVisible(false);
                        choosePem.setVisible(true);
                        pemName.setVisible(true);
                        usbLabel.setVisible(false);
                    }
                }
            }
        });
        Label stateLabel = new Label("");
        stateLabel.setId("stateLabel");
        root.getChildren().addAll(welcomeLabel, signPdfRadio, verifyPdfRadio, choosePdf, pdfName, choosePem, pemName, usbLabel, pinField, signButton, verifyButton, stateLabel);
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
        fileChooser.getExtensionFilters().add(new ExtensionFilter("PDF files (*.pdf)", "*.pdf"));

        Button choosePdf = new Button("Choose PDF File");
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

    private static Button getPemButton(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a .pem file");
        fileChooser.getExtensionFilters().add(new ExtensionFilter("PEM files (*.pem)", "*.pem"));

        Button choosePem = new Button("Choose Public Key File");
        choosePem.setOnAction(event -> {
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                System.out.println("Selected .pem: " + file.getAbsolutePath());
                pubKeyFile = file;
                Label pemName = (Label) stage.getScene().lookup("#pemName");
                if (pemName != null) {
                    pemName.setText("Selected .pem: " + file.getName());
                }
            }
        });
        return choosePem;
    }

    public static void setStateLabel(String string){
        Label stateLabel = (Label) scene.lookup("#stateLabel");
        stateLabel.setText(string);
    }

    public static void setUsbLabel(String string){
        Label stateLabel = (Label) scene.lookup("#usbLabel");
        stateLabel.setText(string);
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