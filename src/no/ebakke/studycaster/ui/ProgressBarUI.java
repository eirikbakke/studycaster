package no.ebakke.studycaster.ui;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class ProgressBarUI {
  private JProgressBar ui;

  public ProgressBarUI(JProgressBar ui) {
    this.ui = ui;
  }

  public void setTaskAppearance(final String text, final boolean indeterminate) {
    ui.setString(text);
    ui.setIndeterminate(indeterminate);
  }

  public void setTaskAppearance(final String text, final boolean indeterminate, final int progress, final int minimum, final int maximum) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        ui.setString(text);
        ui.setIndeterminate(indeterminate);
        ui.setValue(progress);
        ui.setMinimum(minimum);
        ui.setMaximum(maximum);
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
