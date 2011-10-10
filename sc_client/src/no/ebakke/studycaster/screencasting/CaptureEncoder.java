package no.ebakke.studycaster.screencasting;
import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;
import no.ebakke.studycaster.util.Util;

/** Thread-safe. */
public class CaptureEncoder extends Codec {
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final DataOutputStream dout;
  private final Rectangle screenRect;
  private final Robot robot;
  private ScreenCensor censor;
  private long serverSecondsAhead;

  public CaptureEncoder(OutputStream out, Rectangle screenRect) throws IOException, AWTException {
    this.screenRect = screenRect;
    Dimension dim = screenRect.getSize();
    init(dim);
    robot = new Robot();
    dout = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(out)));
    dout.writeUTF(MAGIC_STRING);
    dout.writeInt(dim.width);
    dout.writeInt(dim.height);
  }

  public synchronized void setCensor(ScreenCensor censor) {
    this.censor = censor;
  }

  public synchronized void setServerSecondsAhead(long serverSecondsAhead) {
    this.serverSecondsAhead = serverSecondsAhead;
  }

  /** No external synchronization is needed to invoke this method. */
  private void addMeta(FrameType type) {
    long time = System.currentTimeMillis() + serverSecondsAhead * 1000L;
    PointerInfo pi = MouseInfo.getPointerInfo();
    Point mouseLoc = (pi == null) ? null : pi.getLocation();
    // The metaStamps queue is thread-safe; see the superclass.
    metaStamps.add(new MetaStamp(time, mouseLoc, type));
  }

  private void flushMeta() throws IOException {
    MetaStamp ms;
    while ((ms = metaStamps.poll()) != null) {
      dout.write(MARKER_META);
      ms.writeToStream(dout);
    }
  }

  private void encodeRun(byte code, int runLength) throws IOException {
    if (code == INDEX_REPEAT || runLength < 0)
      throw new AssertionError();
    if (runLength <= 6) {
      for (int i = 0; i < runLength; i++)
        dout.write(code);
    } else {
      dout.write(code);
      dout.write(INDEX_REPEAT);
      dout.writeInt(runLength - 1);
    }
  }

  public void capturePointer() {
    /* No explicit synchronization is used or needed in this method. If the synchronized keyword had
    been used, the method would frequently block for a long period while waiting for captureFrame()
    to complete in a different thread. */
    Util.checkClosed(closed);
    addMeta(FrameType.PERIODIC);
  }

  public synchronized void captureFrame() throws IOException {
    Util.checkClosed(closed);
    addMeta(FrameType.BEFORE_CAPTURE);
    BufferedImage image = robot.createScreenCapture(screenRect);
    Quilt permittedArea = (censor == null) ?
        new Quilt(screenRect) : censor.getPermittedRecordingArea();
    addMeta(FrameType.AFTER_CAPTURE);
    flushMeta();
    compressAndOutputFrame(image, permittedArea);
  }

  private static byte blurredPixel(byte buf[], int width, int x, int y) {
    // TODO: What if the "color" is INDEX_NO_DIFF?
    return buf[y * width + (x / ScreenCensor.MOSAIC_WIDTH) * ScreenCensor.MOSAIC_WIDTH];
  }

  private void compressAndOutputFrame(BufferedImage frame, Quilt permittedArea)
      throws IOException
  {
    swapOldNew();
    copyImage(frame, getCurrentFrame());
    final byte oldBuf[] = getPreviousFrame().getBuffer();
    final byte newBuf[] = getCurrentFrame().getBuffer();
    final int width  = getDimension().width;
    final int height = getDimension().height;
    int  currentRunLength = 0;
    byte currentRunCode   = INDEX_NO_DIFF;
    byte code;

    dout.write(MARKER_FRAME);
    for (int y = 0, i = 0; y < height; y++) {
      boolean censorRunPermitted = false;
      int     censorRunRemaining  = 0;
      for (int x = 0; x < width; x++, i++) {
        // Workaround for bug in Java 1.5. See comment in ScreenCastImage.
        if (newBuf[i] == INDEX_NO_DIFF || newBuf[i] == INDEX_REPEAT)
          newBuf[i] = 0;

        // Screen censoring related.
        if (censorRunRemaining == 0) {
          censorRunRemaining = permittedArea.getHorizontalRunLength(x, y);
          censorRunPermitted = (censorRunRemaining > 0);
          censorRunRemaining = Math.abs(censorRunRemaining);
        }
        if (!censorRunPermitted)
          newBuf[i] = blurredPixel(newBuf, width, x, y);
        censorRunRemaining--;

        // Apply differential between old and new frames.
        final byte oldCol = oldBuf[i];
        final byte newCol = newBuf[i];
        code = (newCol == oldCol) ? INDEX_NO_DIFF : newCol;

        // Perform run-length encoding.
        if (code == currentRunCode) {
          currentRunLength++;
        } else {
          encodeRun(currentRunCode, currentRunLength);
          currentRunLength = 1;
          currentRunCode   = code;
        }
      }
    }
    encodeRun(currentRunCode, currentRunLength);
  }

  /** Closes the underlying OutputStream as well. */
  public synchronized void close() throws IOException {
    if (closed.getAndSet(true))
      return;
    flushMeta();
    dout.close();
  }
}
