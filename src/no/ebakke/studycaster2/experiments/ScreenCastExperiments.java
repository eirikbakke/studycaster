package no.ebakke.studycaster2.experiments;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import no.ebakke.studycaster2.screencasting.CodecEncoder;
import no.ebakke.studycaster2.screencasting.ScreenCensor;

public class ScreenCastExperiments {
  public static void main(String args[]) throws Exception {
    Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    Robot r = new Robot();

    // Note: slowest broadband connection sold in Norway has 100kbps upload speed.
    //FileOutputStream fos = new FileOutputStream("z:\\rectest\\outrageous.nc");
    OutputStream os = new FileOutputStream("z:\\rectest\\outrageous.ebc");
    //final NonBlockingOutputStream os = new NonBlockingOutputStream(4 * 1024 * 1024);
    //ServerContext sc = new ServerContext(new URI("http://www.sieuferd.com/studycaster/server.php"));
    //os.connect(sc.uploadFile("screencast.ebc"));
    final CodecEncoder comp = new CodecEncoder(os, screenRect, new ScreenCensor(Arrays.asList(new String[] {"Excel"}), true));

    Thread printThread = new Thread(new Runnable() {
      public void run() {
        try {
          while(true) {
            //System.out.println("Buffer: " + os.getBufferBytes());
            Thread.sleep(100);
          }
        } catch (Exception e) {}
      }
    });
    printThread.start();

    Thread pointerThread = new Thread(new Runnable() {
      public void run() {
        try {
          while(true) {
            Thread.sleep((long) (1000.0 / 15.0));
            comp.captureMouseLocation();
          }
        } catch (Exception e) {}
      }
    });
    pointerThread.start();

    long bef, aft;
    for (int i = 0; i < 30; i++) {
      bef = System.currentTimeMillis();
      comp.captureFrame();
      aft = System.currentTimeMillis();
      System.out.println("capture took " + (aft - bef));
    }
    comp.finish();
    os.close();
    System.out.println("done");
    printThread.interrupt();
    pointerThread.interrupt();
  }
}
