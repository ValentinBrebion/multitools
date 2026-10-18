package com.example.multitools.tools.image;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;

public class ImageConversionService {

    public void convert(Path source, Path destination, ImageOutputFormat format) throws IOException {
        Path normalizedSource = source.toAbsolutePath().normalize();
        Path normalizedDestination = destination.toAbsolutePath().normalize();
        validatePaths(normalizedSource, normalizedDestination, format);

        BufferedImage image = ImageIO.read(normalizedSource.toFile());
        if (image == null) {
            throw new IOException("Le fichier sélectionné ne contient pas une image prise en charge.");
        }
        if (format == ImageOutputFormat.ICO) {
            image = scaleForIcon(image);
        } else if (format == ImageOutputFormat.JPG) {
            image = flattenTransparency(image);
        }

        Path temporaryFile = Files.createTempFile(
            normalizedDestination.getParent(),
            ".image-conversion-",
            "." + format.getExtension()
        );
        try {
            if (format == ImageOutputFormat.ICO) {
                writeIcon(image, temporaryFile);
            } else {
                writeImage(image, temporaryFile, format);
            }
            Files.move(temporaryFile, normalizedDestination, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private void validatePaths(Path source, Path destination, ImageOutputFormat format) throws IOException {
        if (format == null) {
            throw new IOException("Sélectionnez un format de sortie.");
        }
        if (source.equals(destination)) {
            throw new IOException("Le fichier de destination doit être différent du fichier source.");
        }
        if (!Files.isRegularFile(source)) {
            throw new IOException("Le fichier image sélectionné est introuvable.");
        }
        if (!hasExtension(destination, "." + format.getExtension())) {
            throw new IOException("Le fichier de destination doit avoir l'extension ." + format.getExtension() + ".");
        }

        Path parent = destination.getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            throw new IOException("Le dossier de destination n'existe pas.");
        }
    }

    private void writeImage(BufferedImage image, Path destination, ImageOutputFormat format) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format.getImageIoFormat());
        if (!writers.hasNext()) {
            throw new IOException("L'encodeur " + format.getLabel() + " n'est pas disponible.");
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(destination.toFile())) {
            if (output == null) {
                throw new IOException("Impossible de créer le fichier image de destination.");
            }
            writer.setOutput(output);
            writer.write(image);
        } finally {
            writer.dispose();
        }
    }

    private void writeIcon(BufferedImage image, Path destination) throws IOException {
        ByteArrayOutputStream pngBuffer = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", pngBuffer)) {
            throw new IOException("Impossible de préparer l'image PNG pour le fichier ICO.");
        }
        byte[] pngData = pngBuffer.toByteArray();
        try (DataOutputStream output = new DataOutputStream(Files.newOutputStream(destination))) {
            writeLittleEndianShort(output, 0);
            writeLittleEndianShort(output, 1);
            writeLittleEndianShort(output, 1);
            output.writeByte(image.getWidth() == 256 ? 0 : image.getWidth());
            output.writeByte(image.getHeight() == 256 ? 0 : image.getHeight());
            output.writeByte(0);
            output.writeByte(0);
            writeLittleEndianShort(output, 1);
            writeLittleEndianShort(output, 32);
            writeLittleEndianInt(output, pngData.length);
            writeLittleEndianInt(output, 22);
            output.write(pngData);
        }
    }

    private void writeLittleEndianShort(DataOutputStream output, int value) throws IOException {
        output.writeByte(value & 0xff);
        output.writeByte((value >>> 8) & 0xff);
    }

    private void writeLittleEndianInt(DataOutputStream output, int value) throws IOException {
        output.writeByte(value & 0xff);
        output.writeByte((value >>> 8) & 0xff);
        output.writeByte((value >>> 16) & 0xff);
        output.writeByte((value >>> 24) & 0xff);
    }

    private BufferedImage scaleForIcon(BufferedImage image) {
        int longestSide = Math.max(image.getWidth(), image.getHeight());
        if (longestSide <= 256) {
            return image;
        }

        double scale = 256.0 / longestSide;
        int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );
            graphics.drawImage(image, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return scaled;
    }

    private BufferedImage flattenTransparency(BufferedImage image) {
        BufferedImage flattened = new BufferedImage(
            image.getWidth(),
            image.getHeight(),
            BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = flattened.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, flattened.getWidth(), flattened.getHeight());
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return flattened;
    }

    private boolean hasExtension(Path path, String extension) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(extension);
    }
}
