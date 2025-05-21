package com.example.pdfsignerkeygen;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class MainController {
    @FXML
    private TextField pinField;
    @FXML
    private TextField usbPathField;
    @FXML
    private TextField publicKeyPathField;
    @FXML
    private Label responseLabel;

    @FXML
    protected void generateKeys() throws Exception {
        String pin = pinField.getText();
        String usbPath = usbPathField.getText();
        String publicKeyPath = publicKeyPathField.getText();
        try {
            RSAKeyGenAndEncrypt.encode(pin, usbPath, publicKeyPath);
            responseLabel.setText("Keys generated and stored successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            responseLabel.setText("Error generating keys: " + e.getMessage());
        }
    }
}