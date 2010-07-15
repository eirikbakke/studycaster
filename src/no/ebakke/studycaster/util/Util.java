package no.ebakke.studycaster.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Util {
  private static int BUF_SIZE = 8192;

  public static void streamCopy(InputStream is, OutputStream os) throws IOException {
    byte buf[] = new byte[BUF_SIZE];
    int got;
    while ((got = is.read(buf)) >= 0)
      os.write(buf, 0, got);
  }
}
