package no.ebakke.studycaster.util;

/** See http://en.wikipedia.org/wiki/Moving_average#Application_to_measuring_computer_performance .
Units in milliseconds. */
public class MovingAverage {
  private final double period;
  private double average = 0.0;
  private double lastReading = Double.NEGATIVE_INFINITY;

  public MovingAverage(double period) {
    this.period = period;
  }

  public synchronized void enterReading(double timeSinceLastReading, double newReading) {
    double alpha = 1 - Math.exp(-timeSinceLastReading / period);
    average = alpha * newReading + (1.0 - alpha) * average;
  }

  public synchronized void enterReading(double newReading) {
    double now = System.currentTimeMillis();
    enterReading(now - lastReading, newReading);
    lastReading = now;
  }

  public synchronized double get() {
    return average;
  }
}
