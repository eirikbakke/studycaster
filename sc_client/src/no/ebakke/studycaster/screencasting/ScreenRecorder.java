package no.ebakke.studycaster.screencasting;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;
import no.ebakke.studycaster.screencasting.CaptureScheduler.CaptureTask;
import no.ebakke.studycaster.util.stream.NonBlockingOutputStream;

public class ScreenRecorder {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  private NonBlockingOutputStream nbos;
  private CaptureEncoder enc;
  private boolean stopped = true;
  private IOException storedException;
  private CaptureScheduler pointerRecorder, frameRecorder;
  private ScreenRecorderConfiguration config;

  private CaptureTask pointerRecorderTask = new CaptureTask() {
    public void capture() {
      enc.capturePointer();
    }

    public double getMaxFrequency() {
      return config.getMaxPointerSamplingFrequency();
    }

    public double getMaxDutyCycle() {
      return config.getMaxPointerDutyCycle();
    }

    @Override
    public String toString() {
      return "pointerRecorderTask";
    }
  };
  private CaptureTask frameRecorderTask = new CaptureTask() {
    public void capture() {
      try {
        enc.captureFrame();
      } catch (IOException e) {
        pointerRecorder.interrupt();
        frameRecorder.interrupt();
        storedException = e;
      }
    }

    public double getMaxFrequency() {
      double ret = config.getMaxFrameSamplingFrequency();
      if (nbos == null) {
        return ret;
      } else {
        // TODO: Find a less ad-hoc way of doing this.
        double fillLevel =
            ((double) nbos.getRemainingBytes()) / ((double) nbos.getBufferLimitBytes());
        if (fillLevel > 0.9) {
          return 0.0;
        } else if (fillLevel > 0.75) {
          return ret * 0.1;
        } else if (fillLevel > 0.50) {
          return ret * 0.2;
        } else {
          return ret;
        }
      }
    }

    public double getMaxDutyCycle() {
      return config.getMaxFrameDutyCycle();
    }

    @Override
    public String toString() {
      return "frameRecorderTask";
    }
  };

  public ScreenRecorder(OutputStream out, long serverSecondsAhead,
      ScreenRecorderConfiguration config) throws IOException, AWTException
  {
    this.config = config;
    // TODO: Get rid of this abstraction violation.
    if (out instanceof NonBlockingOutputStream)
      nbos = (NonBlockingOutputStream) out;
    Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    enc = new CaptureEncoder(out, screenRect);
    enc.setServerSecondsAhead(serverSecondsAhead);
  }

  public synchronized void setCensor(ScreenCensor censor) {
    enc.setCensor(censor);
  }

  public synchronized void start() {
    if (!stopped) {
      LOG.warning("Already started");
      return;
    }
    stopped = false;
    pointerRecorder = new CaptureScheduler(pointerRecorderTask);
    frameRecorder   = new CaptureScheduler(frameRecorderTask);
  }


  public void stop() {
    /* TODO: Remove these logging messages once we're confident that the deadlock bug has been
    resolved. */
    LOG.info("ScreenRecorder.stop() called");
    synchronized (this) {
      if (stopped) {
        LOG.warning("Already stopped");
        return;
      }
      stopped = true;
    }
    LOG.info("Calling pointerRecorder.finish()");
    pointerRecorder.finish();
    LOG.info("Calling frameRecorder.finish()");
    frameRecorder.finish();
    LOG.info("ScreenRecorder.stop() completed");
  }

  /** Closes the underlying OutputStream as well. */
  public void close() throws IOException {
    if (!stopped)
      stop();
    if (storedException != null)
      throw storedException;
    enc.finish();
  }
}
