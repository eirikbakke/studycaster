package no.ebakke.studycaster.util.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.logging.Level;
import no.ebakke.studycaster.api.StudyCaster;
import no.ebakke.studycaster.util.Util;
import no.ebakke.studycaster.util.Util.Interruptible;

public class NonBlockingOutputStream extends OutputStream {
  private IOException storedException;
  private PipedOutputStream outPipe;
  private PipedInputStream inPipe;
  private Thread writerThread;
  private volatile boolean flushDue;

  /* All of this wackyness exists as a workaround for the fact that a PipedOutputStream will only work reliably with a single owner thread. */
  private byte[] writeOpB;
  private int writeOpOff, writeOpLen;
  private final Object writeOpLock = new Object();
  private Thread writeOpThread = new Thread(new Runnable() {
    public void run() {
      synchronized (writeOpLock) {
        try {
          while (!Thread.interrupted()) {
            while (writeOpB == null)
              writeOpLock.wait();
            try {
              outPipe.write(writeOpB, writeOpOff, writeOpLen);
            } catch (IOException e) {
              storedException = e;
            }
            writeOpB = null;
            writeOpLock.notifyAll();
          }
        } catch (InterruptedException e) {}
      }
    }
  });


  @Override
  public void write(int b) throws IOException {
    write(new byte[] {(byte) b}, 0, 1);
  }

  @Override
  public synchronized void write(byte[] b, int off, int len) throws IOException {
    checkStoredException();
    synchronized (writeOpLock) {
      writeOpB = b;
      writeOpOff = off;
      writeOpLen = len;
      writeOpLock.notifyAll();
      while (writeOpB != null) {
        Util.ensureInterruptible(new Interruptible() {
          public void run() throws InterruptedException {
            writeOpLock.wait();
          }
        });
      }
    }
    checkStoredException();
  }

  private synchronized void checkStoredException() throws IOException {
    if (storedException != null) {
      IOException e = storedException;
      storedException = null;
      throw e;
    }
  }

  private synchronized void setStoredException(IOException e) {
    storedException = (storedException != null) ? storedException : e;
  }


  /** Upon close, out will be closed as well (analogous to a BufferedOutputStream with out as the underlying stream). */
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
          StudyCaster.log.log(Level.WARNING, "Storing an exception from I/O thread", e);
          setStoredException(e);
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
    outPipe = new PipedOutputStream();
    try {
      inPipe = new PipedInputStream(outPipe, bufferLimit);
    } catch (IOException e) {
      throw new RuntimeException("Unexpected exception", e);
    }
    writeOpThread.start();
  }

  @Override
  public void close() throws IOException {
    writeOpThread.interrupt();
    Util.ensureInterruptible(new Interruptible() {
      public void run() throws InterruptedException {
        writeOpThread.join();
      }
    });
    outPipe.close();
    if (writerThread == null)
      throw new IOException("Never connected.");
    Util.ensureInterruptible(new Interruptible() {
      public void run() throws InterruptedException {
        writerThread.join();
      }
    });
    checkStoredException();
  }

  @Override
  public synchronized void flush() throws IOException {
    /* PipedOutputStream.flush() does not actually seem to block, only notify, so we can use it here. */
    outPipe.flush();
    /* Relaxed interpretation of flush; just flush the underlying output stream as soon as we've gotten around to write to it. */
    flushDue = true;
  }
}
