package no.ebakke.studycaster;

import java.io.BufferedOutputStream;
import java.io.IOException;
import no.ebakke.studycaster.util.stream.NonBlockingOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.Test;

public class NonBlockingOutputStreamTester {
    @Test
    public void testLateConnect1() throws Exception {
      testLateConnect(5000, 5000, 1);
    }

    @Test
    public void test1() throws Exception {
      testParams(5000, 5000, 5000, 1);
    }

    @Test
    public void test2() throws Exception {
      testParams(5000, 5000, 5000, 0);
    }

    @Test
    public void test3() throws Exception {
      testParams(5000, 5000, 123, 0);
    }

    @Test
    public void test4() throws Exception {
      testParams(5000, 5000, 1, 0);
    }

    @Test
    public void test5() throws Exception {
      testParams(1, 500000, 1, 0);
    }

    @Test
    public void test6() throws Exception {
      testParams(1, 500000, 73, 0);
    }

    @Test
    public void test7() throws Exception {
      testParams(1, 50000000, 64 * 1024, 0);
    }

    private void testParams(int minLength, int maxLength, int bufferSize, int delayNanos)
        throws IOException
    {
      ExpectRandomOutputStream eros = new ExpectRandomOutputStream(0, minLength, maxLength);
      eros.setDelayNanos(delayNanos);
      NonBlockingOutputStream os = new NonBlockingOutputStream(bufferSize);
      os.connect(new BufferedOutputStream(eros));
      InputStream is = new RandomInputStream(0, minLength, maxLength);

      System.out.println("Now writing.");
      new RandomHookup(0).hookupStreams(is, os);
      System.out.println("Now closing.");
      os.close();
      System.out.println("Now closed.");
    }

    private void testLateConnect(int minLength, int maxLength, int delayNanos)
        throws IOException
    {
      ExpectRandomOutputStream eros = new ExpectRandomOutputStream(0, minLength, maxLength);
      eros.setDelayNanos(delayNanos);
      NonBlockingOutputStream os = new NonBlockingOutputStream(Integer.MAX_VALUE);
      InputStream is = new RandomInputStream(0, minLength, maxLength);

      System.out.println("Now writing.");
      new RandomHookup(0).hookupStreams(is, os);
      System.out.println("Now connecting.");
      os.connect(eros);
      System.out.println("Now closing.");
      os.close();
      System.out.println("Now closed.");
    }
}
