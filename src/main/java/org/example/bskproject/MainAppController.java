package org.example.bskproject;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class MainAppController{

    @FXML
    private Label pdfName;

    @FXML
    private TextArea welcomeText;

    final FileChooser fileChooser = new FileChooser();

    File selectedPdf;

    @FXML
    protected void selectPdf() {
        fileChooser.setTitle("Select PDF file");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        selectedPdf = fileChooser.showOpenDialog(null);
        if (selectedPdf != null) {
            pdfName.setText(selectedPdf.getAbsolutePath());
        } else {
            pdfName.setText("");
            System.out.println("No file selected.");
        }
    }

    @FXML
    protected void signPdf() throws Exception {
        if (selectedPdf == null) {
            System.out.println("No PDF file selected.");
            return;
        }

        SharedState sharedState = SharedState.getInstance();
        if (sharedState.getPrivateKey() == null) {
            System.out.println("Private key not found.");
            return;
        }

//        // Decrypt private key
//        PrivateKey privateKey = RSAKeyGenAndEncrypt.decryptPrivateKey("user-pin");
//
//        // Load the public certificate (assuming it's already available)
//        Certificate certificate = ...;  // Load from file or keystore
//
//        PDFSignerService signer = new PDFSignerService(privateKey, certificate);
//
//        File signedFile = new File(selectedPdf.getParent(), "signed_" + selectedPdf.getName());
//        signer.signPDF(selectedPdf, signedFile);
//
//        System.out.println("PDF signed successfully: " + signedFile.getAbsolutePath());
    }
}