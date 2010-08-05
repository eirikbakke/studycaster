package no.ebakke.studycaster;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.ebakke.studycaster.util.Blocker;
import no.ebakke.orgstonesoupscreen.DesktopScreenRecorder;
import no.ebakke.orgstonesoupscreen.ScreenRecorder;
import no.ebakke.orgstonesoupscreen.ScreenRecorderListener;
import no.ebakke.studycaster2.NativeLibrary;
import no.ebakke.studycaster2.ServerContext;
import no.ebakke.studycaster2.StreamUtil;

public class StudyCaster {
  private ServerContext serverContext;
  public  static final Logger log = Logger.getLogger("no.ebakke.studycaster");
  private ScreenRecorder recorder;
  private File recordFile;
  private boolean concluded = false;
  private Blocker recorderBlocker = new Blocker();
  private Thread shutdownHook = new Thread(new Runnable() {
    public void run() {
      log.warning("Study not explicitly concluded; concluding via shutdown hook.");
      concludeStudy();
      }
    });

  public ServerContext getServerContext() {
    return serverContext;
  }

  /* Note: URL must point directly to PHP script, end with a slash to use index.php (otherwise POST requests fail). */
  public StudyCaster(String serverURLstring) throws StudyCasterException {
    try {
      try {
        serverContext = new ServerContext(new URI(serverURLstring));
      } catch (URISyntaxException e) {
        throw new StudyCasterException(e);
      }
    } catch (StudyCasterException e) {
      log.log(Level.SEVERE, "Error initializing StudyCaster.", e);
      //log.removeHandler(logHandler);
      throw e;
    }
    Runtime.getRuntime().addShutdownHook(shutdownHook);
  }

  public void concludeStudy() {
    synchronized (this) {
      if (concluded)
        return;
      concluded = true;
    }
    stopRecording();
    if (recordFile != null)
      System.out.println("Recordfile: " + recordFile);
      //recordFile.delete();
    try {
      Runtime.getRuntime().removeShutdownHook(shutdownHook);
    } catch (IllegalStateException e) {
    } catch (SecurityException e) {
    }
    log.info("Concluding study...");
    //log.removeHandler(logHandler);
  }

  @Override
  protected void finalize() {
    concludeStudy();
  }

  public File downloadFile(String remoteName) throws StudyCasterException {
    int dot = remoteName.lastIndexOf(".");
    String extension = (dot == -1) ? "" : remoteName.substring(dot, remoteName.length());
    try {
      File ret = File.createTempFile("sc_", extension);
      try {
        OutputStream os = new FileOutputStream(ret);
        try {
          InputStream is = serverContext.downloadFile(remoteName);
          try {
            StreamUtil.hookupStreams(is, os);
            return ret;
          } finally {
            is.close();
          }
        } finally {
          os.close();
        }
      } catch (IOException e) {
        ret.delete();
        throw e;
      }
    } catch (IOException e) {
      throw new StudyCasterException(e);
    }
  }

  public void uploadFile(File f, String remoteName) throws StudyCasterException {
    try {
      System.out.println("Uploading a file of length " + f.length());
      OutputStream os = serverContext.uploadFile(remoteName);
      try {
        StreamUtil.hookupStreams(new FileInputStream(f), os);
      } finally {
        os.close();
      }
    } catch (IOException e) {
      throw new StudyCasterException(e);
    }
  }

  public void desktopOpenFile(File f, String requiredApp) throws StudyCasterException {
    try {
      Desktop.getDesktop().open(f);
    } catch (IOException e) {
      throw new StudyCasterException("Failed to open the file " + f.getName() + "; do you have " + requiredApp + " installed?", e);
    }
  }

  public void startRecording() throws StudyCasterException {
    if (recorder != null)
      throw new IllegalStateException();

    try {
      System.loadLibrary("libSCNative");
      NativeLibrary.getPermittedRecordingArea(new ArrayList<String>(), true);
    } catch (Exception e) {
      throw new StudyCasterException("Can't initialize window position detector library.", e);
    }

    final OutputStream os;
    try {
      recordFile = File.createTempFile("sc_", ".tmp");
      os = new FileOutputStream(recordFile);
    } catch (IOException e) {
      StudyCaster.log.log(Level.SEVERE, "Can't open temporary file.", e);
      throw new StudyCasterException(e);
    }

    new Thread(new Runnable() {
      int frames = 0;

      public void run() {
        recorder = new DesktopScreenRecorder(os, new ScreenRecorderListener() {
          int frameNum = 0;

          public void frameRecorded(boolean fullFrame) throws IOException {
            log.info("Recorded frame #" + frameNum);
            if (frameNum++ == 0)
              log.info("Recorded the first frame.");

            //System.err.println(recordFile.length());
            if (recordFile.length() > 5000000) {
              log.warning("Recording size passed limit.");
              stopRecording();
            }
          }

          public void recordingStopped() {
            log.info("Recording stopped.");
            recorderBlocker.releaseBlockingThread();
          }
        });
        recorder.startRecording();
      }
    }).start();
  }

  public void stopRecording() {
    if (recorder == null)
      return;
    new Thread(new Runnable() {
      public void run() {
        recorder.stopRecording();
      }
    }).start();
    recorderBlocker.blockUntilReleased(10000);
    recorder = null;
  }

  public File getRecordFile() {
    return recordFile;
  }
}
