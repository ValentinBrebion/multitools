package com.example.multitools.tools.qrcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.awt.image.BufferedImage;
import java.util.Map;

public class QRCodeGenerator {

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
        BufferedImage image = new BufferedImage(
            matrix.getWidth(),
            matrix.getHeight(),
            BufferedImage.TYPE_INT_RGB
        );
        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < matrix.getWidth(); x++) {
                image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
        }
        return image;
    }
}
