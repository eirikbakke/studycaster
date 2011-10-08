package no.ebakke.studycaster.nouveau;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import no.ebakke.studycaster.api.ServerContext;
import no.ebakke.studycaster.api.StudyCasterException;
import no.ebakke.studycaster.configuration.StudyConfiguration;
import no.ebakke.studycaster.configuration.UIStringKey;
import no.ebakke.studycaster.util.Util;

/*
  Test cases for this class:
  * Multiple application launch before or after configuration is loaded.
  * Close window before or after configuration is loaded.
  * Failsafe shutdown 10 seconds after window is closed.
  * Regular shutdown after upload.
  * Regular shutdown after window closure.
  * Invoked with wrong command-line arguments.
*/

// TODO: Rename to StudyCasterUI. Rename threads to reflect change.
public final class StudyUI {
  // Methods in this class must be called from the event-handling thread only.
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  final private MainFrame mainFrame;
  // TODO: Implement the dialog for multiple launches.
  final private EnvironmentHooks hooks;
  private ServerContext serverContext = null;
  private StudyConfiguration configuration = null;
  private Thread initializerThread, failsafeCloseThread, backendCloseThread;

  private StudyUI(EnvironmentHooks hooks) {
    this.hooks = hooks;
    mainFrame = new MainFrame();
    mainFrame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        boolean doClose;
        if (configuration != null) {
          LOG.info("User tried to close main StudyCaster window");
          doClose = JOptionPane.showConfirmDialog(mainFrame.getPositionDialog(),
            getUIString(UIStringKey.DIALOG_CLOSE_MESSAGE),
            getUIString(UIStringKey.DIALOG_CLOSE_TITLE),
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
          if (doClose) {
            LOG.info("User confirmed closing of main StudyCaster window");
          } else {
            LOG.info("User canceled closing of main StudyCaster window");
          }
        } else {
          doClose = true;
          LOG.info("User closed main StudyCaster window before configuration was loaded; exiting");
        }
        if (doClose) {
          mainFrame.removeWindowListener(this);
          closeUI();
          closeBackend(new Runnable() {
            public void run() {
              failsafeCloseThread.interrupt();
            }
          });
        }
      }
    });
  }

  private String getUIString(UIStringKey key) {
    return configuration.getUIStrings().getString(key);
  }

  private void closeUI() {
    if (failsafeCloseThread != null)
      throw new IllegalStateException("UI already closed");
    mainFrame.setVisible(false);
    mainFrame.dispose();
    failsafeCloseThread = new Thread(new Runnable() {
      public void run() {
          try {
            Thread.sleep(7000);
            LOG.warning("Forcing exit in three seconds (this may be last log message)");
            Thread.sleep(3000);
            LOG.warning("Forcing exit ten seconds after window closure");
            System.exit(-1);
          } catch (InterruptedException e) {
            // Fall through.
          }
          // TODO: System.exit() no matter what.
          LOG.info("Fail safe exit disarmed");
        }
      }, "StudyUI-failsafeClose");
    failsafeCloseThread.start();
  }

  private void closeBackend(final Runnable onEndEHT) {
    if (backendCloseThread != null)
      throw new IllegalStateException("Backend already closed");
    backendCloseThread = new Thread(new Runnable() {
      public void run() {
        if (initializerThread != null) {
          Util.ensureInterruptible(new Util.Interruptible() {
            public void run() throws InterruptedException {
              initializerThread.join();
            }
          });
        }
        hooks.close();
        serverContext.close();
        if (onEndEHT != null)
          SwingUtilities.invokeLater(onEndEHT);
      }
    }, "StudyUI-backendClose");
    backendCloseThread.start();
  }

  // Must be called from the event-handling thread.
  private void initUI(StudyCasterException storedException) {
    try {
      if (storedException != null)
        throw storedException;
      mainFrame.setButtonCaptions(configuration.getUIStrings());
      mainFrame.setButtonsVisible(true, true, true);
    } catch (StudyCasterException e) {
      // TODO: Do something.
      e.printStackTrace();
    }
  }

  public void runStudy(final String args[]) {
    if (initializerThread != null)
      throw new IllegalStateException("Already started");
    // TODO: Consider including a copy of the SwingWorker class from JDK 1.6 and using that instead.
    initializerThread = new Thread(new Runnable() {
      public void run() {
        StudyCasterException exception = null;
        try {
          if (args.length != 1) {
            throw new StudyCasterException(
                "Invoked with incorrect command-line arguments (missing configuration ID)");
          }
          serverContext = new ServerContext();
          try {
            hooks.getConsoleStream().connect(serverContext.uploadFile("console.txt"));
            configuration = StudyConfiguration.parseConfiguration(
              serverContext.downloadFile("studyconfig.xml"), args[0]);
          } catch (IOException e) {
            throw new StudyCasterException("Unexpected I/O error", e);
          }
        } catch (StudyCasterException e) {
          exception = e;
        }
        final StudyCasterException exceptionF = exception;
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            initUI(exceptionF);
          }
        });
      }
    }, "StudyUI-runStudy");
    initializerThread.start();

    /* To avoid a race condition in closeBackend(), initializerThread must be started before the UI
    can be displayed. */
    mainFrame.setProgressBarStatus("", true);
    mainFrame.setInstructions("");
    mainFrame.setButtonsVisible(false, false, false);
    mainFrame.setVisible(true);
  }

  public static void main(final String args[]) {
    final EnvironmentHooks api = EnvironmentHooks.createStudyCaster();

    // Must be called before any UI components are rendered.
    try {
      UIUtil.setSystemLookAndFeel();
    } catch (StudyCasterException e) {
      LOG.log(Level.INFO, "Couldn''t set system L&F", e);
    }

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        new StudyUI(api).runStudy(args);
      }
    });
  }
}
