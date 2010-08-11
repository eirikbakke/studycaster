package no.ebakke.studycaster.screencasting;

import no.ebakke.studycaster.api.StudyCaster;
import no.ebakke.studycaster.util.MovingAverage;

public class CaptureScheduler {
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
  });

  public CaptureScheduler(CaptureTask task) {
    this.task = task;
    captureThread.setName("capture-" + task.toString());
    captureThread.start();
  }

  public void interrupt() {
    captureThread.interrupt();
  }

  public void finish() {
    if (finished) {
      StudyCaster.log.warning("Already finished");
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
