package org.one.stone.soup.screen.recording.converter;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import org.one.stone.soup.screen.recorder.FrameDecompressor;

public class RecordingStream {
  private Rectangle area;
  private Rectangle outputArea;
  private FrameDecompressor decompressor;
  private long frameTime;
  private boolean finished = false;

  public RecordingStream(InputStream iStream, int width, int height) {
    this(iStream);
    outputArea = new Rectangle(width, height);
  }

  public RecordingStream(InputStream iStream) {
    try {
      int width = iStream.read();
      width = width << 8;
      width += iStream.read();
      int height = iStream.read();
      height = height << 8;
      height += iStream.read();
      area = new Rectangle(width, height);
      outputArea = area;
      decompressor = new FrameDecompressor(iStream, width * height);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public BufferedImage readFrame() throws IOException {
    FrameDecompressor.FramePacket frame = decompressor.unpack();
    frameTime = frame.getTimeStamp();
    int result = frame.getResult();
    if (result == 0) {
      return null;
    } else if (result == -1) {
      finished = true;
      return null;
    }
    BufferedImage bufferedImage = new BufferedImage(area.width,
            area.height, BufferedImage.TYPE_INT_RGB);
    bufferedImage.setRGB(0, 0, area.width, area.height, frame.getData(), 0,
            area.width);
    if (area == outputArea) {
      return bufferedImage;
    }
    BufferedImage scaledBufferedImage = new BufferedImage(outputArea.width,
            outputArea.height, BufferedImage.TYPE_INT_RGB);
    scaledBufferedImage.getGraphics().drawImage(
            bufferedImage.getScaledInstance(outputArea.width,
            outputArea.height, BufferedImage.SCALE_SMOOTH), 0, 0,
            outputArea.width, outputArea.height, null);
    return scaledBufferedImage;
  }

  public Rectangle getArea() {
    return outputArea;
  }

  public long getFrameTime() {
    return frameTime;
  }

  public boolean isFinished() {
    return finished;
  }
}
