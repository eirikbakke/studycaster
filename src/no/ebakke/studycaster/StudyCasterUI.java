package no.ebakke.studycaster;

import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class StudyCasterUI {
  private StatusFrame sf;

  public StudyCasterUI(final String instructions) {
    StatusFrame.setSystemLookAndFeel();
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        synchronized (StudyCasterUI.this) {
          sf = new StatusFrame(instructions);
          sf.setVisible(true);
          StudyCasterUI.this.notify();
        }
      }
    });
    synchronized (this) {
      while (sf == null) {
        try {
          wait();
        } catch (InterruptedException e) { }
      }
    }
  }

  public void exitWithError(final String errorDialogTitle, final StudyCasterException showException) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        JDialog positionDialog = new JDialog(sf, true);
        Dimension sdim = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension wdim = positionDialog.getSize();
        positionDialog.setLocation(sdim.width - wdim.width - 100, sdim.height - wdim.height - 150);
        JOptionPane.showMessageDialog(positionDialog, showException.getLocalizedMessage(), errorDialogTitle, JOptionPane.WARNING_MESSAGE);
        positionDialog.dispose();
        sf.dispose();
      }
    });
  }

  public ProgressBarUI getProgressBarUI() {
    return sf.getProgressBarUI();
  }

  public void setUploadEnabled(final boolean enabled) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        sf.setUploadEnabled(enabled);
      }
    });
  }

  public void setUIVisible(final boolean visible) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        sf.setVisible(false);
      }
    });
  }
}
