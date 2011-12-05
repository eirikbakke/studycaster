package no.ebakke.studycaster.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jnlp.ServiceManager;
import javax.jnlp.SingleInstanceListener;
import javax.jnlp.SingleInstanceService;
import javax.jnlp.UnavailableServiceException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import no.ebakke.studycaster.api.StudyCasterException;
import no.ebakke.studycaster.util.Blocker;
import no.ebakke.studycaster.util.Util;
import no.ebakke.studycaster.util.Util.CallableExt;
import no.ebakke.studycaster.util.stream.NonBlockingOutputStream;
import no.ebakke.studycaster.util.stream.StreamProgressObserver;

// TODO: Check that this is actually the case.
/** This class should be thread-safe. */
public class StudyCasterUI {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  public enum UIAction {
    NO_ACTION,
    CLOSE,
    UPLOAD
  }
  private StatusFrame sf;
  private UIAction actionTaken = UIAction.NO_ACTION;
  private Blocker actionBlocker = new Blocker();
  private File defaultFile, selectedFile;
  private FileFilter uploadFileFilter;
  private SingleInstanceService singleInstanceService;
  private SingleInstanceListener singleInstanceListener;
  private StreamProgressObserver spo = new StreamProgressObserver() {
      public void updateProgress(final NonBlockingOutputStream nbos) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            // A workaround to support the longs returned by nbos methods.
            double fraction = ((double) nbos.getBytesWritten()) / ((double) nbos.getBytesPosted());
            final int STEPS = 1000000;
            getProgressBarUI().setBounds(0, STEPS);
            getProgressBarUI().setProgress((int) (STEPS * fraction));
          }
        });
      }
    };

  public void setUploadFileFilter(final FileFilter fileFilter) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        uploadFileFilter = fileFilter;
      }
    });
  }

  public void setInstructions(final String instructions) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        sf.setInstructions(instructions);
      }
    });
  }

  public StudyCasterUI()
      throws StudyCasterException, InterruptedException
  {
    StatusFrame.setSystemLookAndFeel();

    // TODO: Move this to a more appropriate place, and make it more robust.
    try {
      singleInstanceService =
          (SingleInstanceService) ServiceManager.lookup("javax.jnlp.SingleInstanceService");
      singleInstanceListener = new SingleInstanceListener() {
        public void newActivation(String[] strings) {
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              JOptionPane.showMessageDialog(
                  (sf == null) ? null : sf.getPositionDialog(),
                  "<html>The StudyCaster client is already running in a different window;<br>" +
                  "please close it first if you wish to start over.</html>",
                  "Already Running",
                  JOptionPane.INFORMATION_MESSAGE);
            }
          });
          LOG.info("User tried to open a new instance");
        }
      };
      singleInstanceService.addSingleInstanceListener(singleInstanceListener);
    } catch (UnavailableServiceException e) {
      LOG.log(Level.INFO,
          "Couldn''t create a SingleInstanceService (normal when run outside of JWS)", e);
    }

    Util.checkedSwingInvokeAndWait(new Util.CallableExt<Void,StudyCasterException>() {
      public Void call() {
        sf = new StatusFrame();
        sf.addWindowListener(new java.awt.event.WindowAdapter() {
          @Override
          public void windowClosed(java.awt.event.WindowEvent evt) {
            if (singleInstanceService != null) {
              singleInstanceService.removeSingleInstanceListener(singleInstanceListener);
              singleInstanceListener = null;
              singleInstanceService = null;
            }
            actionTaken = UIAction.CLOSE;
            actionBlocker.releaseBlockingThread();
            new Thread(new Runnable() {
              public void run() {
                try {
                  Thread.sleep(7000);
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
                LOG.warning("Forcing exit in three seconds (this may be last log message)");
                try {
                  Thread.sleep(3000);
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
                LOG.warning("Forcing exit ten seconds after window closure");
                System.exit(0);
              }
            }, "StudyCasterUI-windowClosureForceQuit").start();
          }
        });
        sf.getUploadButton().addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            sf.getUploadButton().setEnabled(false);
            UploadDialogOld upld = new UploadDialogOld(sf, uploadFileFilter, defaultFile);
            LOG.info("Now displaying upload dialog");
            upld.setVisible(true);
            selectedFile = upld.getSelectedFile();
            if (selectedFile != null) {
              actionTaken = UIAction.UPLOAD;
              actionBlocker.releaseBlockingThread();
            } else {
              sf.getUploadButton().setEnabled(true);
            }
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
          LOG.log(Level.INFO, "User rejected consent dialog ({0})",
              ((decision == JOptionPane.CANCEL_OPTION) ? "pressed cancel" : "closed dialog"));
          sf.dispose();
          actionTaken = UIAction.CLOSE;
        }
        return null;
      }
    });
  }

  public void setDefaultFile(File defaultFile) {
    this.defaultFile = defaultFile;
  }

  public File getSelectedFile() {
    return selectedFile;
  }

  public void disposeUI() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        sf.dispose();
      }
    });
  }

  public boolean showDownloadOrExistingDialog(final String fileName)
      throws StudyCasterException, InterruptedException
  {
    return Util.checkedSwingInvokeAndWait(new CallableExt<Boolean, StudyCasterException>() {
      public Boolean call() {
        final String downloadOption = "Download New File";
        final String existingOption = "Keep Existing File";
        int res = JOptionPane.showOptionDialog(sf.getPositionDialog(),
            "<html>The StudyCaster client found a file from an old session:<br>" + fileName +
            "<br><br>" +
            "Would you like to keep working on the existing file, or download a new one?</html>",
            "Open Sample File",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
            null, new String[] {downloadOption, existingOption}, existingOption);
        return res == JOptionPane.YES_OPTION;
      }
    });
  }

  public void showConfirmationCodeDialog(final String confirmationCode, boolean block)
      throws StudyCasterException, InterruptedException
  {
    Util.checkedSwingInvokeAndWait(new CallableExt<Void, StudyCasterException>() {
      public Void call() {
        ConfirmationCodeDialog.show(sf, confirmationCode);
        return null;
      }
    });
  }

  public void showMessageDialog(final String title, final String message, final int messageType) {
    try {
      Util.checkedSwingInvokeAndWait(new CallableExt<Void,StudyCasterException>() {
        public Void call() throws StudyCasterException {
          JOptionPane.showMessageDialog(sf.getPositionDialog(), message, title, messageType);
          return null;
        }
      });
    } catch (InterruptedException e) {
      LOG.log(Level.WARNING, "Got an unexpected exception showing a message dialog", e);
    } catch (StudyCasterException e) {
      LOG.log(Level.WARNING, "Got an unexpected exception showing a message dialog", e);
    }
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

  public UIAction waitForUserAction() throws InterruptedException {
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
      os.addObserver(spo);
    } else {
      os.removeObserver(spo);
    }
  }
}
