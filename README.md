# PDF-Signer

## Overview
PDF-Signer is a Java-based application that allows users to sign PDF documents using a private key stored on a USB drive. The application monitors the presence of the USB drive and prompts the user to enter a PIN to decrypt the private key when the drive is detected.

## Features
- **PDF Selection**: Users can select a PDF file to sign.
- **USB Drive Monitoring**: The application continuously monitors for the presence of a USB drive containing the private key.
- **PIN Prompt**: When the USB drive is detected, the user is prompted to enter a PIN to decrypt the private key.
- **PDF Signing**: The selected PDF is signed using the decrypted private key.

## Requirements
- Java 11 or higher
- JavaFX
- Maven

## Setup
1. **Clone the repository**:
    ```sh
    git clone https://github.com/jwalasik3/pdf-signer.git
    cd pdf-signer
    ```

2. **Build the project**:
    ```sh
    mvn clean install
    ```

3. **Run the application**:
    ```sh
    mvn javafx:run
    ```

## Usage
1. **Start the application**: Run the application using the setup instructions above.
2. **Select a PDF**: Click on the "Select PDF" button to choose a PDF file to sign.
3. **Insert USB Drive**: Insert the USB drive containing the private key to load it to the application.
4. **Enter PIN**: When prompted, enter the PIN to decrypt the private key.
5. **Sign PDF**: Click on the "Sign PDF" button to sign the selected PDF.

## License
This project is licensed under the MIT License. See the `LICENSE` file for details.