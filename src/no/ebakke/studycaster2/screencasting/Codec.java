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
  protected static final String MAGIC_STRING = "StudyCaster Screencast";
  protected static final byte MARKER_FRAME = 1;
  protected static final byte MARKER_META  = 2;
  protected static final byte INDEX_NO_DIFF = (byte) -1;
  protected static final byte INDEX_REPEAT  = (byte) -2;
  private ScreenCastImage currentFrame, previousFrame;
  protected final Queue<MetaStamp> metaStamps = new ConcurrentLinkedQueue<MetaStamp>();

  protected static class MetaStamp {
    public enum Type {TYPE_PERIODIC, TYPE_BEFORE_CAPTURE, TYPE_AFTER_CAPTURE};

    private long timeMillis;
    private Point mouseLocation;
    private Type type;

    public MetaStamp(long timeMillis, Point mouseLocation, Type type) {
      this.timeMillis = timeMillis;
      this.mouseLocation = mouseLocation;
      this.type = type;
    }

    public void writeToStream(DataOutputStream dout) throws IOException {
      dout.writeByte(type.ordinal());
      dout.writeLong(timeMillis);
      dout.writeInt((mouseLocation == null) ? Integer.MIN_VALUE : mouseLocation.x);
      dout.writeInt((mouseLocation == null) ? Integer.MIN_VALUE : mouseLocation.y);
    }

    public static MetaStamp readFromStream(DataInputStream din) throws IOException {
      byte type = din.readByte();
      if (type < 0 || type >= Type.values().length)
        throw new IOException("Invalid metadata stamp type");
      long time = din.readLong();
      int x = din.readInt();
      int y = din.readInt();
      return new MetaStamp(time, (x == Integer.MIN_VALUE) ? null : new Point(x, y), Type.values()[type]);
    }

    public Point getMouseLocation() {
      return mouseLocation;
    }

    public long getTimeMillis() {
      return timeMillis;
    }

    public Type getType() {
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
