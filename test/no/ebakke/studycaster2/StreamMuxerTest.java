package no.ebakke.studycaster2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.Test;

public class StreamMuxerTest {
  private static final int DATA_AMOUNT = 1000000;
  private static final int NO_THREADS = 255;

  @Test
  public void testBaseFunctionality() throws Exception {

    File tempFile = File.createTempFile("StreamMuxerTest", ".tmp");
    //System.out.println(tempFile.getCanonicalPath());
    tempFile.deleteOnExit();
    StreamMuxer underTest = new StreamMuxer(new FileOutputStream(tempFile));
    List<Thread> threads = new ArrayList<Thread>();
    List<TestDataProducer> testDataProducers = new ArrayList<TestDataProducer>();
    for (int i = 0; i < NO_THREADS; i++) {
      TestDataProducer tdp = new TestDataProducer(i, underTest, "testStream-" + Long.toString(i));
      threads.add(new Thread(tdp));
      testDataProducers.add(tdp);
    }
    for (Thread t : threads)
      t.start();
    for (Thread t : threads) {
      t.join();
    }
    underTest.close();
    for (TestDataProducer tdp : testDataProducers) {
      Exception e = tdp.getFailed();
      if (e != null)
        throw e;
    }

    Map<String, OutputStream> outputStreams = new LinkedHashMap<String, OutputStream>();
    for (int i = 0; i < NO_THREADS; i++)
      outputStreams.put("testStream-" + Long.toString(i), new ExpectRandomOutputStream(i, 0, DATA_AMOUNT));
    InputStream is = new FileInputStream(tempFile);
    StreamMuxer.demux(is, outputStreams);
    for (OutputStream os : outputStreams.values())
      os.close();
    is.close();
  }

  private class TestDataProducer implements Runnable {
    private Random rangen;
    private StreamMuxer muxer;
    private OutputStream target;
    private String streamName;
    private int myAmount;
    private Exception failed = null;

    public Exception getFailed() {
      return failed;
    }

    public TestDataProducer(long seed, StreamMuxer muxer, String streamName) {
      this.rangen = new Random(seed);
      this.myAmount = Math.max((int) (rangen.nextDouble() * DATA_AMOUNT), 0);
      //this.myAmount = 2;
      this.muxer  = muxer;
      this.streamName = streamName;
    }

    public void run() {
      waitRandom();
      try {
        target = muxer.createOutputStream(streamName);
        int pos = 0;
        byte buf[] = new byte[myAmount];
        for (int i = 0; i < buf.length; i++) {
          byte buf2[] = new byte[1];
          rangen.nextBytes(buf2);
          buf[i] = buf2[0];
        }
        //rangen.nextBytes(buf);
        while (pos < myAmount) {
          int doNow = Math.max(1, Math.min((int) (Math.random() * myAmount), myAmount - pos));
          //System.out.println("Thread " + streamName + " writing " + doNow);
          target.write(buf, pos, doNow);
          pos += doNow;
          waitRandom();
        }
        target.close();
      } catch (Exception e) {
        failed = e;
      }
    }

    public void waitRandom() {
      try {
        Thread.sleep((long) (Math.random() * 10));
      } catch (InterruptedException e) {
      }
    }
  }
}