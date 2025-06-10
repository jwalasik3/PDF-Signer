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
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Calendar;
import java.util.Collections;

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
        PrivateKey privateKey = decryptPrivateKey(usbPath, pin);
        Certificate[] chain = loadCertificateChain();

        System.out.println("Signing...");
        PdfSigner signer = new PdfSigner(new PdfReader(srcPdfPath), new FileOutputStream(destPdfPath), new StampingProperties());

        PdfSignatureAppearance appearance = signer.getSignatureAppearance();
        appearance
                .setReason("Proj") // Sets the reason for signing.
                .setLocation("localhost") // Sets the location where the document was signed.
                .setReuseAppearance(false) // Ensures a new appearance is generated for the signature field.
                .setPageNumber(1); // Specifies the page number where the signature will appear.
        signer.setFieldName("sig"); // Sets the name of the signature form field.

        BouncyCastleProvider provider = new BouncyCastleProvider();
        Security.addProvider(provider); // Adds the BouncyCastle security provider.

        IExternalSignature signature = new PrivateKeySignature(privateKey, DigestAlgorithms.SHA256, provider.getName());
        IExternalDigest digest = new BouncyCastleDigest();

        // Signs the PDF document detached (signature data is external to the signed data).
        signer.signDetached(digest, signature, chain, null, null, null, 0, PdfSigner.CryptoStandard.CADES);
        System.out.println("Signed.");
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

        // Extract salt and IV from the beginning of the file bytes
        byte[] salt = new byte[SALT_SIZE];
        byte[] iv = new byte[IV_SIZE];
        byte[] encryptedKeyBytes = new byte[fileBytes.length - SALT_SIZE - IV_SIZE];

        System.arraycopy(fileBytes, 0, salt, 0, SALT_SIZE);
        System.arraycopy(fileBytes, SALT_SIZE, iv, 0, IV_SIZE);
        System.arraycopy(fileBytes, SALT_SIZE + IV_SIZE, encryptedKeyBytes, 0, encryptedKeyBytes.length);

        // Derive AES key from PIN using PBKDF2WithHmacSHA256
        PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_SIZE);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        SecretKey tmpKey = factory.generateSecret(spec);
        SecretKey aesKey = new SecretKeySpec(tmpKey.getEncoded(), "AES");

        // Decrypt the key using AES/CBC/PKCS5Padding
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(iv));
        byte[] decryptedKeyBytes = cipher.doFinal(encryptedKeyBytes);

        // Convert the decrypted bytes into an RSA PrivateKey object
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
    public static boolean verifySignature(File pdfPath) {
        BouncyCastleProvider provider = new BouncyCastleProvider();
        Security.addProvider(provider); // Ensures BouncyCastle is available for cryptographic operations.
        try {
            PdfDocument pdfDoc = new PdfDocument(new PdfReader(pdfPath));
            PdfAcroForm acroForm = PdfAcroForm.getAcroForm(pdfDoc, false);

            // Check if the PDF has any form fields, if not, no signatures can be present.
            if (acroForm == null || acroForm.getFormFields().isEmpty()) {
                System.out.println("No signatures found in the PDF.");
                pdfDoc.close(); // Close the document to release resources
                return false;
            }

            // Load the expected signing certificate for public key comparison.
            Certificate[] chain = loadCertificateChain();
            X509Certificate signingCert = (X509Certificate) chain[0];

            SignatureUtil signatureUtil = new SignatureUtil(pdfDoc);
            // Iterate through all form fields to find signature fields.
            for (String name : acroForm.getFormFields().keySet()) {
                PdfFormField field = acroForm.getField(name);
                // Skip fields that are not signature fields.
                if (!PdfName.Sig.equals(field.getFormType())) {
                    continue;
                }

                // Read signature data and perform integrity and authenticity checks.
                PdfPKCS7 pkcs7 = signatureUtil.readSignatureData(name);
                if (pkcs7.verifySignatureIntegrityAndAuthenticity()) {
                    System.out.println("Signature " + name + " is valid.");
                    // Compare the public key from the signature's certificate with the expected public key.
                    if (pkcs7.getSigningCertificate().getPublicKey().equals(signingCert.getPublicKey())) {
                        System.out.println("Signature matches the public key.");
                    } else {
                        System.out.println("Signature does not match the public key.");
                        pdfDoc.close(); // Close the document
                        return false;
                    }
                } else {
                    System.out.println("Signature " + name + " is invalid.");
                    pdfDoc.close(); // Close the document
                    return false;
                }
            }
            pdfDoc.close(); // Close the document after successful verification
            return true; // All signatures verified successfully.
        } catch (Exception e) {
            // Log any errors that occur during the verification process.
            System.out.println("Error during PDF signature verification: " + e.getMessage());
            return false;
        }
    }
}