package no.ebakke.orgstonesoupscreen;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import no.ebakke.orgstonesoupscreen.FrameDecompressor;

public class RecordingStream {
  private Rectangle area;
  private FrameDecompressor decompressor;
  private long frameTime;
  private boolean finished = false;
  private int[] frameData;

  public RecordingStream(InputStream iStream) {
    try {
      int width = iStream.read();
      width = width << 8;
      width += iStream.read();
      int height = iStream.read();
      height = height << 8;
      height += iStream.read();
      area = new Rectangle(width, height);
      decompressor = new FrameDecompressor(iStream, width * height);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public BufferedImage readFrame() throws IOException {
    if (readFrameData())
      return null;
    BufferedImage bufferedImage = new BufferedImage(area.width, area.height, BufferedImage.TYPE_INT_RGB);
    bufferedImage.setRGB(0, 0, area.width, area.height, getFrameData(), 0, area.width);
    return bufferedImage;
  }

  public boolean readFrameData() throws IOException {
    FrameDecompressor.FramePacket frame = decompressor.unpack();
    frameTime = frame.getTimeStamp();
    frameData = frame.getData();
    int result = frame.getResult();
    if (result == 0) {
      return true;
    } else if (result == -1) {
      finished = true;
      return true;
    }
    return false;
  }

  public int[] getFrameData() {
    return frameData;
  }

  public Rectangle getArea() {
    return area;
  }

  public long getFrameTime() {
    return frameTime;
  }

  public boolean isFinished() {
    return finished;
  }
}
