package no.ebakke.studycaster.nouveau;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jnlp.SingleInstanceListener;
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
  /* Methods in this class must be called from the event-handling thread only, and members must be
  accessed from the event-handling thread only. */
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  final private MainFrame mainFrame;
  final private EnvironmentHooks hooks;
  private ServerContext serverContext = null;
  private StudyConfiguration configuration = null;
  private Thread initializerThread, failsafeCloseThread, backendCloseThread;
  private final WindowListener windowClosingListener;

  private StudyUI(EnvironmentHooks hooks) {
    this.hooks = hooks;
    mainFrame = new MainFrame();
    windowClosingListener = new WindowAdapter() {
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
        if (doClose)
          closeUIandBackend();
      }
    };
    mainFrame.addWindowListener(windowClosingListener);
  }

  private String getUIString(UIStringKey key) {
    return configuration.getUIStrings().getString(key);
  }

  // TODO: Rename or refactor.
  private void closeUIandBackend() {
    closeUI();
    closeBackend(null);
  }

  private void closeUI() {
    if (failsafeCloseThread != null)
      throw new IllegalStateException("UI already closed");
    mainFrame.removeWindowListener(windowClosingListener);
    mainFrame.setVisible(false);
    mainFrame.dispose();
    failsafeCloseThread = new Thread(new Runnable() {
      public void run() {
          try {
            Thread.sleep(7000);
            LOG.warning("Forcing exit in three seconds (this may be last log message)");
            Thread.sleep(3000);
            LOG.warning("Forcing exit ten seconds after window closure");
          } catch (InterruptedException e) {
            LOG.warning("Failsafe exit thread interrupted; exiting immediately");
          }
          System.exit(1);
        }
      }, "StudyUI-failsafeClose");
    // Don't keep the VM running just because of the failsafe thread.
    failsafeCloseThread.setDaemon(true);
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
        // In the case of an error, serverContext may still not be defined.
        if (serverContext != null)
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

      /* Do this after properly setting up the main window, in case there are enqueued messages. */
      SingleInstanceHandler sih = hooks.getSingleInstanceHandler();
      if (sih != null) {
        sih.setListener(new SingleInstanceListener() {
          public void newActivation(String[] strings) {
            LOG.info("Showing already running dialog");
            JOptionPane.showMessageDialog(mainFrame.getPositionDialog(),
                getUIString(UIStringKey.DIALOG_ALREADY_RUNNING_MESSAGE),
                getUIString(UIStringKey.DIALOG_ALREADY_RUNNING_TITLE),
                JOptionPane.INFORMATION_MESSAGE);
          }
        });
      }
    } catch (StudyCasterException e) {
      /* Note: Due to the exception, configuration and serverContext may not be defined. */
      LOG.log(Level.SEVERE, "Showing fatal error dialog", e);
      mainFrame.setProgressBarStatus("", false);
      JOptionPane.showMessageDialog(mainFrame.getPositionDialog(),
          "There was an unexpected error:\n" + e.getMessage(), "Error",
          JOptionPane.ERROR_MESSAGE);
      closeUIandBackend();
    }
  }

  public void runStudy(final String args[]) {
    if (initializerThread != null)
      throw new IllegalStateException("Already started");
    initializerThread = new Thread(new Runnable() {
      private ServerContext      serverContextT;
      private StudyConfiguration configurationT;

      public void run() {
        StudyCasterException exception = null;
        try {
          serverContextT = new ServerContext();
          hooks.getLogFormatter().setServerSecondsAhead(serverContextT.getServerSecondsAhead());
          try {
            hooks.getConsoleStream().connect(serverContextT.uploadFile("console.txt"));
            if (args.length != 1) {
              throw new StudyCasterException(
                  "Invoked with incorrect command-line arguments (missing configuration ID)");
            }
            configurationT = StudyConfiguration.parseConfiguration(
              serverContextT.downloadFile("studyconfig.xml"), args[0]);
          } catch (IOException e) {
            throw new StudyCasterException("Unexpected I/O error", e);
          }
        } catch (StudyCasterException e) {
          exception = e;
        }
        final StudyCasterException exceptionF = exception;
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            /* Change member variables of the outer class only once we're back in the EHT. Seemed
            cleaner than declaring them volatile. */
            serverContext = serverContextT;
            configuration = configurationT;
            initUI(exceptionF);
          }
        });
      }
    }, "StudyUI-runStudy");

    /* To avoid a race condition in closeBackend(), initializerThread must be initialized before the
    UI can be displayed, and started after the UI has been displayed. */
    mainFrame.setProgressBarStatus("", true);
    mainFrame.setInstructions("");
    mainFrame.setButtonsVisible(false, false, false);
    mainFrame.setVisible(true);
    initializerThread.start();
  }

  public static void main(final String args[]) {
    final EnvironmentHooks hooks = EnvironmentHooks.create();

    // Must be called before any UI components are rendered.
    try {
      UIUtil.setSystemLookAndFeel();
    } catch (StudyCasterException e) {
      LOG.log(Level.INFO, "Couldn''t set system L&F", e);
    }

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        new StudyUI(hooks).runStudy(args);
      }
    });
  }
}
