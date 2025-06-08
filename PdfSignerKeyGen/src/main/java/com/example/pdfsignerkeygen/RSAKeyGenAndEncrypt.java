package com.example.pdfsignerkeygen;


import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * This is an auxiliary class that generates an RSA key pair, encrypts the private key using AES encryption,
 * and saves the encrypted private key to a file. The public key is saved to another file.
 * The user is prompted to enter a PIN, which is used to derive an AES key using PBKDF2.
 */
public class RSAKeyGenAndEncrypt {

    private static final int RSA_KEY_SIZE = 4096;
    private static final int AES_KEY_SIZE = 256;
    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int SALT_SIZE = 16;

    public static void encode(String pin, String usbPath, String publicKeyPath) throws Exception {

//        Generating RSA 4096-bit key pair
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(RSA_KEY_SIZE);
        KeyPair keyPair = keyPairGen.generateKeyPair();

//        Deriving AES key from PIN with salt to prevent rainbow table attacks
        byte[] salt = generateSalt();
        SecretKey aesKey = deriveAESKeyFromPIN(pin, salt);

        byte[] encryptedPrivateKey = encryptPrivateKey(keyPair.getPrivate(), aesKey);

        File usbFile = new File(usbPath, "private_key.enc");
        File parent = usbFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();  // Creates parent directory if it doesn't exist
            Files.setAttribute(Path.of(usbPath), "dos:hidden", true);
        }

        File pubFile = new File(publicKeyPath, "public_key.pem");
        System.out.println(pubFile);
        File parentDir = pubFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs(); // Create the directories if they don't exist
        }

//        Saving the encrypted private key to USB pendrive and public key to filepath
        saveToFile(String.valueOf(usbFile), salt, encryptedPrivateKey);
        savePublicKey(String.valueOf(pubFile), keyPair.getPublic());

        System.out.println("Keys generated and stored successfully.");
    }

    private static byte[] generateSalt() {
        byte[] salt = new byte[SALT_SIZE];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    private static SecretKey deriveAESKeyFromPIN(String pin, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_SIZE);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    private static byte[] encryptPrivateKey(PrivateKey privateKey, SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, new IvParameterSpec(iv));

        byte[] encryptedData = cipher.doFinal(privateKey.getEncoded());
        byte[] result = new byte[iv.length + encryptedData.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encryptedData, 0, result, iv.length, encryptedData.length);
        return result;
    }

    private static void saveToFile(String path, byte[] salt, byte[] data) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(salt);
            fos.write(data);
        }
    }

    private static void savePublicKey(String path, PublicKey publicKey) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(Base64.getEncoder().encode(publicKey.getEncoded()));
        }
    }
}
