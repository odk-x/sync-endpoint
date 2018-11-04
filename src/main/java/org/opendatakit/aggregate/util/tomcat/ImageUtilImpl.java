/*
 * Copyright (C) 2011 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.aggregate.util.tomcat;

import org.opendatakit.aggregate.util.ImageUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageUtilImpl implements ImageUtil {

  @Override
  public byte[] resizeImage(byte[] bytes, int width, int height) {

    BufferedImage image;
    try {
      image = ImageIO.read(new ByteArrayInputStream(bytes));
    } catch (IOException e1) {
      e1.printStackTrace();
      return bytes; // returns unaltered bytes
    }

    int imgWidth = image.getWidth();
    int imgHeight = image.getHeight();

    if (width <= 0 || height <= 0) {
      return bytes; // returns unaltered bytes
    }

    int reductionWidth = imgWidth / width;
    int reductionHeight = imgHeight / height;

    if (reductionWidth <= 0 || reductionHeight <= 0) {
      return bytes; // returns unaltered bytes
    }

    int reducer;
    if (reductionWidth > reductionHeight) {
      reducer = reductionWidth;
      if ((imgWidth % width) != 0) {
        reducer++;
      }
    } else {
      reducer = reductionHeight;
      if ((imgHeight % height) != 0) {
        reducer++;
      }
    }

    int resizeWidth = imgWidth / reducer;
    int resizeHeight = imgHeight / reducer;

    Image resized = image.getScaledInstance(resizeWidth, resizeHeight,
        Image.SCALE_FAST);

    ByteArrayOutputStream fileStream = new ByteArrayOutputStream();

    BufferedImage bi = new BufferedImage(resized.getWidth(null),
        resized.getHeight(null), BufferedImage.TYPE_INT_RGB);

    Graphics bg = bi.getGraphics();
    bg.drawImage(resized, 0, 0, null);
    bg.dispose();

    try {
      ImageIO.write(bi, "jpg", fileStream);
    } catch (IOException e) {
      e.printStackTrace();
      return bytes; // returns unaltered bytes
    }

    return fileStream.toByteArray();
  }
}
