package no.ebakke.studycaster2;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class RandomInputStream extends InputStream {
  private int remainingBytes;
  private Random rangen;
  private boolean closed = false;

  public boolean isClosed() {
    return closed;
  }

  public RandomInputStream(long seed, int minLength, int maxLength) {
    this.rangen = new Random(seed);
    this.remainingBytes = Math.max((int) (rangen.nextDouble() * maxLength), minLength);
    //System.out.println("Producing " + remainingBytes);
  }

  @Override
  public int read() throws IOException {
    if (remainingBytes <= 0) {
      //System.out.println("End of stream");
      return -1;
    }
    remainingBytes--;
    byte buf[] = new byte[1];
    rangen.nextBytes(buf);
    return buf[0] & 0xFF;
  }

  @Override
  public void close() throws IOException {
    if (remainingBytes > 0)
      throw new IOException("Closed too early.");
    closed = true;
  }
}
