package no.ebakke.studycaster.screencasting;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.ebakke.studycaster.util.MovingAverage;
import no.ebakke.studycaster.util.Util;

/** Thread-safe. */
public class CaptureScheduler {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  private final MovingAverage avgDuration;
  private final AtomicBoolean started   = new AtomicBoolean(false);
  private final AtomicBoolean finished  = new AtomicBoolean(false);
  private final Thread captureThread;
  private final Lock interruptLock = new ReentrantLock();
  private volatile IOException storedException;
  private final AtomicLong lastCapture = new AtomicLong(Long.MIN_VALUE);

  public CaptureScheduler(final CaptureTask task) {
    captureThread = new Thread(new Runnable() {
      public void run() {
        while (true) {
          final long beforeTime, afterTime;
          /* Give the thread running close() a way to wait with its interrupt until the last
          capture has completed successfully. Otherwise the underlying I/O operations might fail
          with an InterruptedIOException in rare cases. */
          interruptLock.lock();
          try {
            if (Thread.interrupted())
              break;
            beforeTime = System.nanoTime();
            task.capture();
            afterTime = System.nanoTime();
            registerCaptureTime(afterTime);
          } catch (IOException e) {
            LOG.log(Level.WARNING, "Storing an exception in CaptureScheduler", e);
            storedException = e;
            break;
          } finally {
            interruptLock.unlock();
          }

          final double duration = afterTime - beforeTime;
          avgDuration.enterReading(duration);
          final double minDelayDuty = avgDuration.get() * (1.0 / task.getMaxDutyCycle() - 1.0);
          final double minDelayFreq = 1000000000.0 / task.getMaxFrequency() - duration;
          try {
            while (Util.delayAtLeastUntil(
                lastCapture.get() + Math.round(Math.max(minDelayDuty, minDelayFreq))))
            {
              // No action.
            }
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      }
    }, "CaptureScheduler-capture-" + task.toString());
    // Average duty cycle over a longer time if the frequency is low.
    avgDuration = new MovingAverage(30000.0 * 1000000.0 / task.getMaxFrequency());
  }

  private void registerCaptureTime(long nanoTime) {
    Util.atomicSetMax(lastCapture, nanoTime);
  }

  /** May be called by the client to indicate an externally handled capture that should cause
  scheduled captures to be delayed. */
  public void registerCaptureTime() {
    registerCaptureTime(System.nanoTime());
  }

  /* This used to happen automatically in the constructor, which is dangerous. See
  http://www.ibm.com/developerworks/java/library/j-jtp0618/index.html . */
  public void start() {
    if (started.getAndSet(true))
      throw new IllegalStateException("Already started");
    captureThread.start();
  }

  public void close() throws IOException {
    if (finished.getAndSet(true))
      return;

    interruptLock.lock();
    try {
      captureThread.interrupt();
    } finally {
      interruptLock.unlock();
    }

    Util.ensureInterruptible(new Util.Interruptible() {
      public void run() throws InterruptedException {
        captureThread.join();
      }
    });

    if (storedException != null)
      throw storedException;
  }

  public interface CaptureTask {
    public void capture() throws IOException;
    public double getMaxFrequency();
    public double getMaxDutyCycle();
  }
}
