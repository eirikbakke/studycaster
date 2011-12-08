package no.ebakke.studycaster.backend;

/** High-resolution timer guaranteed to increase monotonically, and which returns real time values.
Thread-safe. */
public final class TimeSource {
  private final long offsetNanos;

  public TimeSource() {
    this.offsetNanos = System.nanoTime() - System.currentTimeMillis() * 1000000L;
  }

  @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
  public TimeSource(TimeSource source, long timeAhead) {
    this.offsetNanos = source.offsetNanos - timeAhead;
  }

  public long currentTimeNanos() {
    return System.nanoTime() - offsetNanos;
  }

  public long currentTimeMillis() {
    return Math.round(currentTimeNanos() / 1000000.0);
  }
}
