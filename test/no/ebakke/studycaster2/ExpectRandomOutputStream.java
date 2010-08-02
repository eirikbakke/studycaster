package no.ebakke.studycaster2;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Random;
import junit.framework.Assert;

class ExpectRandomOutputStream extends OutputStream {
  private Random rangen;
  private int myAmount;
  private int pos;
  private int delayNanos;

  public void setDelayNanos(int delay) {
    this.delayNanos = delay;
  }

  public ExpectRandomOutputStream(long seed, int length) {
    this(seed, length, length);
  }

  public ExpectRandomOutputStream(long seed, int minLength, int maxLength) {
    this.rangen = new Random(seed);
    this.myAmount = Math.max((int) (rangen.nextDouble() * maxLength), minLength);
    //System.out.println("Expecting " + myAmount);
  }

  @Override
  public void write(int b) throws IOException {
    if (delayNanos > 0) {
      try {
        Thread.sleep(0, delayNanos);
      } catch (InterruptedException e) {
        throw new InterruptedIOException();
      }
    }
    //System.out.println("got a " + b);
    Assert.assertTrue(pos < myAmount);
    byte[] buf = new byte[1];
    rangen.nextBytes(buf);
    //System.out.println(" b is " + b);
    //System.out.println(" buf is " + buf[0]);
    Assert.assertEquals(buf[0], (byte) b);
    pos++;
  }

  @Override
  public void close() throws IOException {
    Assert.assertEquals(myAmount, pos);
  }
}
