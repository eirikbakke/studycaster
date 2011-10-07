package no.ebakke.studycaster;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

public class RandomHookup {
  private Random rangen;
  private int MAX_BUF_SZ = 18000;

  public RandomHookup(long seed) {
    this.rangen = new Random(seed);
  }

  /** Note, is is automatically closed, os is not. */
  public void hookupStreams(InputStream is, OutputStream os) throws IOException {
    byte buffer[] = new byte[MAX_BUF_SZ];
    try {
      int got;
      do {
        int toRead;
        if (rangen.nextInt() % 5 == 0) {
          toRead = 1;
        } else {
          toRead = rangen.nextInt(MAX_BUF_SZ);
        }
        got = is.read(buffer, 0, toRead);
        if (got < 0)
          break;
        os.write(buffer, 0, got);
      } while (true);
    } finally {
      is.close();
    }
  }
}
