module com.example.pdfsignerkeygen {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires org.bouncycastle.provider;
    requires org.bouncycastle.pkix;

    opens com.example.pdfsignerkeygen to javafx.fxml;
    exports com.example.pdfsignerkeygen;
}