package com.example.multitools.tools.qrcode;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.w3c.dom.Document;

import javax.imageio.ImageIO;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public final class QRCodeLogoLoader {

    private QRCodeLogoLoader() {
    }

    public static BufferedImage load(File file) throws IOException {
        String name = file.getName().toLowerCase(java.util.Locale.ROOT);
        if (name.endsWith(".svg")) {
            return loadSvg(file);
        }
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("Le fichier PNG sélectionné ne peut pas être lu.");
        }
        return image;
    }

    private static BufferedImage loadSvg(File file) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Document document = factory.newDocumentBuilder().parse(file);

            PNGTranscoder transcoder = new PNGTranscoder();
            transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_ALLOW_EXTERNAL_RESOURCES, false);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            transcoder.transcode(new TranscoderInput(document), new TranscoderOutput(output));
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(output.toByteArray()));
            if (image == null) {
                throw new IOException("Le fichier SVG sélectionné ne peut pas être converti.");
            }
            return image;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Impossible de lire le logo SVG : " + e.getMessage(), e);
        }
    }
}
