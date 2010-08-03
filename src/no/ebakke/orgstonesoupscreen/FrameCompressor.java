package no.ebakke.orgstonesoupscreen;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

public class FrameCompressor {
  public class FramePacket {
    private OutputStream oStream;
    private long frameTime;
    private int[] previousData;
    private int[] newData;

    private FramePacket(OutputStream oStream, int frameSize) {
      this.oStream = oStream;
      previousData = new int[frameSize];
    }

    private void nextFrame(int[] frameData, long frameTime, boolean reset) {
      this.frameTime = frameTime;
      previousData = newData;
      newData = null;
      if (previousData == null) {
        previousData = new int[frameData.length];
      }
      if (reset) {
        this.newData = new int[frameData.length];
      } else {
        this.newData = frameData;//new int[frameData.length];
      }
    }
  }
  private FramePacket frame;

  public FrameCompressor(OutputStream oStream, int frameSize) {
    frame = new FramePacket(oStream, frameSize);
  }

  public void pack(int[] newData, long frameTimeStamp, boolean reset) throws IOException {
    frame.nextFrame(newData, frameTimeStamp, reset);

    byte[] packed = new byte[newData.length * 4];

    int inCursor = 0;
    int outCursor = 0;
    int blocks = 0;

    boolean inBlock = true;
    int blockSize = 0;
    byte blockRed = 0;
    byte blockGreen = 0;
    byte blockBlue = 0;

    int blankBlocks = 0;

    int uncompressedCursor = -1;

    byte red;
    byte green;
    byte blue;

    boolean hasChanges = false;
    boolean lastEntry = false;

    //System.out.println("Comparing:"+newData+" to "+frame.previousData);

    while (inCursor < newData.length) {
      if (inCursor == newData.length - 1) {
        lastEntry = true;
      }

      /* Encode difference from previous frame. */
      if (newData[inCursor] == frame.previousData[inCursor]) {
        red = 0;
        green = 0;
        blue = 0;
      } else {
        red = (byte) ((newData[inCursor] & 0x00FF0000) >>> 16);
        green = (byte) ((newData[inCursor] & 0x0000FF00) >>> 8);
        blue = (byte) ((newData[inCursor] & 0x000000FF));

        if (red == 0 && green == 0 && blue == 0) {
          blue = 1;
        }
      }
      /***************************************************************/
      if (blockRed == red && blockGreen == green && blockBlue == blue) {
        if (inBlock == false) {
          if (uncompressedCursor > -1) {
            blocks++;
            hasChanges = true;
            packed[uncompressedCursor] = (byte) (blockSize + 0x80);
          }
          inBlock = true;
          blockSize = 0;
          blankBlocks = 0;
        } else if (blockSize == 126 || lastEntry == true) {
          if (blockRed == 0
                  && blockGreen == 0
                  && blockBlue == 0) {
            if (blankBlocks > 0) {
              blankBlocks++;
              packed[outCursor - 1] = (byte) blankBlocks;
            } else {
              blocks++;
              blankBlocks++;
              packed[outCursor] = (byte) 0xFF;
              outCursor++;
              packed[outCursor] = (byte) blankBlocks;
              outCursor++;
            }
            if (blankBlocks == 255) {
              blankBlocks = 0;
            }
          } else {
            blocks++;
            hasChanges = true;
            packed[outCursor] = (byte) blockSize;
            outCursor++;
            packed[outCursor] = blockRed;
            outCursor++;
            packed[outCursor] = blockGreen;
            outCursor++;
            packed[outCursor] = blockBlue;
            outCursor++;

            blankBlocks = 0;
          }
          inBlock = true;
          blockSize = 0;
        }
      } else {
        if (inBlock == true) {
          if (blockSize > 0) {
            blocks++;
            hasChanges = true;
            packed[outCursor] = (byte) blockSize;
            outCursor++;
            packed[outCursor] = blockRed;
            outCursor++;
            packed[outCursor] = blockGreen;
            outCursor++;
            packed[outCursor] = blockBlue;
            outCursor++;
          }

          uncompressedCursor = -1;
          inBlock = false;
          blockSize = 0;

          blankBlocks = 0;
        } else if (blockSize == 126 || lastEntry == true) {
          if (uncompressedCursor > -1) {
            blocks++;
            hasChanges = true;
            packed[uncompressedCursor] = (byte) (blockSize + 0x80);
          }

          uncompressedCursor = -1;
          inBlock = false;
          blockSize = 0;

          blankBlocks = 0;
        }


        if (uncompressedCursor == -1) {
          uncompressedCursor = outCursor;
          outCursor++;
        }

        packed[outCursor] = red;
        outCursor++;
        packed[outCursor] = green;
        outCursor++;
        packed[outCursor] = blue;
        outCursor++;

        blockRed = red;
        blockGreen = green;
        blockBlue = blue;
      }
      inCursor++;
      blockSize++;
    }
    /***************************************************************/

    /* Now write frame metadata. */

    frame.oStream.write(((int) frame.frameTime & 0xFF000000) >>> 24);
    frame.oStream.write(((int) frame.frameTime & 0x00FF0000) >>> 16);
    frame.oStream.write(((int) frame.frameTime & 0x0000FF00) >>> 8);
    frame.oStream.write(((int) frame.frameTime & 0x000000FF));

    if (hasChanges == false) {
      frame.oStream.write(0);
      frame.oStream.flush();
      frame.newData = frame.previousData;

      return;
    } else {
      frame.oStream.write(1);
      frame.oStream.flush();
    }

    /* Now write out packed (length outCursor) through a GZIPOutputStream. */

    ByteArrayOutputStream bO = new ByteArrayOutputStream();

    byte[] bA;

    GZIPOutputStream zO = new GZIPOutputStream(bO);

    zO.write(packed, 0, outCursor);
    zO.close();
    bO.close();

    bA = bO.toByteArray();

    frame.oStream.write((bA.length & 0xFF000000) >>> 24);
    frame.oStream.write((bA.length & 0x00FF0000) >>> 16);
    frame.oStream.write((bA.length & 0x0000FF00) >>> 8);
    frame.oStream.write((bA.length & 0x000000FF));

    frame.oStream.write(bA);
    frame.oStream.flush();
  }
}
