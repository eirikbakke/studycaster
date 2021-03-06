package no.ebakke.studycaster.ui;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import no.ebakke.studycaster.backend.StudyCasterException;
import no.ebakke.studycaster.util.Util;

public final class UIUtil {
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

  public static Font createFont(final String fileName, final float size) throws IOException {
    try {
      return Font.createFont(Font.TRUETYPE_FONT, Util.getResource(fileName)).deriveFont(size);
    } catch (FontFormatException e) {
      throw new IOException("Could not load font; " + e.getMessage());
    }
  }

  public static String jOptionPaneChoiceString(int choice) {
    if        (choice == JOptionPane.OK_OPTION) {
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
          "minimized" : "visible and not minimized");
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
            ret.setValue(callable.call());
          } catch (Exception e) {
            retE.setValue(e);
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
    if (retE.getValue() != null) {
      if (retE.getValue() instanceof RuntimeException) {
        throw (RuntimeException) retE.getValue();
      } else {
        throw (E) retE.getValue();
      }
    }
    return ret.getValue();
  }

  public interface CallableExt<V,E extends Exception> {
    V call() throws E;
  }

  private static class Wrapper<T> {
    private T value;

    public synchronized T getValue() {
      return value;
    }

    public synchronized void setValue(T value) {
      this.value = value;
    }
  }
}
