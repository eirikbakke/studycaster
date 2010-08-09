package no.ebakke.studycaster.screencasting;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.OutputStream;
import no.ebakke.studycaster.screencasting.CaptureScheduler.CaptureTask;

public class ScreenRecorder {
  private CaptureEncoder enc;
  private boolean stopped = true;
  private IOException storedException;
  private CaptureScheduler pointerRecorder, frameRecorder;
  private CaptureTask pointerRecorderTask = new CaptureTask() {
    public void capture() {
      enc.capturePointer();
    }

    public double getMaxFrequency() {
      return 15.0;
    }

    public double getMaxDutyCycle() {
      return 0.1;
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
      return 5.0;
    }

    public double getMaxDutyCycle() {
      return 0.7;
    }

    @Override
    public String toString() {
      return "frameRecorderTask";
    }
  };

  public ScreenRecorder(OutputStream out) throws IOException, AWTException {
    Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    enc = new CaptureEncoder(out, screenRect);
  }

  public void setCensor(ScreenCensor censor) {
    enc.setCensor(censor);
  }

  public synchronized void start() {
    if (!stopped)
      throw new IllegalStateException("Already started");
    stopped = false;
    pointerRecorder = new CaptureScheduler(pointerRecorderTask);
    frameRecorder   = new CaptureScheduler(frameRecorderTask);
  }


  public synchronized void stop() {
    if (stopped)
      throw new IllegalStateException("Not started");
    stopped = true;
    pointerRecorder.finish();
    frameRecorder.finish();
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
