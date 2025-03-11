module org.example.bskproject {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.pdfbox;
    requires org.bouncycastle.provider;


    opens org.example.bskproject to javafx.fxml;
    exports org.example.bskproject;
}