package no.ebakke.studycaster.util;

public class Blocker {
  private final Object lock = new Object();
  private boolean releasedYet = false;
  private boolean blockedYet = false;

  public void blockUntilReleased(long timeOut) {
    synchronized (lock) {
      long startTime = System.currentTimeMillis();
      long waitTimeOut;
      if (blockedYet)
        throw new IllegalStateException("Expected only one blocking thread.");
      blockedYet = true;
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
        } catch (InterruptedException e) {
          // TODO: Rethrow.
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  public void blockUntilReleased() {
    blockUntilReleased(0);
  }

  public void releaseBlockingThread() {
    synchronized (lock) {
      releasedYet = true;
      lock.notify();
    }
  }
}
