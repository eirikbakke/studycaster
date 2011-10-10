package no.ebakke.studycaster.nouveau;

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
}
