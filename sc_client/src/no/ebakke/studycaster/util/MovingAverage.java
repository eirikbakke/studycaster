package no.ebakke.studycaster.util;

/** Thread-safe. See
http://en.wikipedia.org/wiki/Moving_average#Application_to_measuring_computer_performance .
Units in nanoseconds. */
public class MovingAverage {
  private final double periodNanos;
  private double average = 0.0;
  private double lastReadingNanos = Double.NEGATIVE_INFINITY;

  public MovingAverage(double periodNanos) {
    this.periodNanos = periodNanos;
  }

  public synchronized void enterReading(double timeSinceLastReading, double newReading) {
    double alpha = 1 - Math.exp(-timeSinceLastReading / periodNanos);
    average = alpha * newReading + (1.0 - alpha) * average;
  }

  public synchronized void enterReading(double newReading) {
    double now = System.nanoTime();
    enterReading(now - lastReadingNanos, newReading);
    lastReadingNanos = now;
  }

  public synchronized double get() {
    return average;
  }
}
