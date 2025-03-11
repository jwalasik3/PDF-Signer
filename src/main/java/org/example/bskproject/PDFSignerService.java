package org.example.bskproject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.util.Calendar;

public class PDFSignerService implements SignatureInterface {

    private final PrivateKey privateKey;
    private final Certificate certificate;

    public PDFSignerService(PrivateKey privateKey, Certificate certificate) {
        this.privateKey = privateKey;
        this.certificate = certificate;
    }

    @Override
    public byte[] sign(InputStream content) throws IOException {
        try {
            Security.addProvider(new BouncyCastleProvider());
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = content.read(buffer)) != -1) {
                signature.update(buffer, 0, bytesRead);
            }

            return signature.sign();
        } catch (Exception e) {
            throw new IOException("Error while signing the PDF: " + e.getMessage(), e);
        }
    }

    public void signPDF(File inputFile, File outputFile) throws Exception {
        try (PDDocument document = PDDocument.load(inputFile)) {
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName("PDF Signature Tool");
            signature.setSignDate(Calendar.getInstance());

            document.addSignature(signature, this);
            document.saveIncremental(new FileOutputStream(outputFile));
        }
    }
}
