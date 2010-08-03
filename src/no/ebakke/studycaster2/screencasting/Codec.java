package no.ebakke.studycaster2.screencasting;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

public abstract class Codec {
  protected static final byte INDEX_NO_DIFF = (byte) -1;
  protected static final byte INDEX_REPEAT  = (byte) -2;
  private ScreenCastImage newFrame, oldFrame;

  protected void init(Dimension dim) {
    this.newFrame = new ScreenCastImage(dim);
    this.oldFrame = new ScreenCastImage(dim);
  }

  private void swapOldNew() {
    ScreenCastImage tmp = oldFrame;
    oldFrame = newFrame;
    newFrame = tmp;
  }

  public Dimension getDimension() {
    return new Dimension(newFrame.getWidth(), newFrame.getHeight());
  }

  private void copyImage(BufferedImage from, BufferedImage to) {
    if (!to.getGraphics().drawImage(from, 0, 0, to.getWidth(), to.getHeight(), null))
      throw new AssertionError("Expected immediate image conversion");
  }

  protected void swapInFrame(BufferedImage frame) {
    swapOldNew();
    copyImage(frame, newFrame);
  }

  protected BufferedImage swapOutFrame() {
    ScreenCastImage ret = new ScreenCastImage(getDimension());
    copyImage(newFrame, ret);
    swapOldNew();
    return ret;
  }

  public ScreenCastImage getOldFrame() {
    return oldFrame;
  }

  public ScreenCastImage getNewFrame() {
    return newFrame;
  }
}
