module com.example.pdfsignerkeygen {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;

    opens com.example.pdfsignerkeygen to javafx.fxml;
    exports com.example.pdfsignerkeygen;
}