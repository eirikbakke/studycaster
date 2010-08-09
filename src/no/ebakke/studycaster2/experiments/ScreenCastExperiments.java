package no.ebakke.studycaster2.experiments;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import no.ebakke.studycaster2.screencasting.ScreenCensor;
import no.ebakke.studycaster2.screencasting.ScreenRecorder;

public class ScreenCastExperiments {
  public static void main(String args[]) throws Exception {
    //Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

    // Note: slowest broadband connection sold in Norway has 100kbps upload speed.
    OutputStream os = new FileOutputStream("z:/rectest/localout.ebc");

    //final NonBlockingOutputStream os = new NonBlockingOutputStream(4 * 1024 * 1024);
    //ServerContext sc = new ServerContext(new URI("http://www.sieuferd.com/studycaster/server.php"));
    //os.connect(sc.uploadFile("screencast.ebc"));

    ScreenRecorder sr = new ScreenRecorder(os);
    sr.setCensor(new ScreenCensor(Arrays.asList(new String[] {"Excel"}), true));
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
