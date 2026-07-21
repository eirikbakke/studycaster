package no.ebakke.studycaster.screencasting;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;

public class ScreenCastImage extends BufferedImage {
  private static final int             RESERVED_INDICES = 2;
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
    if (bd[0].length != getWidth() * getHeight()) {
      throw new AssertionError(
          "Expected bank size " + getWidth() * getHeight() + ", got " + bd[0].length);
    }
    return bd[0];
  }

  private static IndexColorModel createColorModel() {
    // (third-party content removed)
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
  }
}
