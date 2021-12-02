package org.opendatakit.aggregate.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.aggregate.odktables.impl.api.FileManifestServiceImpl;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.lang.IllegalArgumentException;

public class ImageManipulation {
    public static final int RESIZED_IMAGE_WIDTH = 50;
    public static final int RESIZED_IMAGE_HEIGHT = 100;

    // TODO need to make sure this code works for images with different formats like rgb, etc.
    // TODO handle case where input dimensions are less than RESIZED_IMAGE_Dimensions
    public static byte[] reducedImage(byte[] input, String contentType) throws IOException, IllegalArgumentException {
        InputStream in = new ByteArrayInputStream(input);
        BufferedImage ogImage = ImageIO.read(in);
        int newWidth = Math.min(ogImage.getWidth(), RESIZED_IMAGE_WIDTH);
        int newHeight = Math.min(ogImage.getHeight(), RESIZED_IMAGE_HEIGHT);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(contentType);
        if (!writers.hasNext()) {
            throw new IllegalArgumentException("Content-Type [" + contentType + "] is not supported by the Java Image I/O API");
        }
        ImageWriter writer = writers.next();
        Image scaledImage = ogImage.getScaledInstance(newWidth, newHeight,
                Image.SCALE_SMOOTH);
        BufferedImage imageBuff = new BufferedImage(newWidth, newHeight,
                BufferedImage.TYPE_INT_RGB);
        imageBuff.getGraphics().drawImage(scaledImage, 0, 0, new Color(0, 0, 0), null);

        ByteArrayOutputStream resized = new ByteArrayOutputStream();
        ImageOutputStream ios = ImageIO.createImageOutputStream(resized);
        writer.setOutput(ios);
        writer.write(new IIOImage(imageBuff, null, null));
        byte[] fileBlob = resized.toByteArray();
        return fileBlob;
    }
}