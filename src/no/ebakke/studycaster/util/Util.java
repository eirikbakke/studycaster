package no.ebakke.studycaster.util;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import no.ebakke.studycaster.StudyCaster;
import no.ebakke.studycaster2.StudyCaster2;

public final class Util {
  private Util() { }

  // TODO: Consider whether this should rather be in the StudyCaster class.
  public static boolean fileAvailableExclusive(File f) {
    try {
      FileOutputStream exclusive = new FileOutputStream(f, true);
      exclusive.close();
    } catch (FileNotFoundException e) {
      return false;
    } catch (IOException e) {
      return false;
    }
    return true;
  }

  public static String getPathTo(File f) {
    try {
      return f.getCanonicalPath();
    } catch (IOException e) {
      StudyCaster.log.log(Level.WARNING, "Couldn't get canonical path.", e);
      return f.getAbsolutePath();
    }
  }

  public static void hookupStreams(InputStream is, OutputStream os) throws IOException {
    byte buffer[] = new byte[16 * 1024];
    try {
      int got;
      while ((got = is.read(buffer)) >= 0) {
        os.write(buffer, 0, got);
      }
    } finally {
      is.close();
    }
  }

  public static void logEnvironmentInfo() {
    String propkeys[] = new String[]
      {"java.vendor", "java.version", "java.class.version", "os.name", "os.arch", "os.version", "user.language", "user.region", "user.timezone"};
    StringBuffer props = new StringBuffer();
    boolean first = true;
    for (String key : propkeys) {
      props.append((first ? "" : ", ") + key + "=" + System.getProperty(key));
      first = false;
    }
    StudyCaster2.log.info("Environment: " + props);

    for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
      StudyCaster2.log.info("Found a screen " + gd.getDisplayMode().getWidth() + "x" + gd.getDisplayMode().getHeight()
          + ((gd.equals(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice())) ? " (default)" : ""));
    }
  }

  public static void ensureInterruptible(Interruptible op) {
    boolean interrupted = false;
    while (true) {
      try {
        op.run();
        break;
      } catch (InterruptedException e) {
        interrupted = true;
      }
    }
    if (interrupted)
      Thread.currentThread().interrupt();
  }

  public interface Interruptible {
    public void run() throws InterruptedException;
  }
}
