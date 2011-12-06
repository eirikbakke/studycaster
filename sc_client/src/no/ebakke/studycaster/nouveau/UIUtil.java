package no.ebakke.studycaster.nouveau;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import javax.swing.UIManager;
import no.ebakke.studycaster.api.StudyCasterException;

final class UIUtil {
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
}
