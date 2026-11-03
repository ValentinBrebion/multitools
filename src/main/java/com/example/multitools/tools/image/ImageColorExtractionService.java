package com.example.multitools.tools.image;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

public class ImageColorExtractionService {

    private static final int MIN_COLORS = 2;
    private static final int MAX_COLORS = 12;
    private static final int MAX_SAMPLES = 100_000;
    private static final int MIN_ALPHA = 32;

    public List<ExtractedColor> extractColors(BufferedImage image, int requestedCount) {
        if (image == null) {
            throw new IllegalArgumentException("L'image ne peut pas être nulle.");
        }
        if (requestedCount < MIN_COLORS || requestedCount > MAX_COLORS) {
            throw new IllegalArgumentException("Le nombre de couleurs doit être compris entre 2 et 12.");
        }

        List<Integer> samples = samplePixels(image);
        if (samples.isEmpty()) {
            return List.of();
        }

        List<ColorBox> boxes = new ArrayList<>();
        boxes.add(new ColorBox(samples));
        while (boxes.size() < requestedCount) {
            ColorBox boxToSplit = boxes.stream()
                .filter(box -> box.colors.size() > 1 && box.splitScore() > 0)
                .max(Comparator.comparingInt(ColorBox::splitScore))
                .orElse(null);
            if (boxToSplit == null) {
                break;
            }
            boxes.remove(boxToSplit);
            ColorBox[] split = boxToSplit.split();
            boxes.add(split[0]);
            boxes.add(split[1]);
        }

        List<ExtractedColor> extractedColors = boxes.stream()
            .sorted(Comparator.comparingInt((ColorBox box) -> box.colors.size()).reversed())
            .map(ColorBox::average)
            .toList();
        LinkedHashMap<String, ExtractedColor> uniqueColors = new LinkedHashMap<>();
        for (ExtractedColor color : extractedColors) {
            uniqueColors.putIfAbsent(color.hex(), color);
        }
        return List.copyOf(uniqueColors.values());
    }

    private List<Integer> samplePixels(BufferedImage image) {
        long pixelCount = (long) image.getWidth() * image.getHeight();
        int stride = Math.max(1, (int) Math.ceil(Math.sqrt((double) pixelCount / MAX_SAMPLES)));
        List<Integer> samples = new ArrayList<>((int) Math.min(pixelCount, MAX_SAMPLES));
        for (int y = 0; y < image.getHeight(); y += stride) {
            for (int x = 0; x < image.getWidth(); x += stride) {
                int argb = image.getRGB(x, y);
                if (((argb >>> 24) & 0xff) >= MIN_ALPHA) {
                    samples.add(argb & 0x00ffffff);
                }
            }
        }
        return samples;
    }

    public record ExtractedColor(int red, int green, int blue, int population) {

        public String hex() {
            return String.format("#%02X%02X%02X", red, green, blue);
        }

        public String rgb() {
            return "RVB(" + red + ", " + green + ", " + blue + ")";
        }
    }

    private static final class ColorBox {

        private final List<Integer> colors;
        private final int redRange;
        private final int greenRange;
        private final int blueRange;

        private ColorBox(List<Integer> colors) {
            this.colors = colors;
            int minRed = 255;
            int maxRed = 0;
            int minGreen = 255;
            int maxGreen = 0;
            int minBlue = 255;
            int maxBlue = 0;
            for (int color : colors) {
                int red = (color >>> 16) & 0xff;
                int green = (color >>> 8) & 0xff;
                int blue = color & 0xff;
                minRed = Math.min(minRed, red);
                maxRed = Math.max(maxRed, red);
                minGreen = Math.min(minGreen, green);
                maxGreen = Math.max(maxGreen, green);
                minBlue = Math.min(minBlue, blue);
                maxBlue = Math.max(maxBlue, blue);
            }
            redRange = maxRed - minRed;
            greenRange = maxGreen - minGreen;
            blueRange = maxBlue - minBlue;
        }

        private int splitScore() {
            return Math.max(redRange, Math.max(greenRange, blueRange));
        }

        private ColorBox[] split() {
            int channel = redRange >= greenRange && redRange >= blueRange
                ? 16
                : greenRange >= blueRange ? 8 : 0;
            colors.sort(Comparator.comparingInt(color -> (color >>> channel) & 0xff));
            int midpoint = colors.size() / 2;
            int splitIndex = midpoint;
            int closestBoundary = Integer.MAX_VALUE;
            for (int index = 1; index < colors.size(); index++) {
                int previousValue = (colors.get(index - 1) >>> channel) & 0xff;
                int currentValue = (colors.get(index) >>> channel) & 0xff;
                int distanceFromMidpoint = Math.abs(midpoint - index);
                if (previousValue != currentValue && distanceFromMidpoint < closestBoundary) {
                    splitIndex = index;
                    closestBoundary = distanceFromMidpoint;
                }
            }
            return new ColorBox[] {
                new ColorBox(new ArrayList<>(colors.subList(0, splitIndex))),
                new ColorBox(new ArrayList<>(colors.subList(splitIndex, colors.size())))
            };
        }

        private ExtractedColor average() {
            long red = 0;
            long green = 0;
            long blue = 0;
            for (int color : colors) {
                red += (color >>> 16) & 0xff;
                green += (color >>> 8) & 0xff;
                blue += color & 0xff;
            }
            int count = colors.size();
            return new ExtractedColor(
                (int) (red / count),
                (int) (green / count),
                (int) (blue / count),
                count
            );
        }
    }
}
