package no.ebakke.studycaster;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
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
              new Thread(new Runnable() {
                public void run() {
                  try {
                    Thread.sleep(5000);
                  } catch (InterruptedException e) {
                    StudyCaster.log.log(Level.WARNING, "Sleep was interrupted.", e);
                  }
                  StudyCaster.log.warning("Forcing exit five seconds after window closure.");
                  System.exit(0);
                }
              }).start();
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

  public void showPaneInDialog(Component comp) {
    
  }

  // TODO: Reduce code duplication between the next two methods.
  public void showConfirmationCodeDialog(final String confirmationCode, boolean block) {
    final Blocker blocker = new Blocker();
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        ConfirmationCodeDialog.show(sf, confirmationCode);
        blocker.releaseBlockingThreads();
      }
    });
    if (block)
      blocker.blockUntilReleased();
  }

  public void showMessageDialog(final String title, final String message, final int messageType, boolean block) {
    final Blocker blocker = new Blocker();

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        JDialog positionDialog = new JDialog(sf);
        JOptionPane.showMessageDialog(positionDialog, message, title, messageType);
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
