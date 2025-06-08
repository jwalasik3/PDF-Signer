package com.example.pdfsignergui;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.*;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Calendar;
import java.util.Collections;

public class PdfSignerUtil {private static final int SALT_SIZE = 16;
    private static final int IV_SIZE = 16;
    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int AES_KEY_SIZE = 256;

    public static void signPdf(File srcPdfPath, File destPdfPath, File usbPath, String pin) throws Exception {
        PrivateKey privateKey = decryptPrivateKey(usbPath, pin);

        // Here the signing logic gets added
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
}

