package com.example.pdfsignergui;

import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

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
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * @brief Utility class for signing and verifying PDF files.
 *
 * The `PdfSignerUtil` class provides static methods to handle the digital signing
 * of PDF documents, decryption of private keys from encrypted files, and the
 * verification of existing digital signatures within PDFs. It leverages the
 * iText library for PDF manipulation and signing, and BouncyCastle for
 * cryptographic operations.
 */
public class PdfSignerUtil {
    /**
     * @brief The size of the salt used in key derivation (in bytes).
     */
    private static final int SALT_SIZE = 16;
    /**
     * @brief The size of the Initialization Vector (IV) used in AES encryption (in bytes).
     */
    private static final int IV_SIZE = 16;
    /**
     * @brief The number of iterations for PBKDF2 key derivation.
     * A higher number increases security but also computation time.
     */
    private static final int PBKDF2_ITERATIONS = 65536;
    /**
     * @brief The size of the AES key in bits (256 bits for AES-256).
     */
    private static final int AES_KEY_SIZE = 256;
    /**
     * @brief The relative path to the certificate file used for signing and verification.
     */
    private static final String CERT_PATH = "../Certs/cert_for_signing.cer";

    /**
     * @brief Digitally signs a PDF document using a private key and a certificate chain.
     *
     * This method takes a source PDF file, decrypts a private key from an encrypted file
     * on a USB drive using a provided PIN, and then applies a digital signature to the PDF.
     * The signed PDF is saved to a specified destination path.
     *
     * @param srcPdfPath The {@link java.io.File} object representing the source, unsigned PDF.
     * @param destPdfPath The {@link java.io.File} object where the signed PDF will be saved.
     * @param usbPath The {@link java.io.File} object pointing to the encrypted private key file on the USB.
     * @param pin The PIN (Personal Identification Number) required to decrypt the private key.
     * @throws Exception If any error occurs during PDF reading/writing, key decryption,
     * certificate loading, or the signing process itself.
     */
    public static void signPdf(File srcPdfPath, File destPdfPath, File usbPath, String pin) throws Exception {
        PrivateKey privateKey;
        try {
            privateKey = decryptPrivateKey(usbPath, pin);
        } catch (Exception e) {
            PdfSignerGui.setStateLabel("Error decrypting private key. Check your PIN");
            return;
        }
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

    /**
     * @brief Decrypts a private key from an encrypted file using a provided PIN.
     *
     * This method reads the encrypted private key, salt, and Initialization Vector (IV)
     * from the specified file. It then derives an AES key from the PIN using PBKDF2
     * and decrypts the private key data. Finally, it converts the decrypted bytes
     * into an RSA `PrivateKey` object.
     *
     * @param encryptedFile The {@link java.io.File} containing the encrypted private key, salt, and IV.
     * @param pin The PIN string used to derive the decryption key.
     * @return The decrypted {@link java.security.PrivateKey} object.
     * @throws Exception If there are issues reading the file, deriving the key,
     * decrypting the data, or converting the key bytes to a `PrivateKey` object.
     */
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

    /**
     * @brief Loads a certificate chain from the pre-defined certificate path (`CERT_PATH`).
     *
     * This method attempts to read an X.509 certificate from the file specified by `CERT_PATH`.
     * This certificate is essential for both signing the PDF (as it contains the public key
     * corresponding to the private key) and for verifying the signature.
     *
     * @return An array of {@link java.security.cert.Certificate} objects, containing the loaded X.509 certificate.
     * @throws FileNotFoundException If the certificate file specified by `CERT_PATH` does not exist.
     * @throws Exception If any other error occurs during the certificate loading process
     * (e.g., invalid certificate format).
     */
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

    /**
     * @brief Verifies the digital signatures present in a PDF document.
     *
     * This method opens the specified PDF, iterates through all its form fields
     * to find signature fields, and then verifies the integrity and authenticity
     * of each signature using iText's `PdfPKCS7` utility. It also compares the
     * public key embedded in the signature with a locally loaded public key
     * to ensure the signature was made by the expected entity.
     *
     * @param pdfPath The {@link java.io.File} object representing the PDF document to be verified.
     * @return `true` if all found signatures are valid and match the locally
     * provided public key; `false` otherwise, or if no signatures are found,
     * or if an error occurs during the verification process.
     */
    public static boolean verifySignature(File pdfPath, PublicKey publicKey) {
        BouncyCastleProvider provider = new BouncyCastleProvider();
        Security.addProvider(provider);
        try {
            PdfDocument pdfDoc = new PdfDocument(new PdfReader(pdfPath));
            PdfAcroForm acroForm = PdfAcroForm.getAcroForm(pdfDoc, false);

            // Check if the PDF has any form fields, if not, no signatures can be present.
            if (acroForm == null || acroForm.getFormFields().isEmpty()) {
                System.out.println("No signatures found in the PDF.");
                PdfSignerGui.setStateLabel("No signatures found in the PDF.");
                pdfDoc.close(); // Close the document to release resources
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
            PdfSignerGui.setStateLabel("Signature is invalid.");
            return false;
        }
    }

    /**
     * @brief Reads a public key from a file.
     * @param publicKeyFilePath The path to the file containing the public key in Base64 format.
     * @return A {@link java.security.PublicKey} object representing the public key.
     * @throws IOException if there is an error reading the file.
     * @throws NoSuchAlgorithmException if the RSA algorithm is not available.
     * @throws InvalidKeySpecException if the key specification is invalid.
     */
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

