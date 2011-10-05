package no.ebakke.studycaster.nouveau;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import no.ebakke.studycaster.api.ServerContext;
import no.ebakke.studycaster.api.StudyCasterException;
import no.ebakke.studycaster.configuration.StudyConfiguration;

// TODO: Rename to StudyCasterUI.
/** Methods in this class must be called from the event-handling thread only. */
public final class StudyUI {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  final private MainFrame mainFrame;
  // TODO: Implement the dialog for multiple launches.
  final private EnvironmentHooks hooks;
  private ServerContext serverContext = null;
  private StudyConfiguration configuration = null;

  private StudyUI(EnvironmentHooks hooks) {
    this.hooks = hooks;
    mainFrame = new MainFrame();
    mainFrame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        // TODO: Implement.
      }
    });
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
    mainFrame.setProgressBarStatus("", true);
    mainFrame.setInstructions("");
    mainFrame.setButtonsVisible(false, false, false);
    mainFrame.setVisible(true);

    // TODO: Consider including a copy of the SwingWorker class from JDK 1.6 and using that instead.
    new Thread(new Runnable() {
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
    }, "StudyUI-runStudy").start(); // TODO: Rename to reflect new class name.
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
