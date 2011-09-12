package no.ebakke.studycaster.applications;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import no.ebakke.studycaster.api.ServerContext;
import no.ebakke.studycaster.screencasting.RecordingConverter;
import no.ebakke.studycaster.screencasting.ScreenCensor;
import no.ebakke.studycaster.screencasting.ScreenRecorder;
import no.ebakke.studycaster.util.Util;
import org.apache.commons.io.IOUtils;

public class HardCode {
  /*
  private static void readPotentialMarkersFrom(byte buf[], long offset) throws IOException {
    try {
      while (true) {
        CountingInputStream cis = new CountingInputStream(new ByteArrayInputStream(buf, (int) offset, buf.length - (int) offset));
        DataInputStream din = new DataInputStream(cis);
        byte headerMarker = din.readByte();
        long nextOffset = offset + 1;
        if (headerMarker == Codec.MARKER_META) {
          boolean bad = false;
          byte type = din.readByte();
          if (type < 0 || type >= Codec.FrameType.values().length)
            bad = true;
          long time = din.readLong();
          if (time < 1250816790841L || time > 1345424790841L) // Five years before/two years after this code was written.
            bad = true;
          int x = din.readInt();
          int y = din.readInt();
          if (x == Integer.MIN_VALUE && y != Integer.MIN_VALUE)
            bad = true;
          if (x != Integer.MIN_VALUE && (x < 0 || y < 0 || x > 30000 || y > 30000))
            bad = true;
          if (!bad) {
            System.out.println("Potential MARKER_META at " + offset + " with timestamp " + time);
            nextOffset = offset + (int) cis.getByteCount();
            if (din.readByte() == Codec.MARKER_FRAME)
              System.out.println("Potential MARKER_FRAME at " + nextOffset + " (frame data at " + (nextOffset + 1) + ")");
          }
        }
        offset = nextOffset;
      }
    } catch (EOFException e) {
    }
  }

  private static void debugScreenCastFile(String fileName) throws IOException {
    File file = new File(fileName);
    InputStream is = new BufferedInputStream(new FileInputStream(file));
    byte buf[] = new byte[(int) file.length()];
    int offset = 0;
    int numRead = 0;
    while (offset < buf.length && (numRead = is.read(buf, offset, buf.length - offset)) >= 0)
        offset += numRead;
    if (offset < buf.length)
      throw new IOException("Problem reading into buffer");
    readPotentialMarkersFrom(buf, 3714);
  }
  */

  public static void main(String args[]) throws Exception {
    int speedupFactor = 1;
    String confCode = "0391c6e4df35";
    boolean download = false;

    if (download) {
      String fileName = "z:/rectest/downloaded.ebc";
      ServerContext sc = new ServerContext();
      OutputStream fos = new FileOutputStream(fileName);
      IOUtils.copy(sc.downloadFile("uploads/" + confCode + "/screencast.ebc"), fos);
      fos.close();
      //RecordingConverter.convert(new FileInputStream(fileName), "z:/rectest/downconv.mkv", speedupFactor);
    } else {
      //RecordingConverter.convert(new FileInputStream("z:/testdown/" + confCode + "/screencast.ebc"),
      //      "z:/testdown/screencast_hardcode.mkv", speedupFactor);

      //debugScreenCastFile("z:/deletable2/cropped");
      RecordingConverter.convert(new FileInputStream("z:/deletable2/screencast.ebc"), "z:/deletable2/screencast_hardcode.mkv", 1);
    }
  }

  public static void mainScreenCastExperiments(String args[]) throws Exception {
    System.out.println(System.getProperty("java.io.tmpdir"));

    //Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

    // Note: slowest broadband connection sold in Norway has 100kbps upload speed.
    OutputStream os = new FileOutputStream("z:/rectest/localout.ebc");

    //final NonBlockingOutputStream os = new NonBlockingOutputStream(4 * 1024 * 1024);
    //ServerContext sc = new ServerContext();
    //os.connect(sc.uploadFile("screencast.ebc"));

    ScreenRecorder sr = new ScreenRecorder(os, 0);
    sr.setCensor(new ScreenCensor(Arrays.asList(new String[] {"Excel"}), Arrays.asList(new String[] {}), true));
    sr.start();
    Thread.sleep(20000);
    /*
    for (int i = 0; i < 1000; i++) {
      //System.err.println("frame time: " + sr.frameRecorder.avgDuration.get());
      Thread.sleep(100);
    }
    */
    sr.close();
  }
}
