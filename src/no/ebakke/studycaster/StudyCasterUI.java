package no.ebakke.studycaster;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    final Blocker initedBlocker = new Blocker();
    StatusFrame.setSystemLookAndFeel();
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        sf = new StatusFrame(instructions);
        sf.addWindowListener(new java.awt.event.WindowAdapter() {
          @Override
          public void windowClosed(java.awt.event.WindowEvent evt) {
            actionTaken = UIAction.CLOSE;
            actionBlocker.releaseBlockingThread();
            new Thread(new Runnable() {
              public void run() {
                try {
                  Thread.sleep(5000);
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
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
            actionBlocker.releaseBlockingThread();
          }
        });
        sf.setVisible(true);
        int decision = JOptionPane.showConfirmDialog(sf.getPositionDialog(),
                "<html>This tool will record the contents of your screen while you perform the HIT,<br>" +
                "so we can learn about how different people approached the task.<br><br>" +
                "OK to start recording?</html>",
                "Screencasting",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (decision == JOptionPane.CANCEL_OPTION || decision == JOptionPane.CLOSED_OPTION) {
          sf.dispose();
          waitForUserAction();
        }
        initedBlocker.releaseBlockingThread();
      }
    });
    initedBlocker.blockUntilReleased();
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
        blocker.releaseBlockingThread();
      }
    });
    if (block)
      blocker.blockUntilReleased();
  }

  public void showMessageDialog(final String title, final String message, final int messageType, boolean block) {
    final Blocker blocker = new Blocker();

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        JOptionPane.showMessageDialog(sf.getPositionDialog(), message, title, messageType);
        blocker.releaseBlockingThread();
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

  public boolean wasClosed() {
    return actionTaken == UIAction.CLOSE;
  }
}
