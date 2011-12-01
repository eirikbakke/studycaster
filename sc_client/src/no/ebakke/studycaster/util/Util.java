package no.ebakke.studycaster.util;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import no.ebakke.studycaster.api.StudyCasterException;

public final class Util {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  private Util() { }

  public static boolean fileAvailableExclusive(File f) {
    // Without this check, the file would be created below if it didn't already exist.
    if (!f.exists())
      return true;
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
      props.append(first ? "" : ", ").append(key).append("=").append(System.getProperty(key));
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

  /** Used to enclose operations which may throw InterruptedException but which are restartable,
  when the contract of the enclosing method does not permit throwing an InterruptedException but
  rather requires that the method blocks until completion. */
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

  /** Returns false iff Desktop API is unavailable on this platform. Any IOException thrown will
  originate from the called method. */
  private static boolean callDesktopMethod(String methodName, Class parameterType, Object argument)
      throws StudyCasterException, IOException
  {
    Class<?> desktopClass = null;
    try {
      desktopClass = Class.forName("java.awt.Desktop");
    } catch (ClassNotFoundException e) { }
    if (desktopClass == null) {
      LOG.info("Java Desktop API not found; probably on JRE 1.5");
      return false;
    }
    final Object desktopObject;
    final Method methodToCall;
    try {
      Method getDesktopMethod = desktopClass.getMethod("getDesktop");
      try {
        desktopObject = getDesktopMethod.invoke(null);
      } catch (InvocationTargetException e) {
        throw new StudyCasterException("Unexpected exception", e.getCause());
      }
      if (desktopObject == null || !(desktopClass.isInstance(desktopObject))) {
        throw new StudyCasterException(
            "Unexpected return value from Desktop.getDesktop(): " + desktopObject);
      }
      methodToCall = desktopClass.getMethod(methodName, parameterType);
      try {
        methodToCall.invoke(desktopObject, argument);
      } catch (InvocationTargetException e) {
        if (e.getCause() instanceof IOException) {
          throw (IOException) e.getCause();
        } else {
          throw new StudyCasterException("Unexpected exception", e.getCause());
        }
      }
    } catch (NoSuchMethodException e) {
      throw new StudyCasterException("Failed to locate an expected library method", e);
    } catch (IllegalAccessException e) {
      throw new StudyCasterException("Failed to invoke a library method", e);
    }
    return true;
  }

  /** Returns false iff the command executes with a non-zero return code. */
  private static boolean desktopOpenURIPlatformDependent(URI uri)
      throws StudyCasterException
  {
    final String osString = System.getProperty("os.name").toLowerCase();
    final String urlString;
    try {
      urlString = uri.toURL().toString();
    } catch (MalformedURLException e) {
      throw new StudyCasterException("Failed to generate URL", e);
    }
    final String command[];
    // See http://www.rgagnon.com/javadetails/java-0014.html
    // See http://frank.neatstep.com/node/84
    // The "PlaceHolderTitle" argument is necessary to support files that might contain spaces.
    /* While it may appear possible to omit the Windows "start" command from the implementations
    below, this let to subtle bugs in which proc.waitFor() would block for certain applications (in
    particular, Adobe Acrobat Professional, but not Microsoft Excel). The /MAX argument should not
    be used as it would maximize even the "pick association" dialog. The "PlaceHolderTitle" argument
    is needed to support opening files with spaces in their paths. */
    if (osString.contains("windows 95") || osString.contains("windows 98") ||
        osString.contains("windows me"))
    {
      // TODO: Test on this platform.
      // See http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html?page=4
      command = new String[] {"command.com", "/C", "start", "\"PlaceholderTitle\"", urlString};
    } else if (osString.contains("win")) {
      command = new String[] {"cmd.exe", "/C", "start", "\"PlaceholderTitle\"", urlString};
    } else if (osString.contains("mac")) {
      /* Note: This was verified to work, opening an XLS file on MacOS X with Java 1.5.0_24.
      Putting single quotes around the URL would break it, so don't. */
      /* See http://developer.apple.com/mac/library/documentation/Darwin/Reference/ManPages/man1/open.1.html */
      command = new String[] {"open", urlString};
    } else {
      throw new StudyCasterException("Desktop API emulation not supported on this platform");
    }
    try {
      Process proc = Runtime.getRuntime().exec(command);
      return proc.waitFor() == 0;
    } catch (IOException e) {
      throw new StudyCasterException("Got IOException while executing shell command", e);
    } catch (InterruptedException e) {
      throw new StudyCasterException("Interrupted while executing shell command", e);
    }
  }

  /** Returns false in the case of an error likely to be caused by a missing file association. */
  public static boolean desktopOpenFile(File fileToOpen) throws StudyCasterException
  {
    try {
      if (callDesktopMethod("open", File.class, fileToOpen))
        return true;
    } catch (IOException e) {
      return false;
    }

    return desktopOpenURIPlatformDependent(fileToOpen.toURI());
  }

  public static void desktopOpenURI(URI toOpen) throws StudyCasterException {
    try {
      if (callDesktopMethod("browse", URI.class, toOpen))
        return;
    } catch (IOException e) {
      throw new StudyCasterException("Java Desktop API failed to open URI", e);
    }

    if (!desktopOpenURIPlatformDependent(toOpen))
      throw new StudyCasterException("Desktop API emulation failed to open URI");
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

  /* TODO: Get rid of this method and the Wrapper class, as well as other unused methods in this
  class. */
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

  /* This function is based on the method with the same name in the 1.6 version of java.util.Arrays.
  Include it here to work for 1.5. */
  public static byte[] copyOfRange(byte[] original, int from, int to) {
    if (from > to)
      throw new IndexOutOfBoundsException();
    final byte[] ret = new byte[to - from];
    System.arraycopy(original, from, ret, 0, Math.min(original.length - from, ret.length));
    return ret;
  }

  @SuppressWarnings("SleepWhileInLoop")
  public static void delayAtLeast(long millis) throws InterruptedException {
    final long startTime = System.currentTimeMillis();
    while (true) {
      final long remainingMillis = Math.round(millis - (System.currentTimeMillis() - startTime));
      if (remainingMillis <= 0)
        break;
      Thread.sleep(remainingMillis);
    }
  }

  public static void checkClosed(AtomicBoolean closed) throws IllegalStateException {
    if (closed.get())
      throw new IllegalStateException("Closed");
  }

  @SuppressWarnings("NestedAssignment")
  public static byte[] computeSHA1(InputStream is) throws IOException {
    final byte[] buf = new byte[128 * 1024];
    final MessageDigest md;
    try {
      md = MessageDigest.getInstance("SHA1");
    } catch (NoSuchAlgorithmException e) {
      throw new UnsupportedOperationException(e);
    }
    int n;
    while ((n = is.read(buf)) > 0)
      md.update(buf, 0, n);
    return md.digest();
  }

  public static byte[] computeSHA1(File file) throws IOException {
    InputStream fis = new FileInputStream(file);
    try {
      return computeSHA1(fis);
    } finally {
      fis.close();
    }
  }
}
