package com.example.pdfsignerkeygen;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * @brief Utility class for generating RSA key pairs and encrypting private keys.
 *
 * This class provides functionality to generate an RSA 4096-bit key pair,
 * encrypt the private key using AES encryption with a PIN-derived key (PBKDF2),
 * and save the encrypted private key and the public key (along with a self-signed
 * X.509 certificate) to specified file paths.
 */
public class RSAKeyGenAndEncrypt {

    /**
     * @brief The size of the RSA key in bits.
     */
    private static final int RSA_KEY_SIZE = 4096;
    /**
     * @brief The size of the AES key in bits.
     */
    private static final int AES_KEY_SIZE = 256;
    /**
     * @brief The number of iterations for PBKDF2 key derivation.
     * A higher number increases the computational cost for brute-force attacks.
     */
    private static final int PBKDF2_ITERATIONS = 65536;
    /**
     * @brief The size of the cryptographic salt in bytes.
     */
    private static final int SALT_SIZE = 16;

    /**
     * @brief Static initializer block to add BouncyCastle security provider.
     * This ensures that BouncyCastle cryptographic algorithms are available
     * throughout the application's lifecycle.
     */
    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    /**
     * @brief Generates an RSA key pair, encrypts the private key, and saves keys and a self-signed certificate to files.
     *
     * This is the main entry point for the key generation and encryption process.
     * It performs the following steps:
     * 1. Generates a 4096-bit RSA key pair.
     * 2. Generates a self-signed X.509 certificate using the generated key pair.
     * 3. Derives an AES encryption key from the provided PIN using PBKDF2 with a random salt.
     * 4. Encrypts the RSA private key using the derived AES key and a random IV.
     * 5. Saves the encrypted private key (along with the salt) to the specified USB path.
     * 6. Saves the RSA public key (Base64 encoded) to the specified public key path.
     * 7. Creates necessary parent directories if they don't exist and attempts to set the private key file as hidden.
     *
     * @param pin The PIN (Personal Identification Number) used to derive the AES key for private key encryption.
     * @param usbPath The path to the directory on the USB drive where the encrypted private key file will be saved.
     * The file name will be `private_key.enc` within this path.
     * @param publicKeyPath The path to the directory where the public key file will be saved.
     * The file name will be `public_key.pem` within this path.
     * @throws Exception If any error occurs during key generation, encryption, certificate generation, or file operations.
     */
    public static void encode(String pin, String usbPath, String publicKeyPath) throws Exception {

        // Generating RSA 4096-bit key pair
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(RSA_KEY_SIZE);
        KeyPair keyPair = keyPairGen.generateKeyPair();
        generateSelfSignedCert(keyPair); // Generate and save a self-signed certificate

        // Deriving AES key from PIN with salt to prevent rainbow table attacks
        byte[] salt = generateSalt();
        SecretKey aesKey = deriveAESKeyFromPIN(pin, salt);

        byte[] encryptedPrivateKey = encryptPrivateKey(keyPair.getPrivate(), aesKey);

        File usbFile = new File(usbPath, ".keys\\private_key.enc"); // Specific file name for private key
        File parent = usbFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();  // Creates parent directory if it doesn't exist
            // Attempt to set the file as hidden on DOS-like file systems (Windows)
            try {
                Files.setAttribute(Path.of(usbPath), "dos:hidden", true);
            } catch (UnsupportedOperationException e) {
                // Ignore if not supported on the current OS/file system
                System.out.println("Warning: 'dos:hidden' attribute not supported on this file system.");
            }
        }

