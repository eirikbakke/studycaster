package no.ebakke.studycaster.util;

public class Blocker {
  private final Object  lock = new Object();
  private boolean releasedYet = false;

  public void blockUntilReleased(long timeOut) {
    long startTime = System.currentTimeMillis();
    long waitTimeOut;
    synchronized (lock) {
      while (!releasedYet) {
        if (timeOut == 0) {
          waitTimeOut = 0;
        } else {
          waitTimeOut = startTime + timeOut - System.currentTimeMillis();
          if (waitTimeOut < 1)
            break;
        }
        try {
          lock.wait(waitTimeOut);
        } catch (InterruptedException e) { }
      }
    }
  }

  public void blockUntilReleased() {
    blockUntilReleased(0);
  }

  public void releaseBlockingThreads() {
    synchronized (lock) {
      releasedYet = true;
      lock.notifyAll();
    }
  }
}
