package com.example.pdfsignerkeygen;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * @brief Main entry point for the PDF Signer Key Generator JavaFX application.
 *
 * The `MainView` class initializes and displays the user interface
 * responsible for generating RSA keys and encrypting the private key
 * for secure storage. It loads its UI components from the `main-view.fxml` file.
 *
 * @author Your Name (You can replace this with your actual name)
 * @version 1.0
 */
public class MainView extends Application {
    /**
     * @brief The primary start method for the JavaFX application.
     *
     * This method is automatically called by the JavaFX runtime upon application launch.
     * It loads the FXML layout for the main view, sets up the scene, and displays
     * the primary stage. It also initializes the `MainController` and starts
     * a `DriveCheck` service to monitor for USB drives.
     *
     * @param stage The primary {@link javafx.stage.Stage} for this application,
     * onto which the application's scene is set.
     * @throws IOException If the `main-view.fxml` file cannot be loaded,
     * indicating a problem with the UI resource.
     */
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainView.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 640, 480);
        stage.setTitle("PDF Signer Key Generator");
        stage.setScene(scene);
        stage.show();

        // Get the controller instance to pass to DriveCheck
        MainController controller = fxmlLoader.getController();
        // Initialize and start the DriveCheck service, passing the controller
        // to allow it to update the UI with drive detection status.
        DriveCheck checker = new DriveCheck(controller);
        checker.startDriveMonitoring();
    }

    /**
     * @brief The main method to launch the JavaFX application.
     *
     * This is the standard entry point for any Java application.
     * It calls `Application.launch()` to start the JavaFX runtime and
     * subsequently invoke the `start()` method.
     *
     * @param args Command line arguments passed to the application.
     * These are not explicitly used in this application.
     */
    public static void main(String[] args) {
        launch();
    }
}