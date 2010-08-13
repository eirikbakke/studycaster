package no.ebakke.studycaster.screencasting;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;

public class ScreenCastImage extends BufferedImage {
  private static final int RESERVED_INDICES = 2;
  private static final IndexColorModel SCREENCAST_COLOR_MODEL = createColorModel();

  public ScreenCastImage(Dimension dim) {
    super(dim.width, dim.height, BufferedImage.TYPE_BYTE_INDEXED, SCREENCAST_COLOR_MODEL);
  }

  public byte[] getBuffer() {
    DataBuffer db = getRaster().getDataBuffer();
    if (!(db instanceof DataBufferByte))
      throw new AssertionError("Expected DataBufferByte, got object " + db.toString());
    byte bd[][] = ((DataBufferByte) db).getBankData();
    if (bd.length != 1)
      throw new AssertionError("Expected one bank, got " + bd.length);
    if (bd[0].length != getWidth() * getHeight())
      throw new AssertionError("Expected bank size " + getWidth() * getHeight() + ", got " + bd[0].length);
    return bd[0];
  }

  private static IndexColorModel createColorModel() {
    // Based on BufferedImage's constructor.
    int[] cmap = new int[256];
    int i = 0;

    // Index zero is black.
    cmap[i++] = 0;

    // Create a 6x6x6 color cube (exclude grayscales)
    for (int r = 0; r < 256; r += 51) {
      for (int g = 0; g < 256; g += 51) {
        for (int b = 0; b < 256; b += 51) {
          if (!(r == g && g == b))
            cmap[i++] = (r << 16) | (g << 8) | b;
        }
      }
    }
    // Assign the remaining non-reserved indicies to gray values (from black, exclusive, to white, inclusive)
    double grayIncr = 255.0 / (256 - i - RESERVED_INDICES);
    double gray = 0;
    for (; i < 256 - RESERVED_INDICES; i++) {
      gray += grayIncr;
      int grayInt = (int) Math.round(gray);
      cmap[i] = (grayInt << 16) | (grayInt << 8) | grayInt;
    }
    /* Make the reserved indicies black; these can be reassigned to index zero by the encoder.
    This approach was chosen due to an apparent bug in JRE 1.5 in which unmapped color values
    would be assumed to represent black regardless of the size of the color map as specified
    in the constructor. */
    for (; i < 256; i++)
      cmap[i] = 0;

    return new IndexColorModel(8, 256, cmap, 0, false, -1, DataBuffer.TYPE_BYTE);
  }
}
