package com.example.multitools.tools.qrcode;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Optional;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.Binarizer;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;

final class QRCodeDecoder {

    private QRCodeDecoder() {
    }

    static Optional<String> decode(BufferedImage image) {
        EnumMap<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

        LuminanceSource imageSource = new BufferedImageLuminanceSource(image);
        try {
            return Optional.of(decode(imageSource, new HybridBinarizer(imageSource), hints));
        } catch (ReaderException ignored) {
            // Try inverted luminance and color-channel fallbacks below.
        }

        LuminanceSource invertedSource = imageSource.invert();
        try {
            return Optional.of(
                decode(invertedSource, new GlobalHistogramBinarizer(invertedSource), hints)
            );
        } catch (ReaderException ignored) {
            // Some stylized codes have better contrast in an individual color channel.
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        for (int i = 0; i < pixels.length; i++) {
            int red = (pixels[i] >> 16) & 0xFF;
            pixels[i] = 0xFF - red;
        }
        LuminanceSource redChannelSource = new RGBLuminanceSource(width, height, pixels);
        try {
            return Optional.of(
                decode(redChannelSource, new GlobalHistogramBinarizer(redChannelSource), hints)
            );
        } catch (ReaderException ignored) {
            return Optional.empty();
        }
    }

    private static String decode(
        LuminanceSource source,
        Binarizer binarizer,
        EnumMap<DecodeHintType, Object> hints
    ) throws ReaderException {
        return new MultiFormatReader()
            .decode(new BinaryBitmap(binarizer), hints)
            .getText();
    }
}
