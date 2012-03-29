package no.ebakke.studycaster.applications;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import no.ebakke.studycaster.backend.EnvironmentHooks;
import no.ebakke.studycaster.backend.StudyCasterException;
import no.ebakke.studycaster.ui.UIUtil;

public class StudyCasterApplication {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");

  public static void main(final String args[]) {
    final EnvironmentHooks hooks = EnvironmentHooks.create();

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        // Must be called before any UI components are rendered.
        try {
          UIUtil.setSystemLookAndFeel();
        } catch (StudyCasterException e) {
          LOG.log(Level.INFO, "Couldn't set system L&F", e);
        }

        new StudyCaster(hooks, true).runStudy();
      }
    });
  }
}
