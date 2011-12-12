package no.ebakke.studycaster.ui;

import java.awt.Canvas;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Frame;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import no.ebakke.studycaster.backend.StudyCasterException;

public final class UIUtil {
  private static final String RESOURCE_DIR = "no/ebakke/studycaster/resources/";
  private UIUtil() { }

  public static void setSystemLookAndFeel() throws StudyCasterException {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      throw new StudyCasterException(e);
    }
  }

  public static void copyStringToClipboard(String str) throws StudyCasterException {
    try {
      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
          new StringSelection(str), null);
    } catch (Exception e) {
      /* Convert unchecked to checked exception; on Windows, setContents() is known to throw an
      IllegalStateException ("cannot open system clipboard") from an underlying native method every
      now and then. */
      throw new StudyCasterException(e);
    }
  }

  private static InputStream getResource(final String fileName) throws IOException {
    InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(
        RESOURCE_DIR + fileName);
    if (is == null) {
      throw new FileNotFoundException(
          "Could not locate resource \"" + fileName + "\"");
    }
    return is;
  }

  public static Font createFont(final String fileName, final float size) throws IOException {
    try {
      return Font.createFont(Font.TRUETYPE_FONT, getResource(fileName)).deriveFont(size);
    } catch (FontFormatException e) {
      throw new IOException("Could not load font; " + e.getMessage());
    }
  }

  public static Image loadImage(final String fileName, boolean wait) throws IOException {
    /* Do this to properly catch an error which may otherwise cause waitForAll() below to hang with
    an "Uncaught error fetching image" output. */
    //getResource(fileName).close();
    final URL pointerImageURL = Thread.currentThread().getContextClassLoader().getResource(
        RESOURCE_DIR + fileName);
    final Image ret = Toolkit.getDefaultToolkit().getImage(pointerImageURL);
    if (wait) {
      final MediaTracker mt = new MediaTracker(new Canvas());
      mt.addImage(ret, 0);
      try {
        mt.waitForAll();
      } catch (InterruptedException e) {
        throw new InterruptedIOException();
      }
    }
    return ret;
  }

  public static String jOptionPaneChoiceString(int choice) {
    if (choice == JOptionPane.OK_OPTION) {
      return "OK";
    } else if (choice == JOptionPane.CANCEL_OPTION){
      return "Cancel";
    } else if (choice == JOptionPane.YES_OPTION) {
      return "Yes";
    } else if (choice == JOptionPane.NO_OPTION) {
      return "No";
    } else if (choice == JOptionPane.CLOSED_OPTION) {
      return "Closed";
    } else {
      return "Choice #" + choice;
    }
  }

  public static String windowStateString(Frame frame) {
    if        (frame == null) {
      return "null";
    } else if (frame.isVisible()) {
      return ((frame.getExtendedState() == Frame.ICONIFIED) ?
          "iconified" : "visible and not minimized");
    } else if (frame.isDisplayable()) {
      return "invisible but displayable";
    } else {
      return "disposed";
    }
  }

  /** Runs callable on the EDT and blocks until it has completed. Safe to call from any thread,
  including the EDT, as long as callable is safe to call on the EDT. */
  @SuppressWarnings("unchecked")
  public static <V,E extends Exception> V swingBlock(final CallableExt<V,E> callable)
      throws E, InterruptedException
  {
    if (EventQueue.isDispatchThread())
      return callable.call();

    final Wrapper<V> ret = new Wrapper<V>();
    final Wrapper<Exception> retE = new Wrapper<Exception>();
    try {
      SwingUtilities.invokeAndWait(new Runnable() {
        public void run() {
          try {
            ret.value = callable.call();
          } catch (Exception e) {
            retE.value = e;
          }
        }
      });
    } catch (InvocationTargetException e) {
      if        (e.getCause() instanceof RuntimeException) {
        throw ((RuntimeException) e.getCause());
      } else if (e.getCause() instanceof Error) {
        throw ((Error) e.getCause());
      } else {
        throw new RuntimeException("Unexpected exception on the EDT", e.getCause());
      }
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

  private static class Wrapper<T> {
    T value;
  }
}
