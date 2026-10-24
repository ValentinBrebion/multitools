package com.example.multitools.tools.qrcode;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

public final class QRCodeImageExporter {

    private static final int ICO_SIZE = 256;
    private static final int ICO_HEADER_SIZE = 22;

    private QRCodeImageExporter() {
    }

    public static void save(BufferedImage image, String format, File file) throws IOException {
        switch (format) {
            case "JPG (JPEG)" -> saveJpeg(image, file);
            case "ICO" -> saveIco(image, file);
            case "PNG" -> writeImage(image, "png", file);
            default -> throw new IllegalArgumentException("Le format d'image sélectionné n'est pas pris en charge.");
        }
    }

    private static void saveJpeg(BufferedImage source, File file) throws IOException {
        BufferedImage image = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        writeImage(image, "jpg", file);
    }

    private static void saveIco(BufferedImage source, File file) throws IOException {
        BufferedImage icon = new BufferedImage(ICO_SIZE, ICO_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = icon.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, ICO_SIZE, ICO_SIZE, null);
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream pngBuffer = new ByteArrayOutputStream();
        if (!ImageIO.write(icon, "png", pngBuffer)) {
            throw new IOException("Aucun encodeur PNG n'est disponible pour créer l'icône.");
        }
        byte[] png = pngBuffer.toByteArray();
        try (DataOutputStream output = new DataOutputStream(new FileOutputStream(file))) {
            writeLittleEndianShort(output, 0);
            writeLittleEndianShort(output, 1);
            writeLittleEndianShort(output, 1);
            output.writeByte(0);
            output.writeByte(0);
            output.writeByte(0);
            output.writeByte(0);
            writeLittleEndianShort(output, 1);
            writeLittleEndianShort(output, 32);
            writeLittleEndianInt(output, png.length);
            writeLittleEndianInt(output, ICO_HEADER_SIZE);
            output.write(png);
        }
    }

    private static void writeImage(BufferedImage image, String format, File file) throws IOException {
        if (!ImageIO.write(image, format, file)) {
            throw new IOException("Aucun encodeur " + format.toUpperCase(Locale.ROOT) + " n'est disponible.");
        }
    }

    private static void writeLittleEndianShort(DataOutputStream output, int value) throws IOException {
        output.writeByte(value & 0xFF);
        output.writeByte((value >>> 8) & 0xFF);
    }

    private static void writeLittleEndianInt(DataOutputStream output, int value) throws IOException {
        output.writeByte(value & 0xFF);
        output.writeByte((value >>> 8) & 0xFF);
        output.writeByte((value >>> 16) & 0xFF);
        output.writeByte((value >>> 24) & 0xFF);
    }
}
