package org.example.bskproject;


import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;

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
    private static final String USB_PATH = "F:/qwertz/usb_private_key.enc";
    private static final String PUBLIC_KEY_PATH = "C:/qwertz/keys/public_key.pem";

    public static void encode() throws Exception {

//        Getting user input for PIN using Scanner
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter PIN for key encryption: ");
        String pin = scanner.nextLine();
        scanner.close();

//        Generating RSA 4096-bit key pair
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(RSA_KEY_SIZE);
        KeyPair keyPair = keyPairGen.generateKeyPair();

//        Using salt for additional security
        byte[] salt = generateSalt();
        SecretKey aesKey = generateAesFromPin(pin, salt);

        byte[] encryptedPrivateKey = encryptPrivateKey(keyPair.getPrivate(), aesKey);

//        Saving the encrypted private key to USB pendrive and public key to filepath
        saveToFile(USB_PATH, salt, encryptedPrivateKey);
        savePublicKey(PUBLIC_KEY_PATH, keyPair.getPublic());

        System.out.println("Keys generated and stored successfully.");
    }

    private static byte[] generateSalt() {
        byte[] salt = new byte[SALT_SIZE];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    private static SecretKey generateAesFromPin(String pin, byte[] salt) throws Exception {
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

    public static PrivateKey decryptPrivateKey(String pin) throws Exception {
        // Read encrypted key and salt from USB
        byte[] fileData = Files.readAllBytes(Paths.get(USB_PATH));

        // Extract salt (first 16 bytes) and encrypted private key
        byte[] salt = Arrays.copyOfRange(fileData, 0, SALT_SIZE);
        byte[] encryptedData = Arrays.copyOfRange(fileData, SALT_SIZE, fileData.length);

        // Derive AES key from PIN and salt
        SecretKey aesKey = generateAesFromPin(pin, salt);

        // Decrypt the private key
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] iv = Arrays.copyOfRange(encryptedData, 0, 16); // Extract IV (first 16 bytes)
        byte[] encryptedKey = Arrays.copyOfRange(encryptedData, 16, encryptedData.length);

        cipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(iv));
        byte[] decryptedKeyBytes = cipher.doFinal(encryptedKey);

        // Convert decrypted bytes to PrivateKey object
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decryptedKeyBytes));
    }

}
