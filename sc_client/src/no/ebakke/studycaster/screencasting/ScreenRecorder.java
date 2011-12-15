package no.ebakke.studycaster.screencasting;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.ebakke.studycaster.backend.TimeSource;
import no.ebakke.studycaster.screencasting.CaptureScheduler.CaptureTask;
import no.ebakke.studycaster.screencasting.ScreenCensor.CensorType;
import no.ebakke.studycaster.screencasting.jna.DesktopLibrary;
import no.ebakke.studycaster.screencasting.jna.DesktopMeta;
import no.ebakke.studycaster.screencasting.jna.DesktopMetaFactory;
import no.ebakke.studycaster.screencasting.jna.Win32DesktopLibrary;
import no.ebakke.studycaster.util.stream.NonBlockingOutputStream;

/** Thread-safe. */
public class ScreenRecorder {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  private final NonBlockingOutputStream nbos;
  private final CaptureEncoder enc;
  private final DesktopMetaListener desktopMetaListener;
  private final DesktopMetaFactory desktopMetaFactory;
  private final AtomicBoolean stopped = new AtomicBoolean(true);
  private final ScreenRecorderConfiguration config;
  private final ScreenCensor censor;
  private volatile CaptureScheduler pointerRecorder, desktopMetaRecorder, frameRecorder;

  /* ******************************************************************************************** */
  private final CaptureTask pointerRecorderTask = new CaptureTask() {
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

  /* ******************************************************************************************** */
  private final CaptureTask desktopMetaRecorderTask = new CaptureTask() {
    public void capture() {
      // TODO: Implement.
    }

    public double getMaxFrequency() {
      return config.getMaxDesktopMetaSamplingFrequency();
    }

    public double getMaxDutyCycle() {
      return config.getMaxDesktopMetaDutyCycle();
    }

    @Override
    public String toString() {
      return "desktopMetaRecorderTask";
    }
  };
  /* ******************************************************************************************** */

  private final CaptureTask frameRecorderTask = new CaptureTask() {
    public void capture() throws IOException {
      final DesktopMeta desktopMeta =
          (desktopMetaFactory == null ? null : desktopMetaFactory.createMeta());
      final Quilt<CensorType> censorQuilt;
      if (censor == null) {
        censorQuilt = ScreenCensor.NO_CENSOR;
      } else if (desktopMeta == null) {
        censorQuilt = ScreenCensor.ALL_MOSAIC;
      } else {
        censorQuilt = censor.getPermittedRecordingArea(desktopMeta.getWindowList());
      }
      enc.captureFrame(censorQuilt);
    }

    public double getMaxFrequency() {
      double ret = config.getMaxFrameSamplingFrequency();
      if (nbos == null) {
        return ret;
      } else {
        double fillLevel = ((double) (nbos.getBytesPosted() - nbos.getBytesWritten())) /
                           ((double) nbos.getBufferLimitBytes());
        // TODO: Find a less ad-hoc way of doing this.
        if        (fillLevel > 0.8) {
          return Math.min(0.1, ret * 0.1);
        } else if (fillLevel > 0.6) {
          return ret * 0.1;
        } else if (fillLevel > 0.4) {
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
  /* ******************************************************************************************** */

  /** If out is a NonBlockingOutputStream, the ScreenRecorder may use its fill level to influence
  the targeted frame rate. Arguments censor and desktopMetaListener may each be null. */
  public ScreenRecorder(OutputStream out, TimeSource timeSource, ScreenRecorderConfiguration config,
      ScreenCensor censor, DesktopMetaListener desktopMetaListener) throws IOException, AWTException
  {
    this.config = config;
    this.censor = censor;
    this.desktopMetaListener = desktopMetaListener;
    if (censor == null && desktopMetaListener == null) {
      desktopMetaFactory = null;
    } else {
      // TODO: Use an exception instead here.
      DesktopLibrary desktopLibrary = Win32DesktopLibrary.create();
      if (desktopLibrary == null) {
        LOG.log(Level.WARNING,
            "Can't initialize native desktop library; applying mosaic to entire screen");
        desktopMetaFactory = null;
      } else {
        desktopMetaFactory = new DesktopMetaFactory(desktopLibrary, timeSource);
      }
    }
    // An abstraction violation, but works cleanly in any case.
    nbos = (out instanceof NonBlockingOutputStream) ? ((NonBlockingOutputStream) out) : null;
    Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    enc = new CaptureEncoder(out, screenRect);
    enc.setTimeSource(timeSource);
  }

  /** This method will not block unless a previous stop() or close() operation is in progress in a
  different thread. */
  public synchronized void start() {
    if (!stopped.getAndSet(false))
      return;
    LOG.info("Starting screen recorder");
    pointerRecorder = new CaptureScheduler(pointerRecorderTask);
    pointerRecorder.start();
    frameRecorder   = new CaptureScheduler(frameRecorderTask);
    frameRecorder.start();
    if (desktopMetaListener != null) {
      desktopMetaRecorder = new CaptureScheduler(desktopMetaRecorderTask);
      desktopMetaRecorder.start();
    }
  }

  public synchronized void stop() throws IOException {
    if (stopped.getAndSet(true))
      return;
    LOG.info("Stopping screen recorder");
    try {
      pointerRecorder.close();
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Unexpected error while closing pointerRecorder", e);
    } finally {
      try {
        frameRecorder.close();
      } catch (IOException e) {
        LOG.log(Level.SEVERE, "Unexpected error while closing frameRecorder", e);
      } finally {
        if (desktopMetaRecorder != null)
          desktopMetaRecorder.close();
      }
    }
  }

  /** Closes the underlying OutputStream as well. */
  public synchronized void close() throws IOException {
    LOG.info("Closing screen recorder");
    try {
      stop();
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Unexpected error while stopping ScreenRecorder", e);
    } finally {
      enc.close();
    }
  }

  public void forceReportMeta() {
    if (desktopMetaListener != null) {
      // TODO: Implement.
    }
  }

  public interface DesktopMetaListener {
    /** Must be thread-safe. */
    public void reportMeta(DesktopMeta meta);
  }
}
