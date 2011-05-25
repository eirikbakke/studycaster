package no.ebakke.studycaster.util.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.swing.SwingUtilities;
import no.ebakke.studycaster.api.StudyCaster;
import no.ebakke.studycaster.util.PipedInputStreamExt;
import no.ebakke.studycaster.util.Util;
import no.ebakke.studycaster.util.Util.Interruptible;

public class NonBlockingOutputStream extends OutputStream {
  private final Object exceptionLock = new Object();
  private IOException storedException;
  private PipedOutputStream outPipe;
  private PipedInputStream inPipe;
  private Thread writerThread;
  private volatile boolean flushDue;
  private int bytesWritten, bufferLimit;
  private final Object observerLock = new Object();
  private List<StreamProgressObserver> observers = new ArrayList<StreamProgressObserver>();

  /* All of this wackyness exists as a workaround for the fact that a PipedOutputStream will only
  work reliably with a single owner thread. */
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
  }, "NonBlockingOutputStream-writeOp");

  public void addObserver(StreamProgressObserver observer) {
    synchronized (observerLock) {
      observers.add(observer);
    }
  }

  public void removeObserver(StreamProgressObserver observer) {
    synchronized (observerLock) {
      observers.remove(observer);
    }
  }

  private void notifyObservers() {
    final List<StreamProgressObserver> toNotify;
    synchronized (observerLock) {
      toNotify = new ArrayList<StreamProgressObserver>(observers);
    }
    final int remaining;
    remaining = getRemainingBytes();
    // TODO: Don't use SwingUtilities here. Clean up this mess.
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        for (StreamProgressObserver observer : toNotify) {
          boolean stillObserving;
          synchronized (observerLock) {
            stillObserving = observers.contains(observer);
          }
          if (stillObserving)
            observer.updateProgress(getWrittenBytes(), remaining);
        }
      }
    });
  }

  @Override
  public void write(int b) throws IOException {
    write(new byte[] {(byte) b}, 0, 1);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
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
    notifyObservers();
    checkStoredException();
  }

  private void checkStoredException() throws IOException {
    synchronized (exceptionLock) {
      if (storedException != null) {
        IOException e = storedException;
        storedException = null;
        throw e;
      }
    }
  }

  private void setStoredException(IOException e) {
    synchronized (exceptionLock) {
      storedException = (storedException != null) ? storedException : e;
    }
  }


  /** Upon close, out will be closed as well (analogous to a BufferedOutputStream with out as the
  underlying stream). */
  public void connect(final OutputStream out) throws IOException {
    if (writerThread != null)
      throw new IOException("Already connected");
    writerThread = new Thread(new Runnable() {
      public void run() {
        try {
          try {
            byte buffer[] = new byte[16 * 1024];
            try {
              int got;
              while ((got = inPipe.read(buffer)) >= 0) {
                out.write(buffer, 0, got);
                synchronized (observerLock) {
                  bytesWritten += got;
                }
                notifyObservers();
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
    }, "NonBlockingOutputStream-writer");
    writerThread.start();
  }

  public int getBufferLimitBytes() {
    return bufferLimit;
  }

  public int getRemainingBytes() {
    try {
      return inPipe.available();
    } catch (IOException e) {
      setStoredException(e);
      return 0;
    }
  }

  public int getWrittenBytes() {
    synchronized (observerLock) {
      return bytesWritten;
    }
  }

  public NonBlockingOutputStream(final OutputStream out, int bufferLimit) {
    this(bufferLimit);
    try {
      connect(out);
    } catch (IOException e) {
      throw new AssertionError("Unexpected exception: " + e.getMessage());
    }
  }

  public NonBlockingOutputStream(int bufferLimit) {
    outPipe = new PipedOutputStream();
    this.bufferLimit = bufferLimit;
    try {
      inPipe = new PipedInputStreamExt(outPipe, bufferLimit);
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
  public void flush() throws IOException {
    /* PipedOutputStream.flush() does not actually seem to block, only notify, so we can use it
    here. */
    outPipe.flush();
    /* Relaxed interpretation of flush; just flush the underlying output stream as soon as we've
    gotten around to write to it. */
    flushDue = true;
  }

  public interface StreamProgressObserver {
    public void updateProgress(int bytesWritten, int bytesRemaining);
  }
}
