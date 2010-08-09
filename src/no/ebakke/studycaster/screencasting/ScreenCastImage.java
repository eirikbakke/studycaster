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
    int[] cmap = new int[256 - RESERVED_INDICES];
    int i = 0;

    // Create a 6x6x6 color cube (exclude grayscales)
    for (int r = 0; r < 256; r += 51) {
      for (int g = 0; g < 256; g += 51) {
        for (int b = 0; b < 256; b += 51) {
          if (!(r == g && r == b))
            cmap[i++] = (r << 16) | (g << 8) | b;
        }
      }
    }
    // And populate the rest of the cmap with gray values (from black to white)
    double grayIncr = 255.0 / (256 - i - RESERVED_INDICES - 1);
    double gray = 0;
    for (; i < 256 - RESERVED_INDICES; i++) {
      int grayInt = (int) Math.round(gray);
      cmap[i] = (grayInt << 16) | (grayInt << 8) | grayInt;
      gray += grayIncr;
    }

    return new IndexColorModel(8, 256 - RESERVED_INDICES, cmap, 0, false, -1, DataBuffer.TYPE_BYTE);
  }
}
