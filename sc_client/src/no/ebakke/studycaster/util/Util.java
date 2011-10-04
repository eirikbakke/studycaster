package no.ebakke.studycaster.util;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import no.ebakke.studycaster.api.StudyCasterException;

public final class Util {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  private Util() { }

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

  public static String getPathString(File f) {
    try {
      return f.getCanonicalPath();
    } catch (IOException e) {
      LOG.log(Level.WARNING, "Couldn''t get canonical path.", e);
      return f.getAbsolutePath();
    }
  }

  public static void logEnvironmentInfo() {
    String propkeys[] = new String[]
      {"java.vendor", "java.version", "java.class.version", "os.name", "os.arch", "os.version",
      "user.language", "user.region", "user.timezone"};
    StringBuffer props = new StringBuffer();
    boolean first = true;
    for (String key : propkeys) {
      props.append((first ? "" : ", ") + key + "=" + System.getProperty(key));
      first = false;
    }
    LOG.log(Level.INFO, "Environment: {0}", props);

    for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
      LOG.log(Level.INFO, "Found a screen {0}x{1}{2}",
          new Object[]{gd.getDisplayMode().getWidth(), gd.getDisplayMode().getHeight(),
          (gd.equals(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice())) ?
          " (default)" : ""});
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

  public static void executeShellCommand(String command[]) throws StudyCasterException {
    try {
      Process proc = Runtime.getRuntime().exec(command);
      if (proc.waitFor() != 0)
        throw new StudyCasterException("Got non-zero exit value while executing shell command");
    } catch (IOException e) {
      throw new StudyCasterException("Got IOException while executing shell command", e);
    } catch (InterruptedException e) {
      throw new StudyCasterException("Interrupted while executing shell command", e);
    }
  }

  public interface Interruptible {
    public void run() throws InterruptedException;
  }

  public static void desktopOpenFile(File fileToOpen, String errorMessage)
      throws StudyCasterException
  {
    Class<?> desktopClass = null;
    try {
      desktopClass = Class.forName("java.awt.Desktop");
    } catch (ClassNotFoundException e) { }
    if (desktopClass != null) {
      try {
        Method getDesktopMethod = desktopClass.getMethod("getDesktop");
        Object desktopObject = getDesktopMethod.invoke(null);
        if (desktopObject == null || !(desktopClass.isInstance(desktopObject))) {
          throw new StudyCasterException(
              "Unexpected return value from Desktop.getDesktop(): " + desktopObject);
        }
        Method openMethod = desktopClass.getMethod("open", File.class);
        openMethod.invoke(desktopObject, fileToOpen);
        return;
      } catch (NoSuchMethodException e) {
        throw new StudyCasterException("Failed to locate an expected library method", e);
      } catch (IllegalAccessException e) {
        throw new StudyCasterException("Failed to invoke a library method", e);
      } catch (InvocationTargetException e) {
        if (e.getCause() instanceof RuntimeException)
          throw (RuntimeException) e.getCause();
        if (e.getCause() instanceof Error)
          throw (Error) e.getCause();
        if (e.getCause() instanceof IOException) {
          // TODO: Take whole string (with pattern for file name) from configuration.
          throw new StudyCasterException("Failed to open the file " + fileToOpen.getName() + "; " +
              errorMessage, e);
        }
      }
    } else {
      LOG.info("Did not find Java Desktop API, using platform-specific " +
          "implementation instead (probably on JRE 1.5 or earlier)");
      String osString = System.getProperty("os.name").toLowerCase();
      /* Note: The implementations below should work equally well for opening URLs in the default
      web browser. */
      String fileURL;
      try {
        fileURL = fileToOpen.toURI().toURL().toString();
      } catch (MalformedURLException e) {
        throw new StudyCasterException("Failed to generate file URL", e);
      }

      String command[];
      // See http://www.rgagnon.com/javadetails/java-0014.html
      // See http://frank.neatstep.com/node/84
      /* I considered using the /MAX argument of the windows "start" command to maximize the window
      of the started application, but this even maximizes the pick association dialog if it happens
      to appear, so don't. */
      if (osString.contains("windows 95") || osString.contains("windows 98") ||
          osString.contains("windows me"))
      {
        // TODO: Test this.
        // See http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html?page=4
        command = new String[] {"command.com", "/C", "start", "\"WindowTitle\"", fileURL};
      } else if (osString.contains("win")) {
        command = new String[] {"cmd.exe", "/C", "start", "\"WindowTitle\"", fileURL};
      } else if (osString.contains("mac")) {
        /* Note: This was verified to work, opening an XLS file on MacOS X with Java 1.5.0_24.
        Putting single quotes around the URL would break it, so don't. */
        /* See http://developer.apple.com/mac/library/documentation/Darwin/Reference/ManPages/man1/open.1.html */
        command = new String[] {"open", fileURL};
      } else {
        throw new StudyCasterException(
            "Can't open file; Java Desktop API or platform-specific implementation not found");
      }
      try {
        executeShellCommand(command);
      } catch (StudyCasterException e) {
        throw new StudyCasterException(
            "Can't open file; problem while executing shell command", e);
      }
    }
  }

  public static String sanitizeFileNameComponent(String s) {
    StringBuilder ret = new StringBuilder();
    for (char c : s.toCharArray()) {
      ret.append((Character.isDigit(c) || Character.isLetter(c) || c == '.') ? c : '_');
      if (ret.length() > 150)
        break;
    }
    return ret.toString();
  }

  @SuppressWarnings("unchecked")
  public static <V,E extends Exception> V checkedSwingInvokeAndWait(final CallableExt<V,E> r)
      throws StudyCasterException, E
  {
    final Wrapper<V> ret = new Wrapper<V>();
    final Wrapper<Exception> retE = new Wrapper<Exception>();
    try {
      SwingUtilities.invokeAndWait(new Runnable() {
        public void run() {
          try {
            ret.value = r.call();
          } catch (Exception e) {
            retE.value = e;
          }
        }
      });
    } catch (InterruptedException e) {
      throw new StudyCasterException("Interrupted method on EHT", e);
    } catch (InvocationTargetException e) {
      LOG.severe("Unexpected InvocationTargetException");
      throw new StudyCasterException("Unexcpected exception from method on EHT", e);
    }
    if (retE.value != null) {
      if (retE.value instanceof RuntimeException) {
        throw (RuntimeException) retE.value;
      } else {
        throw (E) retE.value;
      }
    }
    return ret.value;
  }

  public interface CallableExt<V,E extends Exception> {
    V call() throws E;
  }

  /* This function is copied directly from the 1.6 version of java.util.Arrays.
  Include it here to work for 1.5. */
  public static byte[] copyOfRange(byte[] original, int from, int to) {
    int newLength = to - from;
    if (newLength < 0)
        throw new IllegalArgumentException(from + " > " + to);
    byte[] copy = new byte[newLength];
    System.arraycopy(original, from, copy, 0,
                     Math.min(original.length - from, newLength));
    return copy;
  }
}
