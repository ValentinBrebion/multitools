package com.example.multitools.tools.image;

import org.junit.Test;

import java.awt.image.BufferedImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ImageBackgroundRemovalServiceTest {

    @Test
    public void preservesSubjectPixelsConnectedOnlyDiagonally() {
        BufferedImage input = new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < input.getHeight(); y++) {
            for (int x = 0; x < input.getWidth(); x++) {
                input.setRGB(x, y, 0xff000000);
            }
        }
        input.setRGB(0, 0, 0xffffffff);
        input.setRGB(2, 0, 0xffffffff);
        input.setRGB(1, 1, 0xffffffff);

        BufferedImage result = new ImageBackgroundRemovalService().removeBackground(input, 0);

        assertEquals(0xff, result.getRGB(1, 1) >>> 24);
        assertEquals(0xff000000, result.getRGB(2, 2));
    }

    @Test
    public void preservesWhiteSubjectDetailEnclosedByForeground() {
        BufferedImage input = new BufferedImage(7, 7, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < input.getHeight(); y++) {
            for (int x = 0; x < input.getWidth(); x++) {
                input.setRGB(x, y, 0xffffffff);
            }
        }
        for (int y = 2; y <= 4; y++) {
            for (int x = 2; x <= 4; x++) {
                input.setRGB(x, y, 0xff202020);
            }
        }
        input.setRGB(3, 3, 0xffffffff);

        BufferedImage result = new ImageBackgroundRemovalService().removeBackground(input, 24);

        assertEquals("Le fond connecté au bord doit être transparent.", 0, result.getRGB(0, 0) >>> 24);
        assertEquals("Le détail blanc du sujet doit rester opaque.", 0xff, result.getRGB(3, 3) >>> 24);
        assertEquals("Le contour du sujet doit rester intact.", 0xff, result.getRGB(2, 3) >>> 24);
    }

    @Test
    public void higherToleranceRemovesMoreNearWhiteBackgroundPixels() {
        BufferedImage input = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < input.getHeight(); y++) {
            for (int x = 0; x < input.getWidth(); x++) {
                input.setRGB(x, y, 0xff000000);
            }
        }
        input.setRGB(0, 0, 0xffffffff);
        input.setRGB(9, 0, 0xffffffff);
        input.setRGB(0, 1, 0xfff0f0f0);

        ImageBackgroundRemovalService service = new ImageBackgroundRemovalService();
        int alphaAtZero = service.removeBackground(input, 0).getRGB(0, 1) >>> 24;
        int alphaAtFifteen = service.removeBackground(input, 15).getRGB(0, 1) >>> 24;
        int alphaAtTwenty = service.removeBackground(input, 20).getRGB(0, 1) >>> 24;
        int alphaAtForty = service.removeBackground(input, 40).getRGB(0, 1) >>> 24;

        assertEquals(0xff, alphaAtZero);
        assertTrue("La tolérance doit réduire progressivement l'opacité.", alphaAtFifteen < alphaAtZero);
        assertTrue("La tolérance doit rester effective au-delà de 15.", alphaAtTwenty < alphaAtFifteen);
        assertEquals(0, alphaAtForty);
    }
}
