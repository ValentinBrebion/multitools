package com.example.multitools.tools.qrcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Map;

public class QRCodeGenerator {

    public enum ModuleShape {
        SQUARE("Carrés classiques"),
        DOT("Points ronds"),
        ROUNDED("Carrés aux coins arrondis"),
        DIAMOND("Losanges");

        private final String label;

        ModuleShape(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public BitMatrix generate(String content, int size, ErrorCorrectionLevel errorCorrectionLevel) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Le contenu ne peut pas être vide.");
        }
        if (size < 128 || size > 1024) {
            throw new IllegalArgumentException("La taille doit être comprise entre 128 et 1024 pixels.");
        }
        if (errorCorrectionLevel == null) {
            throw new IllegalArgumentException("Le niveau de correction doit être sélectionné.");
        }

        Map<EncodeHintType, Object> hints = Map.of(
            EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel,
            EncodeHintType.MARGIN, 4
        );
        try {
            return new MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            );
        } catch (WriterException e) {
            throw new IllegalArgumentException("Le contenu est trop long pour être encodé en QR code.", e);
        }
    }

    public BufferedImage toImage(BitMatrix matrix) {
        return toImage(matrix, Color.BLACK, Color.WHITE, Color.BLACK, false,
            ModuleShape.SQUARE, null, 18, 3, false);
    }

    public BufferedImage toImage(
        BitMatrix matrix,
        Color foreground,
        Color background,
        Color gradientEnd,
        boolean gradient,
        ModuleShape shape,
        BufferedImage logo,
        int logoSizePercent,
        int logoMarginPercent,
        boolean removeLogoBackground
    ) {
        BufferedImage image = new BufferedImage(
            matrix.getWidth(),
            matrix.getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(background);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int minX = matrix.getWidth();
            int minY = matrix.getHeight();
            int maxX = -1;
            int maxY = -1;
            for (int y = 0; y < matrix.getHeight(); y++) {
                for (int x = 0; x < matrix.getWidth(); x++) {
                    if (matrix.get(x, y)) {
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                    }
                }
            }

            int moduleSize = findModuleSize(matrix, minX, minY);
            int moduleCount = Math.max(1, (maxX - minX + 1) / moduleSize);
            for (int row = 0; row < moduleCount; row++) {
                for (int column = 0; column < moduleCount; column++) {
                    int x = minX + column * moduleSize;
                    int y = minY + row * moduleSize;
                    if (!matrix.get(Math.min(x + moduleSize / 2, matrix.getWidth() - 1),
                        Math.min(y + moduleSize / 2, matrix.getHeight() - 1))) {
                        continue;
                    }

                    graphics.setColor(gradient
                        ? interpolate(foreground, gradientEnd, (column + row) / (double) Math.max(1, 2 * (moduleCount - 1)))
                        : foreground);
                    ModuleShape moduleShape = isFinderPattern(row, column, moduleCount)
                        ? ModuleShape.SQUARE
                        : shape;
                    drawModule(graphics, moduleShape, x, y, moduleSize);
                }
            }

            if (logo != null) {
                drawLogo(graphics, logo, image.getWidth(), logoSizePercent, logoMarginPercent,
                    background, removeLogoBackground);
            }
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private int findModuleSize(BitMatrix matrix, int firstX, int firstY) {
        int runWidth = 0;
        while (firstX + runWidth < matrix.getWidth() && matrix.get(firstX + runWidth, firstY)) {
            runWidth++;
        }
        return Math.max(1, runWidth / 7);
    }

    private boolean isFinderPattern(int row, int column, int moduleCount) {
        return (row < 7 && column < 7)
            || (row < 7 && column >= moduleCount - 7)
            || (row >= moduleCount - 7 && column < 7);
    }

    private void drawModule(Graphics2D graphics, ModuleShape shape, int x, int y, int size) {
        double inset = size > 2 ? size * 0.08 : 0;
        double extent = size - inset * 2;
        switch (shape) {
            case DOT -> graphics.fillOval((int) Math.round(x + inset), (int) Math.round(y + inset),
                (int) Math.ceil(extent), (int) Math.ceil(extent));
            case ROUNDED -> graphics.fillRoundRect((int) Math.round(x + inset), (int) Math.round(y + inset),
                (int) Math.ceil(extent), (int) Math.ceil(extent), (int) Math.ceil(size * 0.4),
                (int) Math.ceil(size * 0.4));
            case DIAMOND -> {
                int centerX = x + size / 2;
                int centerY = y + size / 2;
                int half = Math.max(1, (int) Math.round(extent / 2));
                graphics.fillPolygon(
                    new int[]{centerX, centerX + half, centerX, centerX - half},
                    new int[]{centerY - half, centerY, centerY + half, centerY},
                    4
                );
            }
            case SQUARE -> graphics.fillRect(x, y, size, size);
        }
    }

    private Color interpolate(Color start, Color end, double fraction) {
        double amount = Math.max(0, Math.min(1, fraction));
        return new Color(
            (int) Math.round(start.getRed() + (end.getRed() - start.getRed()) * amount),
            (int) Math.round(start.getGreen() + (end.getGreen() - start.getGreen()) * amount),
            (int) Math.round(start.getBlue() + (end.getBlue() - start.getBlue()) * amount)
        );
    }

    private void drawLogo(
        Graphics2D graphics,
        BufferedImage source,
        int imageSize,
        int logoSizePercent,
        int marginPercent,
        Color background,
        boolean removeBackground
    ) {
        int logoSize = Math.max(1, imageSize * logoSizePercent / 100);
        int margin = logoSize * marginPercent / 100;
        int boxSize = logoSize + margin * 2;
        int x = (imageSize - boxSize) / 2;
        int y = x;
        graphics.setColor(background);
        graphics.fillRoundRect(x, y, boxSize, boxSize, Math.max(2, margin), Math.max(2, margin));

        BufferedImage logo = new BufferedImage(logoSize, logoSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D logoGraphics = logo.createGraphics();
        try {
            logoGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            double scale = Math.min(logoSize / (double) source.getWidth(), logoSize / (double) source.getHeight());
            int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
            logoGraphics.drawImage(source, (logoSize - width) / 2, (logoSize - height) / 2, width, height, null);
        } finally {
            logoGraphics.dispose();
        }
        if (removeBackground) {
            for (int py = 0; py < logo.getHeight(); py++) {
                for (int px = 0; px < logo.getWidth(); px++) {
                    int argb = logo.getRGB(px, py);
                    Color pixel = new Color(argb, true);
                    if (pixel.getAlpha() > 0 && pixel.getRed() > 240 && pixel.getGreen() > 240
                        && pixel.getBlue() > 240) {
                        logo.setRGB(px, py, argb & 0x00FFFFFF);
                    }
                }
            }
        }
        graphics.drawImage(logo, x + margin, y + margin, null);
    }
}
