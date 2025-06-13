package com.example.pdfsignerkeygen;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * @brief Controller class for the main-view.fxml.
 *
 * This class handles user interactions within the key generation application.
 * It manages the input fields for PIN, USB path, and public key path,
 * and triggers the RSA key generation and private key encryption process
 * using the {@link RSAKeyGenAndEncrypt} utility. It also updates the UI
 * with feedback on the operation's success or failure.
 */
public class MainController {
    /**
     * @brief The {@link javafx.scene.control.TextField} for entering the PIN.
     * This PIN is used to encrypt the private key.
     */
    @FXML
    private TextField pinField;
    /**
     * @brief The {@link javafx.scene.control.TextField} for displaying and setting the USB drive path.
     * The encrypted private key will be saved to a `.keys` subdirectory on this path.
     */
    @FXML
    private TextField usbPathField;
    /**
     * @brief The {@link javafx.scene.control.TextField} for displaying and setting the public key path.
     * The public key (and certificate) will be saved to this path.
     * In the current implementation, it shares the path with the USB drive for consistency.
     */
    @FXML
    private TextField publicKeyPathField;
    /**
     * @brief The {@link javafx.scene.control.Label} used to display feedback messages to the user,
     * such as success messages or error details after key generation.
     */
    @FXML
    private Label responseLabel;

    /**
     * @brief Handles the action triggered when the "Generate Keys" button is clicked.
     *
     * This method retrieves the PIN from `pinField` and the USB path from `usbPathField`.
     * It then calls the static `encode` method of {@link RSAKeyGenAndEncrypt} to perform
     * the key generation and encryption. It updates the `responseLabel` with the outcome
     * of the operation.
     *
     * @throws Exception If an unhandled error occurs during the key generation or encryption process.
     * (Note: Exceptions from `RSAKeyGenAndEncrypt.encode` are caught and displayed).
     */
    @FXML
    protected void generateKeys() throws Exception {
        String pin = pinField.getText();
        // Append ".keys\" to the USB path to define the subdirectory for key storage
        String usbPath = usbPathField.getText() + ".keys\\";

        try {
            // Call the utility method to generate and encrypt keys
            // The publicKeyPath is set to the same usbPath here based on the current implementation.
            RSAKeyGenAndEncrypt.encode(pin, usbPath, publicKeyPathField.getText());
            responseLabel.setText("Keys generated and stored successfully.");
        } catch (Exception e) {
            // Display a user-friendly error message
            responseLabel.setText("Error generating keys: " + e.getMessage());
        }
    }

    /**
     * @brief Sets the text of the USB path text field.
     *
     * This method is typically called by the {@link DriveCheck} service
     * to update the UI with the detected USB drive's path.
     *
     * @param path The string path of the detected USB drive.
     */
    @FXML
    public void setUsbPath(String path) {
        usbPathField.setText(path);
    }
}