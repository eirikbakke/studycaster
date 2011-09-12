package no.ebakke.studycaster.util.stream;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/** A PipedInputStream with a variable buffer size. As of Java 1.6, similar functionality exists in
 the standard class library, but we are targeting for 1.5. */
public class PipedInputStreamExt extends PipedInputStream {
  public PipedInputStreamExt(PipedOutputStream src, int pipeSize) throws IOException {
    this(pipeSize);
    connect(src);
  }

  public PipedInputStreamExt(int pipeSize) {
    super();
    if (pipeSize <= 0)
      throw new IllegalArgumentException("Pipe Size <= 0");
    buffer = new byte[pipeSize];
  }
}
