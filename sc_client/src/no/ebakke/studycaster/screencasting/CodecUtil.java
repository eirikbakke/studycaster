package no.ebakke.studycaster.screencasting;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

final class CodecUtil {
  private CodecUtil() { }

  public static void copyImage(BufferedImage from, BufferedImage to) {
    Graphics2D g = to.createGraphics();
    // Sadly, turning dithering off this way doesn't actually work.
    // g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);

    /* This method was chosen for its high performance, even when converting from one color model to
    another. */
    if (!g.drawImage(from, 0, 0, from.getWidth(), from.getHeight(), null))
      throw new AssertionError("Expected immediate image conversion");
    g.dispose();
  }

  /** Make the components of the supplied dimension even, to make them compatible with certain
  output codecs. */
  public static Dimension makeEven(Dimension dim) {
    int width  = dim.width;
    int height = dim.height;
    if (width  % 2 != 0)
      width++;
    if (height % 2 != 0)
      height++;
    return new Dimension(width, height);
  }
}
