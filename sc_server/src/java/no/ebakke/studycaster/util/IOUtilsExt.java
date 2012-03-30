package no.ebakke.studycaster.util;

// Copied from org.apache.commons.io.IOUtils to support RandomAccessFile.
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class IOUtilsExt {
  private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

  public static int copy(InputStream input, RandomAccessFile output) throws IOException {
    long count = copyLarge(input, output);
    if (count > Integer.MAX_VALUE)
      return -1;
    return (int) count;
  }

  public static long copyLarge(InputStream input, RandomAccessFile output)
      throws IOException {
    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    long count = 0;
    int n = 0;
    while (-1 != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }
}
