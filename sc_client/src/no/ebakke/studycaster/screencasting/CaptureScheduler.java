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
  private static final int CAPTURE_AVERAGE_PERIODS = 30;
  private final MovingAverage avgDuration;
  private final AtomicBoolean started  = new AtomicBoolean(false);
  private final AtomicBoolean finished = new AtomicBoolean(false);
  private final CaptureTask task;
  private final Lock captureLock = new ReentrantLock();
  /* The following fields may only be updated while holding captureLock, but may be read by any
  thread at any time. */
  private final AtomicLong lastCapture = new AtomicLong(Long.MIN_VALUE);
  private volatile IOException storedException;

  private final Thread captureThread = new Thread(new Runnable() {
    public void run() {
      try {
        while (true) {
          final Long duration = oneCapture(task);
          if (duration == null)
            break;
          /* The maximum duty cycle is taken as a per-thread limit without regard to the number of
          processor cores available. Otherwise a curious user with several cores might notice 100%
          utilization in one core and mistake it for an error in the StudyCaster client, even if a
          maximum duty cycle is set. */
          final double minDelayDuty = avgDuration.get() * (1.0 / task.getMaxDutyCycle() - 1.0);
          final double minDelayFreq = 1000000000.0 / task.getMaxFrequency() - duration;
          while (Util.delayAtLeastUntil(
              lastCapture.get() + Math.round(Math.max(minDelayDuty, minDelayFreq))))
          {
            // No action, but recompute delay after each sleep, since lastCapture may change.
          }
        }
      } catch (InterruptedException e) {
        // Exit thread.
      }
    }
  });

  public CaptureScheduler(final CaptureTask task) {
    this.task = task;
    captureThread.setName("CaptureScheduler-capture-" + task.toString());
    avgDuration = new MovingAverage(
        CAPTURE_AVERAGE_PERIODS * (1000000000.0 / task.getMaxFrequency()));
  }

  /** Returns the duration of the operation in nanoseconds, or null if a stored error occurred or
  the CaptureScheduler has been closed. */
  private Long oneCapture(final CaptureTask task) throws InterruptedException {
    captureLock.lock();
    try {
      /* Check for this condition now, before starting any I/O operations. See the corresponding
      call to interrupt() in close(). */
      if (Thread.interrupted())
        throw new InterruptedException();
      if (storedException != null || finished.get())
        return null;
      final long beforeTime = System.nanoTime();
      task.capture();
      final long afterTime = System.nanoTime();
      // Doesn't strictly need to be atomic, but let's be safe.
      Util.atomicSetMax(lastCapture, afterTime);
      final long duration = afterTime - beforeTime;
      avgDuration.enterReading(duration);
      return duration;
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Storing an exception in CaptureScheduler", e);
      if (storedException != null)
        throw new AssertionError();
      storedException = e;
      return null;
    } finally {
      captureLock.unlock();
    }
  }

  public void forceCaptureNow() {
    try {
      // Ignore return value.
      oneCapture(task);
    } catch (InterruptedException e) {
      /* An interrupt detected here would be unrelated to any interrupt triggered by close(), since
      this function should only be called from client threads. */
      Thread.currentThread().interrupt();
    }
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

    /* Wait with the interrupt until the last capture has completed successfully. Otherwise the
    underlying I/O operations might fail with an InterruptedIOException in rare cases. */
    captureLock.lock();
    try {
      captureThread.interrupt();
    } finally {
      captureLock.unlock();
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
