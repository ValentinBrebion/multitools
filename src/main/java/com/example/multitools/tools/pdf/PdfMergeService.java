package com.example.multitools.tools.pdf;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;

public class PdfMergeService {

    public void merge(List<Path> inputFiles, Path outputFile) throws IOException {
        if (inputFiles == null || inputFiles.size() < 2) {
            throw new IllegalArgumentException("Sélectionnez au moins deux fichiers PDF.");
        }
        if (outputFile == null || !outputFile.getFileName().toString().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Choisissez un fichier de destination avec l'extension .pdf.");
        }

        Path output = outputFile.toAbsolutePath().normalize();
        for (Path inputFile : inputFiles) {
            if (inputFile.toAbsolutePath().normalize().equals(output)) {
                throw new IllegalArgumentException(
                        "Le fichier de destination ne peut pas être l'un des fichiers source.");
            }
            if (!Files.isRegularFile(inputFile) || !Files.isReadable(inputFile)) {
                throw new IOException("Fichier PDF introuvable ou illisible : " + inputFile);
            }
        }

        Path parent = output.getParent();
        Path temporaryOutput = Files.createTempFile(parent, "pdf-merge-", ".pdf");
        try {
            PDFMergerUtility merger = new PDFMergerUtility();
            for (Path inputFile : inputFiles) {
                merger.addSource(inputFile.toFile());
            }
            merger.setDestinationFileName(temporaryOutput.toString());
            merger.mergeDocuments(IOUtils.createMemoryOnlyStreamCache());

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
}
