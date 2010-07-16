package no.ebakke.studycaster;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import no.ebakke.studycaster.util.Blocker;

public class StudyCasterUI {
  public enum UIAction {
    NO_ACTION,
    CLOSE,
    UPLOAD
  }

  private StatusFrame sf;
  private final Object actionCondition = new Object();
  private UIAction actionTaken = UIAction.NO_ACTION;
  private Blocker actionBlocker = new Blocker();

  public StudyCasterUI(final String instructions) {
    final Object initedCondition = new Object();
    StatusFrame.setSystemLookAndFeel();
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        synchronized (initedCondition) {
          sf = new StatusFrame(instructions);
          sf.setVisible(true);
          sf.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent evt) {
              actionTaken = UIAction.CLOSE;
              actionBlocker.releaseBlockingThreads();
            }
          });
          sf.getUploadButton().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              sf.getUploadButton().setEnabled(false);
              actionTaken = UIAction.UPLOAD;
              actionBlocker.releaseBlockingThreads();
            }
          });
          initedCondition.notify();
        }
      }
    });
    synchronized (initedCondition) {
      while (sf == null) {
        try {
          initedCondition.wait();
        } catch (InterruptedException e) { }
      }
    }
  }

  public void disposeUI() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        sf.dispose();
      }
    });
  }

  public void showMessageDialog(final String title, final String message, final int messageType, boolean block) {
    final Blocker blocker = new Blocker();

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        JDialog positionDialog = new JDialog(sf, true);
        Dimension sdim = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension wdim = positionDialog.getSize();
        positionDialog.setLocation(sdim.width - wdim.width - 100, sdim.height - wdim.height - 150);
        JOptionPane.showMessageDialog(positionDialog, message, title, messageType);
        positionDialog.dispose();
        blocker.releaseBlockingThreads();
      }
    });
    if (block)
      blocker.blockUntilReleased();
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

  public UIAction waitForUserAction() {
    actionBlocker.blockUntilReleased();
    actionBlocker = new Blocker();
    return actionTaken;
  }
}
