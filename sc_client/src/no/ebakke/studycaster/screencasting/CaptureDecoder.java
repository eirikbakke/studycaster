package no.ebakke.studycaster.screencasting;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import no.ebakke.studycaster.screencasting.CodecMeta.FrameType;
import no.ebakke.studycaster.screencasting.ExtendedMeta.ExtendedMetaReader;

/** Not thread-safe. */
public class CaptureDecoder {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  /** Put some extra space at the bottom to make it easier to work with video players that obscure
  this area with play controls during pausing and when the mouse pointer happens to be on the bottom
  of the screen (e.g. Chrome's built-in video player). */
  private static final double EXTRA_BOTTOM = 0.04;
  private final CodecState state;
  private final DataInputStream dis;
  private final ScreenCastOverlay overlay;
  private final Dimension outputDimension;
  private final ExtendedMetaReader extendedMetaReader;
  private boolean reachedEOF = false, atFrame = false;
  private long currentFrameTime = 0;
  private Long firstMetaTime, nextCaptureTime, lastBeforeCaptureTime;
  private boolean firstFrameRead = false;
  private boolean statFrameChanged, statFrameIndicator, statMetaIndicator, statUserInput;
  private ExtendedMeta currentExtendedMeta, nextExtendedMeta;
  private long lastUserInputNanos = Long.MIN_VALUE;
  private CodecMeta currentCodecMeta;

  /** Argument extendedMetaReader may be null. */
  public CaptureDecoder(InputStream is, ExtendedMetaReader extendedMetaReader) throws IOException {
    this.extendedMetaReader = extendedMetaReader;
    // TODO: Can this odd read be avoided?
    if (extendedMetaReader != null)
      nextExtendedMeta = extendedMetaReader.readOne();
    dis = new DataInputStream(new BufferedInputStream(new GZIPInputStream(is), 4 * 1024 * 1024));
    if (!dis.readUTF().equals(CodecConstants.MAGIC_STRING))
      throw new IOException("File not in StudyCaster screencast format");
    int width  = dis.readInt();
    int height = dis.readInt();
    state = new CodecState(new Dimension(width, height));
    overlay = new ScreenCastOverlay(new Dimension(width, height));
    outputDimension = CodecUtil.makeEven(new Dimension(width,
        (int) ((height + overlay.getStatusAreaHeight()) * (1 + EXTRA_BOTTOM))));
  }

  public void close() throws IOException {
    try {
      if (extendedMetaReader != null) {
        // As a sanity check, make sure we get to the end of the metadata without errors.
        forwardExtendedMeta(Long.MAX_VALUE);
        extendedMetaReader.close();
      }
    } finally {
      dis.close();
    }
  }

  public Dimension getDimension() {
    return new Dimension(outputDimension);
  }

  public long getCurrentTimeMillis() {
    if (currentCodecMeta == null)
      throw new IllegalStateException("No frame retrieved yet.");
    return currentFrameTime;
  }

  /** Skip forward, one byte at a time, until a valid MARKER_META structure has been read. Used to
  recover data in corrupted streams. */
  private void resync() throws IOException {
    // TODO: Avoid losing the metadata read to sync.
    /* TODO: At the next opportunity for changing the file format, include an explicit magic word at
             at the beginning of each MetaStamp, so that resync can be implemented in two lines of
             code instead of the madness below. */
    final int MARKER_META_STRUCT_SZ = 18;
    final int MAX_SKIP = 1024 * 1024;
    byte buf[] = new byte[MAX_SKIP];
    int curReadLoc = 0;
    while (curReadLoc < MARKER_META_STRUCT_SZ)
      buf[curReadLoc++] = dis.readByte();
    while (true) {
      DataInputStream din = new DataInputStream(new ByteArrayInputStream(buf,
          curReadLoc - MARKER_META_STRUCT_SZ, MARKER_META_STRUCT_SZ));
      byte struct_head = din.readByte();
      byte struct_type = din.readByte();
      long struct_time = din.readLong();
      int  struct_x    = din.readInt();
      int  struct_y    = din.readInt();

      if (struct_head != CodecConstants.MARKER_META) {
      } else if (struct_type < 0 || struct_type >= FrameType.values().length) {
        // Two years before this code was written/January 1st 2050.
      } else if (struct_time < 1250816790841L || struct_time > 2524608000000L) {
      } else if (struct_x == Integer.MIN_VALUE && struct_y != Integer.MIN_VALUE) {
      } else if (struct_x != Integer.MIN_VALUE &&
          (struct_x < 0 || struct_y < 0 || struct_x > 30000 || struct_y > 30000)) {
      } else {
        LOG.log(Level.INFO, "Resynced after skipping {0} bytes", curReadLoc);
        return;
      }

      if (curReadLoc == buf.length)
        throw new IOException("Failed to resync after skipping " + curReadLoc + "bytes");
      buf[curReadLoc++] = dis.readByte();
    }
  }

