package no.ebakke.studycaster.ui;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class ProgressBarUI {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  private JProgressBar ui;

  public ProgressBarUI(JProgressBar ui) {
    this.ui = ui;
  }

  public void setTaskAppearance(final String text, final boolean indeterminate) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        LOG.log(Level.INFO, "Changing status bar text to \"{0}\"", text);
        ui.setString(text);
        ui.setIndeterminate(indeterminate);
        if (indeterminate)
          setProgress(0);
      }
    });
  }

  public void setBounds(final int minimum, final int maximum) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        ui.setMinimum(minimum);
        ui.setMaximum(maximum);
        ui.setIndeterminate(false);
      }
    });
  }

  public void setProgress(final int progress) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        ui.setValue(progress);
      }
    });
  }
}
