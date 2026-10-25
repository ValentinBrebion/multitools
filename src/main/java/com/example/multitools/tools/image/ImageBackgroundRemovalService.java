package com.example.multitools.tools.image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;

public class ImageBackgroundRemovalService {

    private static final int LUMA_BITS = 5;
    private static final int CHROMA_BITS = 6;
    private static final int LUMA_BIN_COUNT = 1 << LUMA_BITS;
    private static final int CHROMA_BIN_COUNT = 1 << CHROMA_BITS;
    private static final int COLOR_BIN_WIDTH = 1 << (8 - LUMA_BITS);
    private static final int CHROMA_BIN_WIDTH = 1 << (8 - CHROMA_BITS);
    private static final int COLOR_BIN_COUNT = 1 << (LUMA_BITS + CHROMA_BITS * 2);
    private static final int CHROMA_TOLERANCE = 4;

    public void removeBackground(Path source, Path destination, int tolerance) throws IOException {
        Path normalizedSource = source.toAbsolutePath().normalize();
        Path normalizedDestination = destination.toAbsolutePath().normalize();
        if (normalizedSource.equals(normalizedDestination)) {
            throw new IOException("Le fichier de destination doit être différent du fichier source.");
        }
        if (!Files.isRegularFile(normalizedSource) || !Files.isReadable(normalizedSource)) {
            throw new IOException("Le fichier image sélectionné est introuvable ou illisible.");
        }
        if (tolerance < 0 || tolerance > 80) {
            throw new IllegalArgumentException("La tolérance doit être comprise entre 0 et 80.");
        }
        if (!normalizedDestination.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")) {
            throw new IOException("Le fichier de destination doit avoir l'extension .png.");
        }
        Path parent = normalizedDestination.getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            throw new IOException("Le dossier de destination n'existe pas.");
        }

        BufferedImage input = ImageIO.read(normalizedSource.toFile());
        if (input == null) {
            throw new IOException("Le fichier sélectionné ne contient pas une image prise en charge.");
        }
        BufferedImage output = removeBackground(input, tolerance);

