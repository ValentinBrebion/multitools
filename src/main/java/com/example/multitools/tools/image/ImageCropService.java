package com.example.multitools.tools.image;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;

public class ImageCropService {

    public void crop(Path source, Path destination, int x, int y, int width, int height) throws IOException {
        Path normalizedSource = source.toAbsolutePath().normalize();
        Path normalizedDestination = destination.toAbsolutePath().normalize();
        if (normalizedSource.equals(normalizedDestination)) {
            throw new IOException("Le fichier de destination doit être différent du fichier source.");
        }
        if (!Files.isRegularFile(normalizedSource)) {
            throw new IOException("Le fichier image sélectionné est introuvable.");
        }

        String extension = getExtension(normalizedSource);
        String outputFormat = getImageIoFormat(extension);
        if (outputFormat == null || !getExtension(normalizedDestination).equals(extension)) {
            throw new IOException("Le format de destination doit correspondre au format de l'image source.");
        }
        Path parent = normalizedDestination.getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            throw new IOException("Le dossier de destination n'existe pas.");
        }

        BufferedImage image = ImageIO.read(normalizedSource.toFile());
        if (image == null) {
            throw new IOException("Le fichier sélectionné ne contient pas une image prise en charge.");
        }
        if (x < 0 || y < 0 || width < 1 || height < 1
            || (long) x + width > image.getWidth() || (long) y + height > image.getHeight()) {
            throw new IOException("La zone sélectionnée dépasse les limites de l'image.");
        }
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(outputFormat);
        if (!writers.hasNext()) {
            throw new IOException("L'encodeur " + extension.toUpperCase(Locale.ROOT) + " n'est pas disponible.");
        }

        BufferedImage cropped = image.getSubimage(x, y, width, height);
        if (extension.equals("jpg") || extension.equals("jpeg")) {
            BufferedImage flattened = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = flattened.createGraphics();
            try {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, width, height);
                graphics.drawImage(cropped, 0, 0, null);
            } finally {
                graphics.dispose();
            }
            cropped = flattened;
        }
        Path temporaryFile = Files.createTempFile(parent, ".image-crop-", "." + extension);
        ImageWriter writer = writers.next();
        try {
            try (ImageOutputStream output = ImageIO.createImageOutputStream(temporaryFile.toFile())) {
                if (output == null) {
                    throw new IOException("Impossible de créer le fichier image temporaire.");
                }
                writer.setOutput(output);
                writer.write(cropped);
            }
            Files.move(temporaryFile, normalizedDestination, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            writer.dispose();
            Files.deleteIfExists(temporaryFile);
        }
    }

    private String getExtension(Path path) throws IOException {
        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            throw new IOException("Le fichier image doit posséder une extension.");
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String getImageIoFormat(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> "jpeg";
            case "png", "gif", "bmp", "webp" -> extension;
            default -> null;
        };
    }
}
