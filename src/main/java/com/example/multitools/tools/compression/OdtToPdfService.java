package com.example.multitools.tools.compression;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public final class OdtToPdfService {

    private static final float MARGIN = 50;
    private static final float FONT_SIZE = 11;
    private static final float LINE_HEIGHT = 15;
    private static final float PARAGRAPH_SPACING = 7;

    private OdtToPdfService() {
    }

    public static void convert(Path input, Path output) throws IOException {
        if (input == null || !Files.isRegularFile(input) || !Files.isReadable(input)
                || !input.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".odt")) {
            throw new IOException("Le fichier source doit être un document ODT existant et lisible.");
        }
        if (output == null || output.getFileName() == null
                || !output.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IOException("Le fichier de destination doit utiliser l'extension .pdf.");
        }

        Path source = input.toAbsolutePath().normalize();
        Path destination = output.toAbsolutePath().normalize();
        if (source.equals(destination)) {
            throw new IOException("Le fichier PDF de destination ne peut pas remplacer le fichier ODT source.");
        }
        Path parent = destination.getParent();
        if (parent == null || !Files.isDirectory(parent) || !Files.isWritable(parent)) {
            throw new IOException("Le dossier de destination est introuvable ou non accessible en écriture.");
        }

        List<String> paragraphs = extractParagraphs(source);
        Path temporaryOutput = Files.createTempFile(parent, ".odt-to-pdf-", ".pdf");
        try {
            renderPdf(paragraphs, temporaryOutput);
            try {
                Files.move(temporaryOutput, destination,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryOutput, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryOutput);
        }
    }

    private static List<String> extractParagraphs(Path input) throws IOException {
        try (ZipFile odt = new ZipFile(input.toFile())) {
            ZipEntry content = odt.getEntry("content.xml");
            if (content == null || content.getSize() > 50L * 1024 * 1024) {
                throw new IOException("Le document ODT est invalide ou son contenu est trop volumineux.");
            }
            try (InputStream contentStream = odt.getInputStream(content)) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                factory.setXIncludeAware(false);
                factory.setExpandEntityReferences(false);
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

                Node document = factory.newDocumentBuilder().parse(contentStream);
                List<String> paragraphs = new ArrayList<>();
                extractBlocks(document, paragraphs);
                if (paragraphs.stream().noneMatch(paragraph -> !paragraph.isBlank())) {
                    throw new IOException("Le document ODT ne contient aucun texte à convertir.");
                }
                return paragraphs;
            } catch (Exception exception) {
                if (exception instanceof IOException ioException) {
                    throw ioException;
                }
                throw new IOException("Impossible de lire le contenu du document ODT.", exception);
            }
        }
    }

    private static void extractBlocks(Node node, List<String> paragraphs) {
        Node child = node.getFirstChild();
        while (child != null) {
            if (child instanceof Element element && isTextBlock(element)) {
                paragraphs.add(readText(element).replace('\n', ' ').strip());
            } else if (child instanceof Element element && isTableRow(element)) {
                List<String> cells = new ArrayList<>();
                collectTableCells(element, cells);
                paragraphs.add(String.join(" | ", cells));
            } else {
                extractBlocks(child, paragraphs);
            }
            child = child.getNextSibling();
        }
    }

    private static void collectTableCells(Node row, List<String> cells) {
        Node child = row.getFirstChild();
        while (child != null) {
            if (child instanceof Element element && "table-cell".equals(element.getLocalName())) {
                List<String> cellParagraphs = new ArrayList<>();
                extractBlocks(element, cellParagraphs);
                cells.add(String.join(" ", cellParagraphs).strip());
            } else if (child instanceof Element element
                    && "covered-table-cell".equals(element.getLocalName())) {
                cells.add("");
            } else {
                collectTableCells(child, cells);
            }
            child = child.getNextSibling();
        }
    }

    private static boolean isTextBlock(Element element) {
        String localName = element.getLocalName();
        return "p".equals(localName) || "h".equals(localName);
    }

    private static boolean isTableRow(Element element) {
        return "table-row".equals(element.getLocalName());
    }

    private static String readText(Node node) {
        StringBuilder text = new StringBuilder();
        Node child = node.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.TEXT_NODE) {
                text.append(child.getNodeValue());
            } else if (child instanceof Element element) {
                String localName = element.getLocalName();
                if ("tab".equals(localName)) {
                    text.append("    ");
                } else if ("line-break".equals(localName)) {
                    text.append('\n');
                } else if ("s".equals(localName)) {
                    String count = element.getAttributeNS(
                            "urn:oasis:names:tc:opendocument:xmlns:text:1.0", "c");
                    int spaces = count.isEmpty() ? 1 : Math.min(Integer.parseInt(count), 100);
                    text.append(" ".repeat(spaces));
                } else {
                    text.append(readText(element));
                }
            }
            child = child.getNextSibling();
        }
        return text.toString();
    }

    private static void renderPdf(List<String> paragraphs, Path output) throws IOException {
        PDType1Font font = new PDType1Font(FontName.HELVETICA);
        float contentWidth = PDRectangle.LETTER.getWidth() - 2 * MARGIN;
        try (PDDocument document = new PDDocument()) {
            PageWriter page = new PageWriter(document, font);
            for (String paragraph : paragraphs) {
                if (paragraph.isBlank()) {
                    page.addBlankLine();
                    continue;
                }
                String normalized = paragraph.replace('\t', ' ').replaceAll("\\s+", " ").strip();
                validateFontSupport(font, normalized);
                page.addParagraph(wrap(normalized, font, contentWidth));
            }
            page.finish();
            document.save(output.toFile());
        }
    }

    private static void validateFontSupport(PDType1Font font, String text) throws IOException {
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            String character = new String(Character.toChars(codePoint));
            try {
                font.encode(character);
            } catch (IllegalArgumentException exception) {
                throw new IOException("Le caractère U+" + String.format("%04X", codePoint)
                        + " n'est pas pris en charge par le rendu PDF simplifié.", exception);
            }
            offset += Character.charCount(codePoint);
        }
    }

    private static List<String> wrap(String paragraph, PDType1Font font, float maxWidth)
            throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : paragraph.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.getStringWidth(candidate) * FONT_SIZE / 1000 <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
            } else if (!line.isEmpty()) {
                lines.add(line.toString());
                line.setLength(0);
                appendLongWord(word, font, maxWidth, lines, line);
            } else {
                appendLongWord(word, font, maxWidth, lines, line);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private static void appendLongWord(String word, PDType1Font font, float maxWidth,
                                       List<String> lines, StringBuilder line) throws IOException {
        for (int offset = 0; offset < word.length();) {
            int codePoint = word.codePointAt(offset);
            String character = new String(Character.toChars(codePoint));
            String candidate = line + character;
            if (!line.isEmpty() && font.getStringWidth(candidate) * FONT_SIZE / 1000 > maxWidth) {
                lines.add(line.toString());
                line.setLength(0);
            }
            line.append(character);
            offset += Character.charCount(codePoint);
        }
    }

    private static final class PageWriter {
        private final PDDocument document;
        private final PDType1Font font;
        private PDPage page;
        private PDPageContentStream content;
        private float y;

        private PageWriter(PDDocument document, PDType1Font font) throws IOException {
            this.document = document;
            this.font = font;
            newPage();
        }

        private void addParagraph(List<String> lines) throws IOException {
            for (String line : lines) {
                ensureSpace(LINE_HEIGHT);
                writeLine(line);
            }
            addBlankLine();
        }

        private void addBlankLine() throws IOException {
            ensureSpace(PARAGRAPH_SPACING);
            y -= PARAGRAPH_SPACING;
        }

        private void ensureSpace(float required) throws IOException {
            if (y - required < MARGIN) {
                content.close();
                newPage();
            }
        }

        private void writeLine(String line) throws IOException {
            content.beginText();
            content.setFont(font, FONT_SIZE);
            content.newLineAtOffset(MARGIN, y);
            content.showText(line);
            content.endText();
            y -= LINE_HEIGHT;
        }

        private void newPage() throws IOException {
            page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        private void finish() throws IOException {
            content.close();
        }
    }
}
