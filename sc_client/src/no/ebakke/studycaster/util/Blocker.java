package no.ebakke.studycaster.util;

// TODO: Get rid of this class.
public class Blocker {
  private final Object lock   = new Object();
  private boolean releasedYet = false;
  private boolean blockedYet  = false;

  public void blockUntilReleased(long timeOut) throws InterruptedException {
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
        lock.wait(waitTimeOut);
      }
    }
  }

  public void blockUntilReleased() throws InterruptedException {
    blockUntilReleased(0);
  }

  public void releaseBlockingThread() {
    synchronized (lock) {
      releasedYet = true;
      lock.notify();
    }
  }
}
