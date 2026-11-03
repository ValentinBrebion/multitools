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

    private static final int EDGE_FEATHER = 12;

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
        int[] backgroundColors = collectBackgroundColors(input);
        int[] pixels = input.getRGB(0, 0, width, height, null, 0, width);
        byte[] backgroundMask = new byte[pixelCount];
        int maximumDistance = tolerance + EDGE_FEATHER;
        int maximumDistanceSquared = maximumDistance * maximumDistance;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                int backgroundColor = nearestBackgroundColor(pixels[index], backgroundColors);
                if ((pixels[index] >>> 24) != 0 && backgroundColor >= 0
                    && colorDistanceSquared(pixels[index], backgroundColor) <= maximumDistanceSquared) {
                    backgroundMask[index] = 1;
                }
            }
        }

        findBorderConnectedBackground(backgroundMask, width, height);
        BufferedImage output = new BufferedImage(
            width, height, BufferedImage.TYPE_INT_ARGB
        );
        for (int index = 0; index < pixelCount; index++) {
            if (backgroundMask[index] == 2) {
                int backgroundColor = nearestBackgroundColor(pixels[index], backgroundColors);
                pixels[index] = applySoftMatte(pixels[index], backgroundColor, tolerance);
            }
        }
        output.setRGB(0, 0, width, height, pixels, 0, width);
        return output;
    }

    private int[] collectBackgroundColors(BufferedImage input) {
        int width = input.getWidth();
        int height = input.getHeight();
        int sampleWidth = Math.max(1, width / 12);
        int sampleHeight = Math.max(1, height / 12);
        int stride = Math.max(1, Math.min(width, height) / 200);
        return new int[] {
            medianCornerColor(input, 0, sampleWidth, sampleHeight, stride),
            medianCornerColor(input, width - sampleWidth, sampleWidth, sampleHeight, stride)
        };
    }

    private int medianCornerColor(BufferedImage input, int startX, int sampleWidth,
                                  int sampleHeight, int stride) {
        int[] redHistogram = new int[256];
        int[] greenHistogram = new int[256];
        int[] blueHistogram = new int[256];
        int sampleCount = 0;
        for (int y = 0; y < sampleHeight; y += stride) {
            for (int x = 0; x < sampleWidth; x += stride) {
                int argb = input.getRGB(startX + x, y);
                if ((argb >>> 24) < 16) {
                    continue;
                }
                redHistogram[(argb >>> 16) & 0xff]++;
                greenHistogram[(argb >>> 8) & 0xff]++;
                blueHistogram[argb & 0xff]++;
                sampleCount++;
            }
        }
        if (sampleCount == 0) {
            return -1;
        }
        int red = histogramMedian(redHistogram, sampleCount);
        int green = histogramMedian(greenHistogram, sampleCount);
        int blue = histogramMedian(blueHistogram, sampleCount);
        return (red << 16) | (green << 8) | blue;
    }

    private int histogramMedian(int[] histogram, int sampleCount) {
        int medianIndex = sampleCount / 2;
        int accumulated = 0;
        for (int value = 0; value < histogram.length; value++) {
            accumulated += histogram[value];
            if (accumulated > medianIndex) {
                return value;
            }
        }
        throw new IllegalStateException("L'histogramme de couleur est vide.");
    }

    private int nearestBackgroundColor(int argb, int[] backgroundColors) {
        int nearestColor = -1;
        int nearestDistance = Integer.MAX_VALUE;
        for (int backgroundColor : backgroundColors) {
            if (backgroundColor < 0) {
                continue;
            }
            int distance = colorDistanceSquared(argb, backgroundColor);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestColor = backgroundColor;
            }
        }
        return nearestColor;
    }

    private int colorDistanceSquared(int firstArgb, int secondRgb) {
        int redDifference = ((firstArgb >>> 16) & 0xff) - ((secondRgb >>> 16) & 0xff);
        int greenDifference = ((firstArgb >>> 8) & 0xff) - ((secondRgb >>> 8) & 0xff);
        int blueDifference = (firstArgb & 0xff) - (secondRgb & 0xff);
        return redDifference * redDifference
            + greenDifference * greenDifference
            + blueDifference * blueDifference;
    }

    private int applySoftMatte(int argb, int backgroundRgb, int tolerance) {
        double distance = Math.sqrt(colorDistanceSquared(argb, backgroundRgb));
        double matte = Math.max(0, Math.min(1, (distance - tolerance) / EDGE_FEATHER));
        matte = matte * matte * (3 - 2 * matte);
        int sourceAlpha = (argb >>> 24) & 0xff;
        int outputAlpha = (int) Math.round(sourceAlpha * matte);
        if (outputAlpha == 0) {
            return argb & 0x00ffffff;
        }

        double remainingBackground = 1 - matte;
        int red = removeBackgroundColor((argb >>> 16) & 0xff,
            (backgroundRgb >>> 16) & 0xff, remainingBackground, matte);
        int green = removeBackgroundColor((argb >>> 8) & 0xff,
            (backgroundRgb >>> 8) & 0xff, remainingBackground, matte);
        int blue = removeBackgroundColor(argb & 0xff,
            backgroundRgb & 0xff, remainingBackground, matte);
        return (outputAlpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private int removeBackgroundColor(int source, int background, double remainingBackground, double matte) {
        return (int) Math.round(Math.max(0, Math.min(255,
            (source - background * remainingBackground) / matte)));
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
}
