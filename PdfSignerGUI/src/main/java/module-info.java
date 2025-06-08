module com.example.pdfsignergui {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires kernel;
    requires sign;

    opens com.example.pdfsignergui to javafx.fxml;
    exports com.example.pdfsignergui;
}