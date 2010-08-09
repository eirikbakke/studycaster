package no.ebakke.studycaster2.screencasting;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class Codec {
  protected enum FrameType { PERIODIC, BEFORE_CAPTURE, AFTER_CAPTURE; };
  protected static final String MAGIC_STRING = "StudyCaster Screencast";
  protected static final byte MARKER_FRAME = 1;
  protected static final byte MARKER_META  = 2;
  protected static final byte INDEX_NO_DIFF = (byte) -1;
  protected static final byte INDEX_REPEAT  = (byte) -2;
  private ScreenCastImage currentFrame, previousFrame;
  protected final Queue<MetaStamp> metaStamps = new ConcurrentLinkedQueue<MetaStamp>();

  protected static class MetaStamp {
    private long timeMillis;
    private Point pointerLocation;
    private FrameType type;

    public MetaStamp(long timeMillis, Point pointerLocation, FrameType type) {
      this.timeMillis = timeMillis;
      this.pointerLocation = pointerLocation;
      this.type = type;
    }

    public void writeToStream(DataOutputStream dout) throws IOException {
      dout.writeByte(type.ordinal());
      dout.writeLong(timeMillis);
      dout.writeInt((pointerLocation == null) ? Integer.MIN_VALUE : pointerLocation.x);
      dout.writeInt((pointerLocation == null) ? Integer.MIN_VALUE : pointerLocation.y);
    }

    public static MetaStamp readFromStream(DataInputStream din) throws IOException {
      byte type = din.readByte();
      if (type < 0 || type >= FrameType.values().length)
        throw new IOException("Invalid metadata stamp type");
      long time = din.readLong();
      int x = din.readInt();
      int y = din.readInt();
      return new MetaStamp(time, (x == Integer.MIN_VALUE) ? null : new Point(x, y), FrameType.values()[type]);
    }

    public Point getMouseLocation() {
      return pointerLocation;
    }

    public long getTimeMillis() {
      return timeMillis;
    }

    public FrameType getType() {
      return type;
    }
  }

  protected void init(Dimension dim) {
    this.currentFrame = new ScreenCastImage(dim);
    this.previousFrame = new ScreenCastImage(dim);
  }

  protected void swapOldNew() {
    ScreenCastImage tmp = previousFrame;
    previousFrame = currentFrame;
    currentFrame = tmp;
  }

  public Dimension getDimension() {
    return new Dimension(currentFrame.getWidth(), currentFrame.getHeight());
  }

  protected void copyImage(BufferedImage from, BufferedImage to) {
    Graphics2D g = to.createGraphics();
    // Sadly, this doesn't actually work.
    // g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
    if (!g.drawImage(from, 0, 0, to.getWidth(), to.getHeight(), null))
      throw new AssertionError("Expected immediate image conversion");
    g.dispose();
  }

  public ScreenCastImage getPreviousFrame() {
    return previousFrame;
  }

  public ScreenCastImage getCurrentFrame() {
    return currentFrame;
  }
}
