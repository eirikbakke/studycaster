package no.ebakke.studycaster2.screencasting;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

public class CodecEncoder extends Codec {
  private DataOutputStream dout;

  public CodecEncoder(OutputStream out, Dimension dim) throws IOException {
    init(dim);
    this.dout = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(out)));
    dout.writeInt(dim.width);
    dout.writeInt(dim.height);
  }

  private final void encodeRun(byte code, int runLength) throws IOException {
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

  public void compressFrame(BufferedImage frame, long timeMillis) throws IOException {
    swapInFrame(frame);
    final byte oldBuf[] = getOldFrame().getBuffer();
    final byte newBuf[] = getNewFrame().getBuffer();
    final int width  = getDimension().width;
    final int height = getDimension().height;
    int  currentRunLength = 0;
    byte currentRunCode   = INDEX_NO_DIFF;
    byte code;

    for (int y = 0, i = 0; y < height; y++) {
      for (int x = 0; x < width; x++, i++) {
        if (newBuf[i] == INDEX_NO_DIFF || newBuf[i] == INDEX_REPEAT)
          throw new AssertionError("Unexpected color index.");

        code = (newBuf[i] == oldBuf[i]) ? INDEX_NO_DIFF : newBuf[i];
        if (code == currentRunCode) {
          currentRunLength++;
        } else {
          encodeRun(currentRunCode, currentRunLength);
          currentRunLength = 1;
          currentRunCode   = code;
        }
      }
    }
    if (!(currentRunLength == newBuf.length && currentRunCode == INDEX_NO_DIFF)) {
      encodeRun(currentRunCode, currentRunLength);
      dout.writeLong(timeMillis);
      dout.flush();
    }
  }
}
