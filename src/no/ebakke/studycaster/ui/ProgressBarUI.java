package no.ebakke.studycaster.ui;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import no.ebakke.studycaster.api.StudyCaster;

public class ProgressBarUI {
  private JProgressBar ui;

  public ProgressBarUI(JProgressBar ui) {
    this.ui = ui;
  }

  public void setTaskAppearance(final String text, final boolean indeterminate) {
    StudyCaster.log.info("Changing status bar text to \"" + text + "\"");
    ui.setString(text);
    ui.setIndeterminate(indeterminate);
    if (indeterminate)
      setProgress(0);
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
