package com.example.multitools.tools.pdf;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.Base64;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

public class PdfProtectionService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public void protect(Path inputFile, Path outputFile, String currentPassword, String password)
            throws IOException {
        if (inputFile == null || !Files.isRegularFile(inputFile) || !Files.isReadable(inputFile)) {
            throw new IOException("Le fichier PDF source est introuvable ou illisible.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Saisissez un mot de passe.");
        }
        if (outputFile == null || outputFile.getFileName() == null
                || !outputFile.getFileName().toString().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Choisissez un fichier de destination avec l'extension .pdf.");
        }

        Path input = inputFile.toAbsolutePath().normalize();
        Path output = outputFile.toAbsolutePath().normalize();
        if (input.equals(output)) {
            throw new IllegalArgumentException("Le fichier de destination doit être différent du fichier source.");
        }

        Path temporaryOutput = Files.createTempFile(output.getParent(), "pdf-protect-", ".pdf");
        try {
            try (PDDocument document = Loader.loadPDF(input.toFile(),
                    currentPassword == null ? "" : currentPassword)) {
                AccessPermission permissions = new AccessPermission();
                StandardProtectionPolicy policy = new StandardProtectionPolicy(
                        createOwnerPassword(), password, permissions);
                policy.setEncryptionKeyLength(256);
                document.protect(policy);
                document.save(temporaryOutput.toFile());
            }

            try {
                Files.move(temporaryOutput, output,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporaryOutput, output, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryOutput);
        }
    }

    private String createOwnerPassword() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
