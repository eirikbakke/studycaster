package no.ebakke.studycaster.util.stream;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.ebakke.studycaster.util.Util;
import org.apache.commons.io.IOExceptionWithCause;

/** An output stream for which calls to flush() and write() return immediately, queueing the
operations for completion in a separate I/O thread. Thread-safe. */
public class NonBlockingOutputStream extends OutputStream {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");

  private final AtomicReference<IOException> storedException =
      new AtomicReference<IOException>(null);
  private final List<StreamProgressObserver> observers = new ArrayList<StreamProgressObserver>();
  private final long bufferLimit;
  /* Don't rely on WriteOpQueue to keep track of remaining bytes, as this may result in a race
  condition. */
  private final AtomicLong bytesWritten    = new AtomicLong(0);
  private final AtomicLong bytesPosted     = new AtomicLong(0);
  private final AtomicBoolean flushPending = new AtomicBoolean(false);
  private final WriteOpQueue pending;
  private final AtomicBoolean closed = new AtomicBoolean(false);
  // Make writerThread volatile in case connect() and close() is called by separate threads.
  private volatile Thread writerThread;

  /* ******************************************************************************************** */
  public NonBlockingOutputStream(long bufferLimit) {
    this.bufferLimit = bufferLimit;
    pending = new WriteOpQueue(bufferLimit);
  }

  public NonBlockingOutputStream() {
    this(Long.MAX_VALUE);
  }

  /** Upon close, out will be closed as well (analogous to a BufferedOutputStream with out as the
  underlying stream). */
  public final void connect(final OutputStream out) {
    if (writerThread != null)
      throw new IllegalStateException("Already connected");
    writerThread = new Thread(new Runnable() {
      public void run() {
        try {
          while (true) {
            byte buf[] = null;
            try {
              buf = pending.remove();
            } catch (InterruptedException e) {
              throw new InterruptedIOException();
            }
            if (buf == null)
              break;
            out.write(buf);
            bytesWritten.addAndGet(buf.length);
            if (bytesWritten.get() == bytesPosted.get() && flushPending.getAndSet(false))
              out.flush();
            notifyObservers();
          }
          /* TODO: Write a unit test that detects the lack of the close or final flush. If the
          following line is removed, the resulting bug can currently only be seen when the entire
          StudyCaster system is run. */
          out.close();
        } catch (IllegalStateException e) {
          setStoredException(new IOExceptionWithCause(e));
        } catch (IOException e) {
          setStoredException(e);
        }
      }
    }, "NonBlockingOutputStream-writer");
    writerThread.start();
  }

  /* ******************************************************************************************** */
  public void addObserver(StreamProgressObserver observer) {
    synchronized (observers) {
      observers.add(observer);
    }
  }

  public void removeObserver(StreamProgressObserver observer) {
    synchronized (observers) {
      observers.remove(observer);
    }
  }

  private void notifyObservers() {
    final List<StreamProgressObserver> toNotify;
    synchronized (observers) {
      toNotify = new ArrayList<StreamProgressObserver>(observers);
    }
    // TODO: Might consider putting the notifications in a separate thread.
    for (final StreamProgressObserver observer : toNotify)
      observer.updateProgress(this);
  }

  /* ******************************************************************************************** */
  @Override
  public void write(int b) throws IOException {
    write(new byte[] {(byte) b}, 0, 1);
  }

  @Override
  public void write(byte b[], int off, int len) throws IOException {
    Util.checkClosed(closed);
    checkStoredException();
    pending.push(b, off, len);
    // Only do this if the push operation succeeds.
    bytesPosted.addAndGet(len);
    /* While it might be appropriate to send a notification to observers at this point, keep things
    simple and just wait until the writer thread gets around to do so instead. */
  }

  private void checkStoredException() throws IOException {
    IOException e = storedException.get();
    if (e != null)
      throw new IOExceptionWithCause("Error in write thread", e);
  }

  private void setStoredException(IOException e) {
    LOG.log(Level.WARNING, "Storing an exception from I/O thread", e);
    // Will only store the first exception. Swallowed exceptions are part of reality, however.
    storedException.compareAndSet(null, e);
  }

  /** Wait for all pending write operations to complete, then close the underlying output stream.
  This method may block. */
  @Override
  public void close() throws IOException {
    if (closed.getAndSet(true))
      return;
    if (writerThread == null) {
      if (getBytesPosted() == getBytesWritten()) {
        return;
      } else {
        throw new IOException("Never connected; data never written");
      }
    }
    Util.ensureInterruptible(new Util.Interruptible() {
      public void run() throws InterruptedException {
        pending.pushEOF();
      }
    });
    Util.ensureInterruptible(new Util.Interruptible() {
      public void run() throws InterruptedException {
        writerThread.join();
      }
    });
    checkStoredException();
    if (getBytesPosted() != getBytesWritten())
      throw new IOException("There were unwritten bytes");
  }

  /** Schedule a flush() to happen on the underlying output stream as soon as the I/O thread becomes
  idle. This method intentionally never blocks, which is contrary to the traditional contract for
  flush. */
  @Override
  public void flush() throws IOException {
    Util.checkClosed(closed);
    flushPending.set(true);
  }

  /* ******************************************************************************************** */
  public long getBufferLimitBytes() {
    return bufferLimit;
  }

  public long getBytesWritten() {
    return bytesWritten.get();
  }

  public long getBytesPosted() {
    return bytesPosted.get();
  }
}
