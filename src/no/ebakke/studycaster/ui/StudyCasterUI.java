package no.ebakke.studycaster.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.jnlp.ServiceManager;
import javax.jnlp.SingleInstanceListener;
import javax.jnlp.SingleInstanceService;
import javax.jnlp.UnavailableServiceException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import no.ebakke.studycaster.api.ServerContext;
import no.ebakke.studycaster.api.StudyCaster;
import no.ebakke.studycaster.util.Blocker;
import no.ebakke.studycaster.util.stream.NonBlockingOutputStream;
import no.ebakke.studycaster.util.stream.NonBlockingOutputStream.StreamProgressObserver;

public class StudyCasterUI {
  public enum UIAction {
    NO_ACTION,
    CLOSE,
    UPLOAD
  }
  private StatusFrame sf;
  private UIAction actionTaken = UIAction.NO_ACTION;
  private Blocker actionBlocker = new Blocker();
  private int streamProgressStart;
  private SingleInstanceService singleInstanceService;
  private SingleInstanceListener singleInstanceListener;
  private StreamProgressObserver spo = new StreamProgressObserver() {
      public void updateProgress(final int bytesWritten, final int bytesRemaining) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            getProgressBarUI().setBounds(streamProgressStart, bytesWritten + bytesRemaining + ServerContext.DEF_UPLOAD_CHUNK_SZ);
            getProgressBarUI().setProgress(bytesWritten);
          }
        });
      }
    };


  public StudyCasterUI(final String instructions) {
    final Blocker initedBlocker = new Blocker();
    StatusFrame.setSystemLookAndFeel();

    // TODO: Move this to a more appropriate place and make it more robust.
    try {
      singleInstanceService = (SingleInstanceService) ServiceManager.lookup("javax.jnlp.SingleInstanceService");
      singleInstanceListener = new SingleInstanceListener() {
        public void newActivation(String[] strings) {
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              JOptionPane.showMessageDialog(
                  (sf == null) ? null : sf.getPositionDialog(),
                  "<html>The User Study Console is already running in a different window;<br>" +
                  "please close it first if you wish to start over.</html>",
                  "Already Running",
                  JOptionPane.INFORMATION_MESSAGE);
            }
          });
          StudyCaster.log.info("User tried to open a new instance");
        }
      };
      singleInstanceService.addSingleInstanceListener(singleInstanceListener);
    } catch (UnavailableServiceException e) {
      StudyCaster.log.warning("Failed to create a SingleInstanceService");
    }

    // TODO: Use SwingUtilities.invokeAndWait() instead.
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        sf = new StatusFrame(instructions);
        sf.addWindowListener(new java.awt.event.WindowAdapter() {
          @Override
          public void windowClosed(java.awt.event.WindowEvent evt) {
            actionTaken = UIAction.CLOSE;
            actionBlocker.releaseBlockingThread();
            if (singleInstanceService != null) {
              singleInstanceService.removeSingleInstanceListener(singleInstanceListener);
              singleInstanceListener = null;
              singleInstanceService = null;
            }
            new Thread(new Runnable() {
              public void run() {
                try {
                  Thread.sleep(7000);
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
                StudyCaster.log.warning("Forcing exit in three seconds (this may be last log message)");
                try {
                  Thread.sleep(3000);
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
                StudyCaster.log.warning("Forcing exit ten seconds after window closure");
                System.exit(0);
              }
            }, "window-closure-force-quit").start();
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
          StudyCaster.log.info("User rejected consent dialog (" + ((decision == JOptionPane.CANCEL_OPTION) ? "pressed cancel" : "closed dialog") + ")");
          sf.dispose();
          actionTaken = UIAction.CLOSE;
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

  public void showConfirmationCodeDialog(final String confirmationCode, boolean block) {
    // TODO: Use SwingUtilities.invokeAndWait() instead.
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
    // TODO: Use SwingUtilities.invokeAndWait() instead.
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

  public void setMonitorStreamProgress(NonBlockingOutputStream os, boolean doMonitor) {
    if (os == null)
      return;
    if (doMonitor) {
      streamProgressStart = os.getWrittenBytes();
      os.addObserver(spo);
    } else {
      os.removeObserver(spo);
    }
  }
}
