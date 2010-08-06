package no.ebakke.studycaster2.screencasting;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import no.ebakke.studycaster.StatusFrame;

public class CodecDecoder extends Codec {
  private DataInputStream dis;
  private boolean reachedEOF = false, atFrame = false;
  private Image pointerImage;
  private Point pointerImageHotSpot;
  private long currentMetaTime = -1, nextCaptureTime = -1, lastBeforeCaptureTime = -1;

  public CodecDecoder(InputStream is) throws IOException {
    dis = new DataInputStream(new BufferedInputStream(new GZIPInputStream(is)));
    if (!dis.readUTF().equals(MAGIC_STRING))
      throw new IOException("Screencast not in StudyCaster format");
    int width  = dis.readInt();
    int height = dis.readInt();
    init(new Dimension(width, height));

    URL pointerImageURL = StatusFrame.class.getClassLoader().getResource("no/ebakke/studycaster/resources/pointer_shine_weaker.png");
    pointerImage = Toolkit.getDefaultToolkit().getImage(pointerImageURL);
    pointerImageHotSpot = new Point(41,40);
    MediaTracker mt = new MediaTracker(new Canvas());
    mt.addImage(pointerImage, 0);
    try {
      mt.waitForAll();
    } catch (InterruptedException e) {
      throw new InterruptedIOException();
    }
  }

  public long getCurrentTimeMillis() {
    if (currentMetaTime == -1)
      throw new IllegalStateException("No frame retrieved yet.");
    return currentMetaTime;
  }

  private void drawMousePointer(BufferedImage image, Point p) {
    Graphics2D g = image.createGraphics();
    if (!g.drawImage(pointerImage, p.x - pointerImageHotSpot.x, p.y - pointerImageHotSpot.y, null))
      throw new AssertionError("Expected immediate image conversion");
    g.dispose();
  }

  private boolean readUntilFrame() throws IOException {
    MetaStamp ms;
    byte headerMarker;
    while (true) {
      try {
        headerMarker = dis.readByte();
      } catch (EOFException e) {
        return false;
      }
      if        (headerMarker == MARKER_FRAME) {
        return true;
      } else if (headerMarker == MARKER_META) {
        ms = MetaStamp.readFromStream(dis);
        switch (ms.getType()) {
          case        BEFORE_CAPTURE:
            lastBeforeCaptureTime = ms.getTimeMillis();
          break; case AFTER_CAPTURE:
            if (lastBeforeCaptureTime < 0)
              throw new IOException("Missing before-capture timestamp");
            //nextCaptureTime = lastBeforeCaptureTime / 2 + ms.getTimeMillis() / 2;
            // Mouse pointer information seems delayed by 200ms on my machine; may compensate for it here.
            nextCaptureTime = lastBeforeCaptureTime - 200;
            lastBeforeCaptureTime = -1;
          break; case PERIODIC:
        }
        metaStamps.add(ms);
      } else {
        throw new IOException("Invalid header marker.");
      }
    }
  }

  public BufferedImage nextFrame() throws IOException {
    MetaStamp ms;
    BufferedImage ret = null;
    while (ret == null) {
      ms = metaStamps.peek();
      if (ms == null || ms.getTimeMillis() >= nextCaptureTime) {
        if (reachedEOF) {
          return null;
        } else if (atFrame) {
          readFrame();
          atFrame = false;
          continue;
        } else {
          reachedEOF = !readUntilFrame();
          atFrame = true;
          continue;
        }
      }
      metaStamps.remove();
      if (ms.getType() == FrameType.PERIODIC) {
        currentMetaTime = ms.getTimeMillis();
        ret = new ScreenCastImage(getDimension());
        copyImage(getCurrentFrame(), ret);
        if (ms.getMouseLocation() != null)
          drawMousePointer(ret, ms.getMouseLocation());
      }
    }
    return ret;
  }

  private void readFrame() throws IOException {
    final byte oldBuf[] = getPreviousFrame().getBuffer();
    final byte newBuf[] = getCurrentFrame().getBuffer();
    int  currentRunLength = 0;
    byte currentRunCode   = INDEX_NO_DIFF;
    byte code;

    for (int i = 0; i < newBuf.length; i++) {
      if (currentRunLength == 0) {
        code = dis.readByte();
        if (code == INDEX_REPEAT) {
          currentRunLength = dis.readInt();
          if (currentRunLength == 0)
            throw new IOException("Invalid run length");
        } else {
          currentRunLength = 1;
          currentRunCode   = code;
        }
      }
      newBuf[i] = (currentRunCode == INDEX_NO_DIFF) ? oldBuf[i] : currentRunCode;
      currentRunLength--;
    }
    if (currentRunLength > 0)
      throw new IOException("Pixel overflow at end of image (remaining run length > 0)");
    swapOldNew();
  }
}
