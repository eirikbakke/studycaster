package no.ebakke.studycaster2.screencasting;

import no.ebakke.studycaster.util.MovingAverage;

public class CaptureScheduler {
  private CaptureTask task;
  private boolean finished = false;
  private MovingAverage avgDuration = new MovingAverage(5000.0);
  private Thread captureThread = new Thread(new Runnable() {
    public void run() {
      while (!Thread.interrupted()) {
        double beforeTime = System.currentTimeMillis();
        task.capture();
        double afterTime = System.currentTimeMillis();
        avgDuration.enterReading(afterTime - beforeTime);
        double minDelayDuty = avgDuration.get() * (1.0 / task.getMaxDutyCycle() - 1.0);
        double minDelayFreq = 1000.0 / task.getMaxFrequency() - (afterTime - beforeTime);
        long delay = Math.round(Math.max(minDelayDuty, minDelayFreq));
        if (delay > 0) {
          try {
          Thread.sleep(delay);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      }
    }
  });

  public CaptureScheduler(CaptureTask task) {
    this.task = task;
    captureThread.start();
  }

  public void interrupt() {
    captureThread.interrupt();
  }

  public void finish() {
    if (finished)
      throw new IllegalStateException("Already finished");
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