        File pubFile = new File(publicKeyPath, "public_key.pem"); // Specific file name for public key
        System.out.println(pubFile); // Debugging output
        File parentDir = pubFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs(); // Create the directories if they don't exist
        }

        // Saving the encrypted private key (with salt) to USB pendrive and public key to specified filepath
        saveToFile(String.valueOf(usbFile), salt, encryptedPrivateKey);
        savePublicKey(String.valueOf(pubFile), keyPair.getPublic());

        System.out.println("Keys generated and stored successfully.");
    }

    /**
     * @brief Generates a cryptographically strong random salt.
     *
     * The salt is used in the PBKDF2 key derivation process to prevent
     * rainbow table attacks and ensure that identical PINs produce different
     * derived keys.
     *
     * @return A byte array containing the randomly generated salt.
     */
    private static byte[] generateSalt() {
        byte[] salt = new byte[SALT_SIZE];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    /**
     * @brief Derives an AES `SecretKey` from a PIN and a salt using PBKDF2.
     *
     * This method uses PBKDF2 with HmacSHA256 as the pseudo-random function
     * to stretch the provided PIN into a strong cryptographic key. The number
     * of iterations (`PBKDF2_ITERATIONS`) makes the key derivation computationally
     * expensive, hindering brute-force attacks on the PIN.
     *
     * @param pin The user's PIN as a String.
     * @param salt The salt bytes used in the key derivation process.
     * @return A {@link javax.crypto.SecretKey} object suitable for AES encryption.
     * @throws Exception If there is an issue with the cryptographic algorithms or factories.
     */
    private static SecretKey deriveAESKeyFromPIN(String pin, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_SIZE);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    /**
     * @brief Encrypts a private key using AES encryption.
     *
     * This method takes a {@link java.security.PrivateKey} and an AES `SecretKey`
     * to encrypt the private key's encoded bytes. It uses AES in CBC (Cipher Block Chaining)
     * mode with PKCS5Padding. A random Initialization Vector (IV) is generated for each encryption
     * and prepended to the encrypted data to ensure unique ciphertexts for identical plaintexts.
     *
     * @param privateKey The {@link java.security.PrivateKey} object to be encrypted.
     * @param aesKey The {@link javax.crypto.SecretKey} (AES key) used for encryption.
     * @return A byte array containing the IV followed by the encrypted private key data.
     * @throws Exception If there are issues with the cryptographic algorithms, padding,
     * or the encryption process.
     */
    private static byte[] encryptPrivateKey(PrivateKey privateKey, SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] iv = new byte[16]; // AES block size is 16 bytes for IV
        new SecureRandom().nextBytes(iv); // Generate a random IV for each encryption
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, new IvParameterSpec(iv));

        byte[] encryptedData = cipher.doFinal(privateKey.getEncoded());
        byte[] result = new byte[iv.length + encryptedData.length];
        // Concatenate IV and encrypted data
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encryptedData, 0, result, iv.length, encryptedData.length);
        return result;
    }

    /**
     * @brief Saves the salt and encrypted private key data to a file.
     *
     * The salt is written first, followed by the encrypted private key data.
     * This allows the decryption process to correctly extract the salt
     * before deriving the AES key.
     *
     * @param path The full file path where the data should be saved.
     * @param salt The byte array containing the salt.
     * @param data The byte array containing the encrypted private key data (including IV).
     * @throws IOException If an I/O error occurs during file writing.
     */
    private static void saveToFile(String path, byte[] salt, byte[] data) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(salt); // Write salt first
            fos.write(data); // Then write the IV + encrypted key data
        }
    }

    /**
     * @brief Saves the public key to a file in Base64 encoded format.
     *
     * The public key's encoded form (typically X.509 SubjectPublicKeyInfo)
     * is Base64 encoded and then written directly to the specified file.
     *
     * @param path The full file path where the public key should be saved.
     * @param publicKey The {@link java.security.PublicKey} object to be saved.
     * @throws IOException If an I/O error occurs during file writing.
     */
    private static void savePublicKey(String path, PublicKey publicKey) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(Base64.getEncoder().encode(publicKey.getEncoded()));
        }
    }

    /**
     * @brief Generates a self-signed X.509 certificate for the given key pair.
     *
     * This method creates an X.509 version 3 certificate where the issuer and
     * subject are the same. It sets basic constraints (not a CA), key usage
     * for digital signatures, and includes Subject Key Identifier and Authority Key Identifier extensions.
     * The certificate is signed using the provided private key and saved to a file.
     *
     * @param keyPair The {@link java.security.KeyPair} (containing both public and private keys)
     * for which the self-signed certificate will be generated.
     * @throws NoSuchAlgorithmException If the specified signature algorithm is not available.
     * @throws OperatorCreationException If there's an issue creating the content signer.
     * @throws CertificateException If there's an issue with certificate creation or conversion.
     * @throws CertIOException If there's an I/O error during certificate extension processing.
     * @throws IOException If an I/O error occurs during saving the certificate to a file.
     */
    private static void generateSelfSignedCert(KeyPair keyPair) throws Exception {
        try {
            String subjectDN = "CN=PAdES Emulation Cert, OU=BSK_Proj, O=193382&193650, L=Gdansk, ST=Pomeranian, C=PL";
            X500Name issuerName = new X500Name(subjectDN);
            X500Name subjectName = new X500Name(subjectDN);

            // Generating a random serial number for the certificate
            BigInteger serialNumber = BigInteger.valueOf(new Random().nextLong() & Long.MAX_VALUE);

            // Setting validity period for 1 year
            Calendar calendar = Calendar.getInstance();
            Date notBefore = calendar.getTime();
            calendar.add(Calendar.YEAR, 1); // Valid for one year from now
            Date notAfter = calendar.getTime();

            String signatureAlgorithm = "SHA256WithRSAEncryption";

            // Creating the X.509v3 certificate builder
            X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    issuerName, // Issuer is the same as subject for self-signed
                    serialNumber,
                    notBefore,
                    notAfter,
                    subjectName,
                    keyPair.getPublic() // Public key to be certified
            );

            JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();

            // Setting basic constraints: not a Certificate Authority (CA)
            boolean isCA = false;
            certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(isCA));

            // Setting key usage: digitalSignature for signing purposes
            certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));

            // Adding Subject Key Identifier and Authority Key Identifier extensions
            certBuilder.addExtension(Extension.subjectKeyIdentifier, false,
                    extUtils.createSubjectKeyIdentifier(keyPair.getPublic()));
            certBuilder.addExtension(Extension.authorityKeyIdentifier, false,
                    extUtils.createAuthorityKeyIdentifier(keyPair.getPublic()));

            // Building the content signer using the private key
            ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm)
                    .setProvider("BC") // Specify BouncyCastle provider
                    .build(keyPair.getPrivate());

            // Converting the builder to an X509Certificate object
            X509Certificate certificate = new JcaX509CertificateConverter()
                    .setProvider("BC") // Specify BouncyCastle provider
                    .getCertificate(certBuilder.build(contentSigner));

            System.out.println("\nSelf-Signed X.509 Certificate Generated:");
            System.out.println("Subject DN: " + certificate.getSubjectX500Principal().getName());
            System.out.println("Issuer DN: " + certificate.getIssuerX500Principal().getName());
            System.out.println("Serial Number: " + certificate.getSerialNumber());
            System.out.println("Valid From: " + certificate.getNotBefore());
            System.out.println("Valid To: " + certificate.getNotAfter());
            System.out.println("Signature Algorithm: " + certificate.getSigAlgName());
            System.out.println("Public Key: " + certificate.getPublicKey());

            // Saving the generated certificate to a file
            String certPath = "../Certs"; // Relative path to the Certs directory
            File certFile = new File(certPath, "cert_for_signing.cer"); // File name for the certificate
            File parentDir = certFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs(); // Create the directory if it doesn't exist
            }
            try (FileOutputStream fos = new FileOutputStream(certFile)) {
                fos.write(certificate.getEncoded()); // Write the encoded certificate bytes
                System.out.println("\nCert saved to: " + certFile);
            }

        } catch (NoSuchAlgorithmException | OperatorCreationException | CertificateException | CertIOException e) {
            System.err.println("Error creating a cert: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error writing cert to file: " + e.getMessage());
        }
    }
}