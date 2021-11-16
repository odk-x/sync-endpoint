package org.opendatakit.aggregate.util;

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

public class ImageManipulation {
    public static final int RESIZED_IMAGE_WIDTH = 400;
    public static final int RESIZED_IMAGE_HEIGHT = 800;

    // TODO need to make sure this code works for images with different formats like rgb, etc.
    // TODO handle case where input dimensions are less than RESIZED_IMAGE_Dimensions
    public static byte[] reducedImage(byte[] input, String contentType) throws IOException {
        InputStream in = new ByteArrayInputStream(input);
        BufferedImage fullSizeImage = ImageIO.read(in);
        ImageWriter writer = ImageIO.getImageWritersByMIMEType(contentType).next();

        Image scaledImage = fullSizeImage.getScaledInstance(RESIZED_IMAGE_WIDTH, RESIZED_IMAGE_HEIGHT,
                Image.SCALE_SMOOTH);
        BufferedImage imageBuff = new BufferedImage(RESIZED_IMAGE_WIDTH, RESIZED_IMAGE_HEIGHT,
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