        Path temporaryFile = Files.createTempFile(parent, ".background-removal-", ".png");
        try {
            if (!ImageIO.write(output, "png", temporaryFile.toFile())) {
                throw new IOException("L'encodeur PNG n'est pas disponible.");
            }
            try {
                Files.move(temporaryFile, normalizedDestination,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporaryFile, normalizedDestination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    BufferedImage removeBackground(BufferedImage input, int tolerance) {
        if (input == null) {
            throw new IllegalArgumentException("L'image à traiter ne peut pas être nulle.");
        }
        if (tolerance < 0 || tolerance > 80) {
            throw new IllegalArgumentException("La tolérance doit être comprise entre 0 et 80.");
        }

        int width = input.getWidth();
        int height = input.getHeight();
        long pixelCountLong = (long) width * height;
        if (pixelCountLong > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("L'image est trop grande pour être traitée.");
        }
        int pixelCount = (int) pixelCountLong;
        boolean[] borderColors = collectBorderColors(input);
        boolean[] backgroundPalette = expandBorderColors(borderColors, tolerance);
        int[] pixels = input.getRGB(0, 0, width, height, null, 0, width);
        byte[] backgroundMask = new byte[pixelCount];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = pixels[y * width + x];
                int red = (argb >>> 16) & 0xff;
                int green = (argb >>> 8) & 0xff;
                int blue = argb & 0xff;
                int index = y * width + x;
                backgroundMask[index] = (byte) (backgroundPalette[colorBin(red, green, blue)] ? 1 : 0);
            }
        }

        findBorderConnectedBackground(backgroundMask, width, height);
        BufferedImage output = new BufferedImage(
            width, height, BufferedImage.TYPE_INT_ARGB
        );
        for (int index = 0; index < pixelCount; index++) {
            if (backgroundMask[index] == 2) {
                pixels[index] &= 0x00ffffff;
            }
        }
        output.setRGB(0, 0, width, height, pixels, 0, width);
        return output;
    }

    private boolean[] collectBorderColors(BufferedImage input) {
        int width = input.getWidth();
        int height = input.getHeight();
        boolean[] borderColors = new boolean[COLOR_BIN_COUNT];
        int sampleWidth = Math.max(1, width / 5);
        int sampleHeight = Math.max(1, height / 8);
        int stride = Math.max(1, Math.min(width, height) / 200);
        for (int y = 0; y < sampleHeight; y += stride) {
            for (int x = 0; x < sampleWidth; x += stride) {
                addBorderColor(input.getRGB(x, y), borderColors);
                addBorderColor(input.getRGB(width - 1 - x, y), borderColors);
            }
        }
        return borderColors;
    }

    private void addBorderColor(int argb, boolean[] borderColors) {
        if (((argb >>> 24) & 0xff) < 16) {
            return;
        }
        borderColors[colorBin((argb >>> 16) & 0xff, (argb >>> 8) & 0xff, argb & 0xff)] = true;
    }

    private boolean[] expandBorderColors(boolean[] borderColors, int tolerance) {
        boolean[] palette = new boolean[COLOR_BIN_COUNT];
        int radius = (tolerance + COLOR_BIN_WIDTH - 1) / COLOR_BIN_WIDTH;
        int chromaRadius = (CHROMA_TOLERANCE + CHROMA_BIN_WIDTH - 1) / CHROMA_BIN_WIDTH;
        for (int bin = 0; bin < borderColors.length; bin++) {
            if (!borderColors[bin]) {
                continue;
            }
            int lumaBin = (bin >>> (CHROMA_BITS * 2)) & (LUMA_BIN_COUNT - 1);
            int redChromaBin = (bin >>> CHROMA_BITS) & (CHROMA_BIN_COUNT - 1);
            int greenChromaBin = bin & (CHROMA_BIN_COUNT - 1);
            for (int luma = Math.max(0, lumaBin - radius);
                 luma <= Math.min(LUMA_BIN_COUNT - 1, lumaBin + radius); luma++) {
                for (int redChroma = Math.max(0, redChromaBin - chromaRadius);
                     redChroma <= Math.min(CHROMA_BIN_COUNT - 1, redChromaBin + chromaRadius);
                     redChroma++) {
                    for (int greenChroma = Math.max(0, greenChromaBin - chromaRadius);
                         greenChroma <= Math.min(CHROMA_BIN_COUNT - 1, greenChromaBin + chromaRadius);
                         greenChroma++) {
                        palette[(luma << (CHROMA_BITS * 2))
                            | (redChroma << CHROMA_BITS) | greenChroma] = true;
                    }
                }
            }
        }
        return palette;
    }

    private void findBorderConnectedBackground(byte[] mask, int width, int height) {
        int pixelCount = mask.length;
        int initialCapacity = (int) Math.min(
            pixelCount, Math.max(1024L, ((long) width + height) * 2)
        );
        int[] queue = new int[initialCapacity];
        int queueSize = 0;

        for (int x = 0; x < width; x++) {
            queueSize = enqueueBackgroundPixel(mask, queue, queueSize, x);
            if (height > 1) {
                queueSize = enqueueBackgroundPixel(mask, queue, queueSize, (height - 1) * width + x);
            }
        }
        for (int y = 1; y < height - 1; y++) {
            queueSize = enqueueBackgroundPixel(mask, queue, queueSize, y * width);
            if (width > 1) {
                queueSize = enqueueBackgroundPixel(mask, queue, queueSize, y * width + width - 1);
            }
        }

        int readIndex = 0;
        while (readIndex < queueSize) {
            int index = queue[readIndex++];
            int x = index % width;
            int y = index / width;
            for (int offsetY = -1; offsetY <= 1; offsetY++) {
                int nextY = y + offsetY;
                if (nextY < 0 || nextY >= height) {
                    continue;
                }
                for (int offsetX = -1; offsetX <= 1; offsetX++) {
                    if (Math.abs(offsetX) + Math.abs(offsetY) != 1) {
                        continue;
                    }
                    int nextX = x + offsetX;
                    if (nextX < 0 || nextX >= width) {
                        continue;
                    }
                    int nextIndex = nextY * width + nextX;
                    if (mask[nextIndex] == 1) {
                        mask[nextIndex] = 2;
                        if (queueSize == queue.length) {
                        int newCapacity = (int) Math.min(pixelCount, (long) queue.length * 2);
                        queue = Arrays.copyOf(queue, newCapacity);
                    }
                    queue[queueSize++] = nextIndex;
                    }
                }
            }
        }
    }

    private int enqueueBackgroundPixel(byte[] mask, int[] queue, int queueSize, int index) {
        if (mask[index] == 1) {
            mask[index] = 2;
            queue[queueSize++] = index;
        }
        return queueSize;
    }

    private int colorBin(int red, int green, int blue) {
        int luma = (red * 299 + green * 587 + blue * 114) / 1000;
        int redChroma = red - blue + 255;
        int greenChroma = green - blue + 255;
        return ((luma >>> (8 - LUMA_BITS)) << (CHROMA_BITS * 2))
            | ((redChroma >>> (8 - CHROMA_BITS)) << CHROMA_BITS)
            | (greenChroma >>> (8 - CHROMA_BITS));
    }
}
