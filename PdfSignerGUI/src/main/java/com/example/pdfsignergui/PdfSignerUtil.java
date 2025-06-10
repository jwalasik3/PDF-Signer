package com.example.pdfsignergui;

import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;

public class PdfSignerUtil {
    private static final int SALT_SIZE = 16;
    private static final int IV_SIZE = 16;
    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int AES_KEY_SIZE = 256;
    private static final String CERT_PATH = "../Certs/cert_for_signing.cer";

    public static void signPdf(File srcPdfPath, File destPdfPath, File usbPath, String pin) throws Exception {
        PrivateKey privateKey = decryptPrivateKey(usbPath, pin);
        Certificate[] chain = loadCertificateChain();

        System.out.println("Signing...");
        PdfSignerGui.setStateLabel("Signing...");
        PdfSigner signer = new PdfSigner(new PdfReader(srcPdfPath), new FileOutputStream(destPdfPath), new StampingProperties());

        PdfSignatureAppearance appearance = signer.getSignatureAppearance();
        appearance
                .setReason("Proj")
                .setLocation("localhost")
                .setReuseAppearance(false)
                .setPageNumber(1);
        signer.setFieldName("sig");

        BouncyCastleProvider provider = new BouncyCastleProvider();
        Security.addProvider(provider);

        IExternalSignature signature = new PrivateKeySignature(privateKey, DigestAlgorithms.SHA256, provider.getName());
        IExternalDigest digest = new BouncyCastleDigest();

        signer.signDetached(digest, signature, chain, null, null, null, 0, PdfSigner.CryptoStandard.CADES);
        System.out.println("Signed.");
        PdfSignerGui.setStateLabel("Pdf Signed.");
    }

    public static PrivateKey decryptPrivateKey(File encryptedFile, String pin) throws Exception {
        byte[] fileBytes = Files.readAllBytes(encryptedFile.toPath());

        // Extract salt and IV
        byte[] salt = new byte[SALT_SIZE];
        byte[] iv = new byte[IV_SIZE];
        byte[] encryptedKeyBytes = new byte[fileBytes.length - SALT_SIZE - IV_SIZE];

        System.arraycopy(fileBytes, 0, salt, 0, SALT_SIZE);
        System.arraycopy(fileBytes, SALT_SIZE, iv, 0, IV_SIZE);
        System.arraycopy(fileBytes, SALT_SIZE + IV_SIZE, encryptedKeyBytes, 0, encryptedKeyBytes.length);

        // Derive AES key from PIN
        PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_SIZE);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        SecretKey tmpKey = factory.generateSecret(spec);
        SecretKey aesKey = new SecretKeySpec(tmpKey.getEncoded(), "AES");

        // Decrypt the key
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(iv));
        byte[] decryptedKeyBytes = cipher.doFinal(encryptedKeyBytes);

        // Convert to RSA PrivateKey
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decryptedKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    // Getting the cert
    public static Certificate[] loadCertificateChain() throws Exception {
        File certFile = new File(CERT_PATH);

        // Checking if file exists
        if (!certFile.exists()) {
            throw new FileNotFoundException("Certificate file not found: " + certFile.getAbsolutePath());
        }

        try (FileInputStream fis = new FileInputStream(certFile)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate cert = cf.generateCertificate(fis);
            return new Certificate[]{cert};
        }
    }

    public static boolean verifySignature(File pdfPath, PublicKey publicKey) {
        BouncyCastleProvider provider = new BouncyCastleProvider();
        Security.addProvider(provider);
        try {
            PdfDocument pdfDoc = new PdfDocument(new PdfReader(pdfPath));
            PdfAcroForm acroForm = PdfAcroForm.getAcroForm(pdfDoc, false);

            if (acroForm == null || acroForm.getFormFields().isEmpty()) {
                System.out.println("No signatures found in the PDF.");
                PdfSignerGui.setStateLabel("No signatures found in the PDF.");
                return false;
            }

            SignatureUtil signatureUtil = new SignatureUtil(pdfDoc);
            for (String name : acroForm.getFormFields().keySet()) {
                PdfFormField field = acroForm.getField(name);
                if (!PdfName.Sig.equals(field.getFormType())) {
                    continue;
                }

                PdfPKCS7 pkcs7 = signatureUtil.readSignatureData(name);
                System.out.println("Checking signature integrity...");
                PdfSignerGui.setStateLabel("Checking signature integrity...");

                if (pkcs7.verifySignatureIntegrityAndAuthenticity()) {
                    System.out.println("Signature " + name + " is valid.");
                    PdfSignerGui.setStateLabel("Signature " + name + " is valid.");

                    if(publicKey == null){
                        PdfSignerGui.setStateLabel("No public key given.");
                        return false;
                    }
                    if (pkcs7.getSigningCertificate().getPublicKey().equals(publicKey)) {
                        System.out.println("Signature matches the public key.");
                        PdfSignerGui.setStateLabel("Signature matches the public key.");
                    } else {
                        System.out.println("Signature does not match the public key.");
                        PdfSignerGui.setStateLabel("Signature does not match the public key.");
                        pdfDoc.close();
                        return false;
                    }
                } else {
                    System.out.println("Signature " + name + " is invalid.");
                    PdfSignerGui.setStateLabel("Signature " + name + " is invalid.");
                    pdfDoc.close();
                    return false;
                }
            }
            pdfDoc.close();
            return true;
        } catch (Exception e) {
            System.out.println("Error during PDF signature verification: " + e.getMessage());
            PdfSignerGui.setStateLabel("Error during PDF signature verification: " + e.getMessage());
            return false;
        }
    }

    public static PublicKey readKeyFromFile(String publicKeyFilePath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        if(publicKeyFilePath == null || publicKeyFilePath.isEmpty()){
            return null;
        }

        byte[] encodedKeyBytes = Files.readAllBytes(Paths.get(publicKeyFilePath));
        String encodedKeyString = new String(encodedKeyBytes);

        byte[] decodedKey = Base64.getDecoder().decode(encodedKeyString);

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return keyFactory.generatePublic(keySpec);
    }
}

