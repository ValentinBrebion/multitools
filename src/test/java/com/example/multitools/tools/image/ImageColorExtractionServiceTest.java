package com.example.multitools.tools.image;

import org.junit.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ImageColorExtractionServiceTest {

    @Test
    public void extractsDominantColorsInOrderOfFrequency() {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, x < 7 ? 0xffcc2200 : 0xff0033cc);
            }
        }

        List<ImageColorExtractionService.ExtractedColor> colors =
            new ImageColorExtractionService().extractColors(image, 2);

        assertEquals(2, colors.size());
        assertEquals("#CC2200", colors.get(0).hex());
        assertEquals("#0033CC", colors.get(1).hex());
        assertEquals("RVB(204, 34, 0)", colors.get(0).rgb());
    }

    @Test
    public void ignoresTransparentPixelsAndDoesNotDuplicateSolidColors() {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, 0x00000000);
            }
        }
        image.setRGB(1, 1, 0xff28a050);

        List<ImageColorExtractionService.ExtractedColor> colors =
            new ImageColorExtractionService().extractColors(image, 6);

        assertEquals(1, colors.size());
        assertEquals("#28A050", colors.get(0).hex());
        assertTrue(colors.get(0).population() > 0);
    }
}
