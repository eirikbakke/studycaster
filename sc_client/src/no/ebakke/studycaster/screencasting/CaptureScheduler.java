package no.ebakke.studycaster.screencasting;

import java.util.logging.Logger;
import no.ebakke.studycaster.util.MovingAverage;

public class CaptureScheduler {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  private CaptureTask task;
  private boolean finished = false;
  private MovingAverage avgDuration = new MovingAverage(5000.0);
  private Thread captureThread = new Thread(new Runnable() {
    public void run() {
      try {
        while (true) {
          double beforeTime = System.currentTimeMillis();
          task.capture();
          double afterTime = System.currentTimeMillis();
          avgDuration.enterReading(afterTime - beforeTime);
          while (true) {
            double minDelayDuty = avgDuration.get() * (1.0 / task.getMaxDutyCycle() - 1.0);
            double minDelayFreq = 1000.0 / task.getMaxFrequency() - (afterTime - beforeTime);
            double remainingDelay = Math.max(minDelayDuty, minDelayFreq) - (System.currentTimeMillis() - afterTime);
            long delay = Math.round(remainingDelay);
            if (delay > 0) {
              Thread.sleep(Math.min(delay, 500));
            } else {
              break;
            }
          }
        }
      } catch (InterruptedException e) { }
    }
  }, "CaptureScheduler-capture");

  public CaptureScheduler(CaptureTask task) {
    this.task = task;
    captureThread.setName("capture-" + task.toString());
  }

  /* This used to happen automatically in the constructor, which is dangerous. See
  http://www.ibm.com/developerworks/java/library/j-jtp0618/index.html . */
  public void start() {
    captureThread.start();
  }

  public void interrupt() {
    captureThread.interrupt();
  }

  public void finish() {
    if (finished) {
      LOG.warning("Already finished");
      return;
    }
    finished = true;
    interrupt();
    boolean interrupted = false;
    while (true) {
      try {
        captureThread.join();
        break;
      } catch (InterruptedException e) {
        interrupted = true;
      }
    }
    if (interrupted)
      Thread.currentThread().interrupt();
  }

  public interface CaptureTask {
    public void capture();
    public double getMaxFrequency();
    public double getMaxDutyCycle();
  }
}
