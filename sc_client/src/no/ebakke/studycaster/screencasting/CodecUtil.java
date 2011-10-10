package no.ebakke.studycaster.screencasting;

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
    if (!g.drawImage(from, 0, 0, to.getWidth(), to.getHeight(), null))
      throw new AssertionError("Expected immediate image conversion");
    g.dispose();
  }
}
