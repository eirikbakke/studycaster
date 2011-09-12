package no.ebakke.studycaster;

import no.ebakke.studycaster.util.stream.NonBlockingOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.Test;

public class NonBlockingOutputStreamTester {
    @Test
    public void testPostStream() throws Exception {
      ExpectRandomOutputStream eros = new ExpectRandomOutputStream(0, 5000);
      eros.setDelayNanos(1);
      OutputStream os = new NonBlockingOutputStream(eros, 50000);
      InputStream is = new RandomInputStream(0, 5000);

      System.out.println("Now writing.");
      TestUtil.hookupStreams(is, os);
      System.out.println("Now closing.");
      os.close();
      System.out.println("Now closed.");
    }
}
