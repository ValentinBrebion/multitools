package com.example.multitools.tools.pdf;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;

public class PdfStampService {

    public void stamp(Path inputFile, Path outputFile, String currentPassword, String stampText)
            throws IOException {
        if (inputFile == null || !Files.isRegularFile(inputFile) || !Files.isReadable(inputFile)) {
            throw new IOException("Le fichier PDF source est introuvable ou illisible.");
        }
        if (!"CONFIDENTIEL".equals(stampText) && !"BROUILLON".equals(stampText)) {
            throw new IllegalArgumentException("Choisissez le tampon CONFIDENTIEL ou BROUILLON.");
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

        Path temporaryOutput = Files.createTempFile(output.getParent(), "pdf-stamp-", ".pdf");
        try {
            try (PDDocument document = Loader.loadPDF(input.toFile(),
                    currentPassword == null ? "" : currentPassword)) {
                PDType1Font font = new PDType1Font(FontName.HELVETICA_BOLD);
                PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
                graphicsState.setNonStrokingAlphaConstant(0.22f);

                for (PDPage page : document.getPages()) {
                    applyStamp(document, page, font, graphicsState, stampText);
                }
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

    private void applyStamp(PDDocument document, PDPage page, PDType1Font font,
                            PDExtendedGraphicsState graphicsState, String text) throws IOException {
        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();
        float textWidthAtDefaultSize = font.getStringWidth(text) / 1000f;
        float fontSize = Math.min(72f, Math.min(pageWidth * 0.72f / textWidthAtDefaultSize,
                pageHeight * 0.16f));
        float textWidth = textWidthAtDefaultSize * fontSize;

        try (PDPageContentStream content = new PDPageContentStream(
                document, page, AppendMode.APPEND, true, true)) {
            content.saveGraphicsState();
            content.setGraphicsStateParameters(graphicsState);
            content.setNonStrokingColor(0.8f, 0.08f, 0.08f);
            content.transform(Matrix.getTranslateInstance(pageWidth / 2f, pageHeight / 2f));
            content.transform(Matrix.getRotateInstance(Math.toRadians(45), 0, 0));
            content.beginText();
            content.setFont(font, fontSize);
            content.newLineAtOffset(-textWidth / 2f, -fontSize * 0.35f);
            content.showText(text);
            content.endText();
            content.restoreGraphicsState();
        }
    }
}
