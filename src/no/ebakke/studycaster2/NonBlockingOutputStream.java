package no.ebakke.studycaster2;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class NonBlockingOutputStream extends PipedOutputStream {
  private IOException storedException;
  private PipedInputStream inPipe;
  private Thread writerThread;
  private volatile boolean flushDue;

  public void connect(final OutputStream out) {
    if (writerThread != null)
      throw new IllegalStateException("Already connected");
    writerThread = new Thread(new Runnable() {
      public void run() {
        try {
          try {
            byte buffer[] = new byte[16 * 1024];
            try {
              int got;
              while ((got = inPipe.read(buffer)) >= 0) {
                out.write(buffer, 0, got);
                if (flushDue && inPipe.available() == 0) {
                  out.flush();
                  flushDue = false;
                }
              }
            } finally {
              inPipe.close();
            }
          } finally {
            out.close();
          }
        } catch (IOException e) {
          storedException = e;
        }
      }
    });
    writerThread.start();
  }

  public int getBufferBytes() throws IOException {
    return inPipe.available();
  }

  public NonBlockingOutputStream(final OutputStream out, int bufferLimit) {
    this(bufferLimit);
    connect(out);
  }

  public NonBlockingOutputStream(int bufferLimit) {
    try {
      inPipe = new PipedInputStream(this, bufferLimit);
    } catch (IOException e) {
      throw new AssertionError("Unexpected IOException: " + e.getMessage());
    }
  }

  @Override
  public void close() throws IOException {
    super.close();
    if (writerThread == null)
      throw new IOException("Never connected.");
    try {
      writerThread.join();
    } catch (InterruptedException e) {
      throw new InterruptedIOException();
    }
    if (storedException != null)
      throw storedException;
  }

  @Override
  public synchronized void flush() throws IOException {
    /* PipedOutputStream.flush() does not actually seem to block, only notify, so we can keep the default implementation. */
    super.flush();
    /* Relaxed interpretation of flush; just flush the underlying output stream as soon as we've gotten around to write to it. */
    flushDue = true;
  }
}
