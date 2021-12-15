package org.opendatakit.aggregate.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.aggregate.odktables.impl.api.FileManifestServiceImpl;
import org.opendatakit.common.datamodel.BinaryContentManipulator;

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

    public static final int REDUCED_SMALLER_DIMENSION = 480;
    public static final int REDUCED_LARGER_DIMENSION = 640;

    public static byte[] reducedImage(byte[] input, String contentType) throws IOException, IllegalArgumentException {
        InputStream in = new ByteArrayInputStream(input);
        BufferedImage ogImage = ImageIO.read(in);
        int newWidth;
        int newHeight;
        if (ogImage.getWidth() > ogImage.getHeight()) {
            newWidth = REDUCED_LARGER_DIMENSION;
            newHeight = REDUCED_SMALLER_DIMENSION;
        } else {
            newWidth = REDUCED_SMALLER_DIMENSION;
            newHeight = REDUCED_LARGER_DIMENSION;
        }
        newWidth = Math.min(ogImage.getWidth(), newWidth);
        newHeight = Math.min(ogImage.getHeight(), newHeight);
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