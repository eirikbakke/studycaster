package no.ebakke.studycaster.util.stream;

import java.io.InterruptedIOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import no.ebakke.studycaster.util.Util;

/** Thread-safe. */
class WriteOpQueue {
  private final Lock lock          = new ReentrantLock();
  private final Condition notFull  = lock.newCondition();
  private final Condition notEmpty = lock.newCondition();
  private final long maxBytesInQueue;
  private final AtomicLong bytesInQueue = new AtomicLong(0);
  /* The queue will be externally synchronized by the lock. This has equivalent memory
  synchronization behavior as using the synchronized statement; see the Javadoc for Lock. */
  private final Queue<byte[]> ops = new LinkedList<byte[]>();

  WriteOpQueue(long maxBytesInQueue) {
    if (maxBytesInQueue < 1)
      throw new IllegalArgumentException();
    this.maxBytesInQueue = maxBytesInQueue;
  }

  WriteOpQueue() {
    this(Long.MAX_VALUE);
  }

  public void pushEOF() throws InterruptedException {
    pushOne(null);
  }

  public void push(byte b[], int off, int len) throws InterruptedIOException {
    if (b == null)
      throw new NullPointerException();
    for (int subOff = 0; subOff < len; ) {
      final int subLen = Math.min(len - subOff, (int) Math.min(maxBytesInQueue, Integer.MAX_VALUE));
      try {
        pushOne(Util.copyOfRange(b, off + subOff, off + subOff + subLen));
      } catch (InterruptedException e) {
        InterruptedIOException iioe = new InterruptedIOException();
        iioe.bytesTransferred = subOff;
        throw iioe;
      }
      subOff += subLen;
    }
  }

  private void pushOne(byte[] op) throws InterruptedException {
    lock.lockInterruptibly();
    try {
      if (op != null) {
        while (bytesInQueue.get() + op.length > maxBytesInQueue)
          notFull.await();
        bytesInQueue.addAndGet(op.length);
      }
      ops.add(op);
      notEmpty.signal();
    } finally {
      lock.unlock();
    }
  }

  /** Returns null for the EOF marker. */
  public byte[] remove() throws InterruptedException {
    lock.lockInterruptibly();
    try {
      while (ops.isEmpty())
        notEmpty.await();
      byte[] ret = ops.remove();
      if (ret != null)
        bytesInQueue.addAndGet(-ret.length);
      /* Leave it to the condition loop in the waiting thread to determine if the condition is
      actually satisfied. */
      notFull.signal();
      return ret;
    } finally {
      lock.unlock();
    }
  }
}
