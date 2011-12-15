package no.ebakke.studycaster.screencasting;

import java.awt.Dimension;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Thread-safe. */
final class CodecState {
  private final Queue<CodecMeta> codecMetaStamps = new ConcurrentLinkedQueue<CodecMeta>();
  private final Dimension dimension;
  private volatile ScreenCastImage currentFrame, previousFrame;

  CodecState(Dimension dimension) {
    this.dimension     = dimension;
    this.currentFrame  = new ScreenCastImage(dimension);
    this.previousFrame = new ScreenCastImage(dimension);
  }

  public synchronized void swapFrames() {
    ScreenCastImage tmp = previousFrame;
    previousFrame = currentFrame;
    currentFrame = tmp;
  }

  public Dimension getDimension() {
    return new Dimension(dimension);
  }

  public ScreenCastImage getPreviousFrame() {
    return previousFrame;
  }

  public ScreenCastImage getCurrentFrame() {
    return currentFrame;
  }

  public void addCodecMeta(CodecMeta meta) {
    codecMetaStamps.add(meta);
  }

  public CodecMeta pollCodecMeta() {
    return codecMetaStamps.poll();
  }

  // TODO: Get rid of this method after the call site is rewritten.
  public CodecMeta peekCodecMeta() {
    return codecMetaStamps.peek();
  }
}
