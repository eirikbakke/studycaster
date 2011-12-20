package no.ebakke.studycaster.screencasting;

import java.awt.Point;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/** Thread-safe and immutable. */
class CodecMeta {
  private final long      timeMillis;
  private final Point     pointerLocation;
  private final FrameType type;

  CodecMeta(long timeMillis, Point pointerLocation, FrameType type) {
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

  public static CodecMeta readFromStream(DataInputStream din) throws IOException {
    byte type = din.readByte();
    if (type < 0 || type >= FrameType.values().length)
      throw new IOException("Invalid metadata stamp type");
    long time = din.readLong();
    int x = din.readInt();
    int y = din.readInt();
    if (x == Integer.MIN_VALUE && y != Integer.MIN_VALUE)
      throw new IOException("Inconsistent inavailability of mouse coordinates");
    return new CodecMeta(time,
        (x == Integer.MIN_VALUE) ? null : new Point(x, y), FrameType.values()[type]);
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

  /* TODO: At next opportunity to change the file format, get rid of this distinction. Also
  introduce an end-of-file marker. */
  public static enum FrameType {
    PERIODIC, BEFORE_CAPTURE, AFTER_CAPTURE
  }
}
