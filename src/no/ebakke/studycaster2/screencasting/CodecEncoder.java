package no.ebakke.studycaster2.screencasting;
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
import java.util.zip.GZIPOutputStream;

public class CodecEncoder extends Codec {
  private DataOutputStream dout;
  private Robot robot;
  private Rectangle screenRect;

  public CodecEncoder(OutputStream out, Rectangle screenRect) throws IOException, AWTException {
    this.screenRect = screenRect;
    Dimension dim = screenRect.getSize();
    init(dim);
    robot = new Robot();
    dout = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(out)));
    dout.writeUTF(MAGIC_STRING);
    dout.writeInt(dim.width);
    dout.writeInt(dim.height);
  }

  public void captureMouseLocation() {
    addMeta(MetaStamp.Type.TYPE_PERIODIC);
  }

  private void addMeta(MetaStamp.Type type) {
    long time = System.currentTimeMillis();
    PointerInfo pi = MouseInfo.getPointerInfo();
    Point mouseLoc = (pi == null) ? null : pi.getLocation();
    metaStamps.add(new MetaStamp(time, mouseLoc, type));
  }

  private void flushMeta() throws IOException {
    MetaStamp ms;
    while ((ms = metaStamps.poll()) != null) {
      dout.write(MARKER_META);
      ms.writeToStream(dout);
    }
  }

  private final boolean encodeRun(boolean headerWritten, byte code, int runLength) throws IOException {
    if (code == INDEX_REPEAT || runLength < 0)
      throw new AssertionError();
    if (runLength == 0) {
      return headerWritten;
    } else {
      if (!headerWritten) {
        dout.write(MARKER_FRAME);
      }
      if (runLength <= 6) {
        for (int i = 0; i < runLength; i++)
          dout.write(code);
      } else {
        dout.write(code);
        dout.write(INDEX_REPEAT);
        dout.writeInt(runLength - 1);
      }
      return true;
    }
  }

  public synchronized void captureFrame() throws IOException {
    addMeta(MetaStamp.Type.TYPE_BEFORE_CAPTURE);
    BufferedImage image = robot.createScreenCapture(screenRect);
    addMeta(MetaStamp.Type.TYPE_AFTER_CAPTURE);
    flushMeta();
    compressAndOutputFrame(image);
  }

  private void compressAndOutputFrame(BufferedImage frame) throws IOException {
    swapOldNew();
    copyImage(frame, getCurrentFrame());
    final byte oldBuf[] = getPreviousFrame().getBuffer();
    final byte newBuf[] = getCurrentFrame().getBuffer();
    final int width  = getDimension().width;
    final int height = getDimension().height;
    int  currentRunLength = 0;
    byte currentRunCode   = INDEX_NO_DIFF;
    byte code;
    boolean headerWritten = false;

    for (int y = 0, i = 0; y < height; y++) {
      for (int x = 0; x < width; x++, i++) {
        if (newBuf[i] == INDEX_NO_DIFF || newBuf[i] == INDEX_REPEAT)
          throw new AssertionError("Unexpected color index.");
        code = (newBuf[i] == oldBuf[i]) ? INDEX_NO_DIFF : newBuf[i];
        if (code == currentRunCode) {
          currentRunLength++;
        } else {
          headerWritten = encodeRun(headerWritten, currentRunCode, currentRunLength);
          currentRunLength = 1;
          currentRunCode   = code;
        }
      }
    }
    if (!(currentRunCode == INDEX_NO_DIFF && currentRunLength == newBuf.length))
      encodeRun(headerWritten, currentRunCode, currentRunLength);
  }

  public void finish() throws IOException {
    flushMeta();
    dout.close();
  }
}
