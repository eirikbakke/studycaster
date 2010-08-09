package no.ebakke.studycaster;

import java.awt.AWTException;
import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.ebakke.studycaster.util.Util;
import no.ebakke.studycaster2.NonBlockingOutputStream;
import no.ebakke.studycaster2.ServerContext;
import no.ebakke.studycaster2.screencasting.ScreenCensor;
import no.ebakke.studycaster2.screencasting.ScreenRecorder;

public class StudyCaster {
  public static final Logger log = Logger.getLogger("no.ebakke.studycaster");
  private ServerContext serverContext;
  private ScreenRecorder recorder;
  private boolean concluded = false;
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

    try {
      NonBlockingOutputStream os = new NonBlockingOutputStream(4 * 1024 * 1024);
      os.connect(serverContext.uploadFile("screencast.ebc"));
      //OutputStream os = serverContext.uploadFile("screencast.ebc");
      recorder = new ScreenRecorder(os);
    } catch (IOException e) {
      log.log(Level.WARNING, "Failed to initialize screen recorder", e);
    } catch (AWTException e) {
      log.log(Level.WARNING, "Failed to initialize screen recorder", e);
    }
  }

  public void concludeStudy() {
    synchronized (this) {
      if (concluded)
        return;
      concluded = true;
    }
    try {
      waitForScreenCastUpload();
    } catch (StudyCasterException e) {
      log.log(Level.WARNING, "Failed to upload screencast while concluding study", e);
    }
    try {
      Runtime.getRuntime().removeShutdownHook(shutdownHook);
    } catch (IllegalStateException e) {
    } catch (SecurityException e) {
    }
    log.info("Concluding study...");
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
            Util.hookupStreams(is, os);
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
      //System.out.println("Uploading a file of length " + f.length());
      OutputStream os = serverContext.uploadFile(remoteName);
      try {
        Util.hookupStreams(new FileInputStream(f), os);
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

  public void startRecording(ScreenCensor censor) throws StudyCasterException {
    if (recorder != null) {
      recorder.setCensor(censor);
      recorder.start();
    }
  }

  public void stopRecording() {
    if (recorder != null) {
      recorder.stop();
    }
  }

  public void waitForScreenCastUpload() throws StudyCasterException {
    if (recorder != null) {
      try {
        recorder.close();
      } catch (IOException e) {
        throw new StudyCasterException("Error uploading screencast", e);
      } finally {
        recorder = null;
      }
    }
  }
}