  private boolean readUntilFrame() throws IOException {
    CodecMeta codecMeta;
    byte headerMarker;
    while (true) {
      try {
        headerMarker = dis.readByte();
      } catch (EOFException e) {
        return false;
      }
      if        (headerMarker == CodecConstants.MARKER_FRAME) {
        return true;
      } else if (headerMarker == CodecConstants.MARKER_META) {
        codecMeta = CodecMeta.readFromStream(dis);
        // TODO: Simplify this once the file format is changed to get rid of FrameType.
        switch (codecMeta.getType()) {
          case        BEFORE_CAPTURE:
            lastBeforeCaptureTime = codecMeta.getTimeMillis();
          break; case AFTER_CAPTURE:
            if (lastBeforeCaptureTime == null)
              throw new IOException("Missing before-capture timestamp");
            //nextCaptureTime = lastBeforeCaptureTime / 2 + ms.getTimeMillis() / 2;
            nextCaptureTime = lastBeforeCaptureTime;
            lastBeforeCaptureTime = null;
          break; case PERIODIC:
        }
        state.addCodecMeta(codecMeta);
      } else {
        throw new IOException("Invalid header marker");
      }
    }
  }

  private void forwardExtendedMeta(long toTimeNanos) throws IOException {
    if (extendedMetaReader == null)
      return;
    while (nextExtendedMeta != null &&
           nextExtendedMeta.getDesktopMeta().getTimeNanos() <= toTimeNanos)
    {
      currentExtendedMeta = nextExtendedMeta;
      nextExtendedMeta    = extendedMetaReader.readOne();
    }
    if (currentExtendedMeta != null) {
      long newLastUserInputNanos = currentExtendedMeta.getDesktopMeta().getLastUserInputNanos();
      statUserInput |= newLastUserInputNanos > lastUserInputNanos;
      lastUserInputNanos = newLastUserInputNanos;
    }
  }

  /** Returns false iff the regular end of the stream was reached. */
  public boolean nextFrame() throws IOException {
    CodecMeta readCodecMeta = null;
    while (true) {
      if (readCodecMeta == null)
        readCodecMeta = state.pollCodecMeta();
      if (readCodecMeta == null || readCodecMeta.getTimeMillis() >= nextCaptureTime) {
        if (reachedEOF) {
          return false;
        } else if (atFrame) {
          readFrame();
          atFrame = false;
          firstFrameRead = true;
          statFrameChanged = true;
          continue;
        } else {
          reachedEOF = !readUntilFrame();
          atFrame = true;
          continue;
        }
      }
      if (firstFrameRead && readCodecMeta.getType() == FrameType.PERIODIC) {
        forwardExtendedMeta(readCodecMeta.getTimeMillis() * 1000000L);
        currentFrameTime += (currentCodecMeta == null) ? 0 :
            Math.max(1L, readCodecMeta.getTimeMillis() - currentCodecMeta.getTimeMillis());
        currentCodecMeta = readCodecMeta;
        if (firstMetaTime == null)
          firstMetaTime = currentCodecMeta.getTimeMillis();
        return true;
      }
      readCodecMeta = null;
    }
  }

  /** Draws the frame previously loaded by nextFrame(), and inverts the state of any blinking status
  indicators. */
  public void drawFrame(BufferedImage output) {
    CodecUtil.copyImage(state.getCurrentFrame(), output);
    Graphics2D g = output.createGraphics();
    if (currentCodecMeta.getMouseLocation() != null) {
      final Point p = currentCodecMeta.getMouseLocation();
      overlay.drawPointer(g, p.x, p.y);
    }
    if (currentExtendedMeta != null)
      overlay.drawDesktopMeta(g, currentExtendedMeta.getDesktopMeta());
    overlay.drawStatus(g, currentCodecMeta.getTimeMillis(),
        currentCodecMeta.getTimeMillis() - firstMetaTime, statUserInput, statFrameIndicator,
        statMetaIndicator,
        (currentExtendedMeta == null ? null : (currentExtendedMeta.getPageName())));
    g.dispose();

    // Blink indicator flags.
    statMetaIndicator  = !statMetaIndicator;
    statFrameIndicator = statFrameChanged ? !statFrameIndicator : statFrameIndicator;
    statFrameChanged = false;
    statUserInput    = false;
  }

  @SuppressWarnings("AssignmentToForLoopParameter")
  private void readFrame() throws IOException {
    state.swapFrames();
    final Graphics2D g  = state.getCurrentFrame().createGraphics();
    final byte oldBuf[] = state.getPreviousFrame().getBuffer();
    final byte newBuf[] = state.getCurrentFrame().getBuffer();
    int  currentRunLength = 0;
    byte currentRunCode   = CodecConstants.INDEX_NO_DIFF;

    for (int i = 0; i < newBuf.length; i++) {
      if (currentRunLength == 0) {
        final byte code = dis.readByte();
        if (code == CodecConstants.INDEX_REPEAT) {
          currentRunLength = dis.readInt();
        } else {
          currentRunLength = 1;
          currentRunCode   = code;
        }
      }
      if (currentRunLength <= 0 || currentRunLength > newBuf.length - i) {
        LOG.log(Level.WARNING, "Invalid or overflowing run length {0}", currentRunLength);
        currentRunLength = 0;
        // 23 = light blue, 14 = turquoise
        for (; i < newBuf.length; i++)
          newBuf[i] = 23;
        overlay.drawWarning(g, "Encoder warning: invalid run length");
        resync();
        break;
      }
      newBuf[i] = (currentRunCode == CodecConstants.INDEX_NO_DIFF) ? oldBuf[i] : currentRunCode;
      currentRunLength--;
    }
    if (currentRunLength > 0) {
      throw new AssertionError(
          "Invalid run length should have been caught by earlier consistency check");
    }
    g.dispose();
  }
}
