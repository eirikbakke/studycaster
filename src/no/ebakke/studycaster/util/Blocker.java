package no.ebakke.studycaster.util;

public class Blocker {
  private final Object  lock = new Object();
  private boolean releasedYet = false;

  public void blockUntilReleased() {
    synchronized (lock) {
      while (!releasedYet) {
        try {
          lock.wait();
        } catch (InterruptedException e) { }
      }
    }
  }

  public void releaseBlockingThreads() {
    synchronized (lock) {
      releasedYet = true;
      lock.notifyAll();
    }
  }
}
