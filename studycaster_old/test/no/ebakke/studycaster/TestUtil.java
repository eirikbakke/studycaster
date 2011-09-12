package no.ebakke.studycaster;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class TestUtil {
  private TestUtil() { }

  /** Note, is is automatically closed, os is not. */
  public static void hookupStreams(InputStream is, OutputStream os) throws IOException {
    byte buffer[] = new byte[16 * 1024];
    try {
      int got;
      while ((got = is.read(buffer)) >= 0) {
        os.write(buffer, 0, got);
      }
    } finally {
      is.close();
    }
  }
}
