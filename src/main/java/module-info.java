module org.example.bskproject {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.bskproject to javafx.fxml;
    exports org.example.bskproject;
}