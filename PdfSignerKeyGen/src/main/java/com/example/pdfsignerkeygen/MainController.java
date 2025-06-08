package com.example.pdfsignerkeygen;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.File;

/**
 * MainController is the controller class for the main-view.fxml file.
 * It handles user interactions and manages the generation of RSA keys
 * and the encryption of the private key using AES encryption.
 */
public class MainController {
    @FXML
    private TextField pinField;
    @FXML
    private TextField usbPathField;
    @FXML
    private TextField publicKeyPathField;
    @FXML
    private Label responseLabel;

    /**
     * This method is called when the user clicks the "Generate Keys" button.
     * @throws Exception if there is an error during key generation.
     */
    @FXML
    protected void generateKeys() throws Exception {
        String pin = pinField.getText();
        String usbPath = usbPathField.getText() + ".keys\\";
        try {
            RSAKeyGenAndEncrypt.encode(pin, usbPath, usbPath);
            responseLabel.setText("Keys generated and stored successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            responseLabel.setText("Error generating keys: " + e.getMessage());
        }
    }

    @FXML
    public void setUsbPath(String path) {
        usbPathField.setText(path);
    }
}