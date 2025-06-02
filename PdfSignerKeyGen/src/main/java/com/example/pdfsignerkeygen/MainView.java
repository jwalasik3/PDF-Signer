package com.example.pdfsignerkeygen;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * MainView is the entry point of the JavaFX application that provides a user interface
 * for generating RSA keys and encrypting the private key using AES encryption.
 * It loads the main-view.fxml file to display the UI (all UI components are defined in that file).
 */
public class MainView extends Application {

    /**
     * The start method is called by the JavaFX runtime to initialize the application.
     * It loads the FXML file and sets up the main stage with the scene.
     *
     * @param stage The primary stage for this application, onto which the application scene can be set.
     * @throws IOException If the FXML file cannot be loaded.
     */
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainView.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 640, 480);
        stage.setTitle("PDF Signer Key Generator");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * The main method is the entry point of the JavaFX application.
     * It launches the application by calling the launch method.
     *
     * @param args Command line arguments (not used in this application).
     */
    public static void main(String[] args) {
        launch();
    }
}