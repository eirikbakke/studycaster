package no.ebakke.studycaster2.screencasting;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class CodecDecoder extends Codec {
  private DataInputStream dis;
  private boolean reachedEOF = false;
  private long currentTimeMillis;

  public CodecDecoder(InputStream is) throws IOException {
    this.dis = new DataInputStream(new BufferedInputStream(new GZIPInputStream(is)));
    int width  = dis.readInt();
    int height = dis.readInt();
    init(new Dimension(width, height));
  }

  public long getCurrentTimeMillis() {
    return currentTimeMillis;
  }

  public BufferedImage nextFrame() throws IOException {
    final byte oldBuf[] = getOldFrame().getBuffer();
    final byte newBuf[] = getNewFrame().getBuffer();
    int  currentRunLength = 0;
    byte currentRunCode   = INDEX_NO_DIFF;
    byte code;

    if (reachedEOF)
      return null;
    try {
      for (int i = 0; i < newBuf.length; i++) {
        if (currentRunLength == 0) {
          try {
            code = dis.readByte();
          } catch (EOFException e) {
            reachedEOF = true;
            if (i > 0)
              throw e;
            return null;
          }
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
      currentTimeMillis = dis.readLong();
    } catch (EOFException e) {
    }

    return swapOutFrame();
  }
}
