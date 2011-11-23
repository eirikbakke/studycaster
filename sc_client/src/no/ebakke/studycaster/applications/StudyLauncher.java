package no.ebakke.studycaster.applications;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import no.ebakke.studycaster.configuration.StudyConfiguration;
import no.ebakke.studycaster.api.ServerContext;
import no.ebakke.studycaster.api.StudyCaster;
import no.ebakke.studycaster.api.StudyCasterException;
import no.ebakke.studycaster.screencasting.ScreenCensor;
import no.ebakke.studycaster.ui.StudyCasterUI;
import no.ebakke.studycaster.ui.StudyCasterUI.UIAction;
import no.ebakke.studycaster.util.Util;

/** This class will go away in a future version. */
public final class StudyLauncher {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  private StudyLauncher() { }

  public static void main(String args[]) throws InterruptedException {
    // TODO: Move even this error into the UI.
    final String configurationID = System.getProperty("studycaster.config.id");
    if (configurationID == null) {
      System.err.println("Property studycaster.config.id not set.");
      return;
    }
    runStudy(configurationID);
  }

  // TODO: Open the window before anything else, to allow force-quitting.
  public static void runStudy(String configurationID) throws InterruptedException {
    final Logger log = Logger.getLogger("no.ebakke.studycaster");
    final StudyConfiguration configuration;
    final ServerContext serverContext;

    LOG.info("Entering initial log message to promote fail-fast behavior of potential ConsoleTee bugs.");

    StudyCasterUI scui;

    try {
      scui = new StudyCasterUI();
    } catch (StudyCasterException e) {
      LOG.log(Level.SEVERE, "Unexpected exception during UI initialization", e);
      System.exit(0);
      return;
    }

    // TODO: Load instructions before the consent dialog becomes visible.
    if (scui.wasClosed()) {
      try {
        StudyCaster sc = new StudyCaster();
        LOG.info("User declined at consent dialog.");
        sc.concludeStudy();
      } catch (StudyCasterException e) {
        LOG.warning("Failed to send consent-declined log message");
      }
      System.exit(0);
    }

    StudyCaster sc = null;
    long lastModified1, lastModified2;
    boolean download = true;
    File openedFile;
    try {
      sc = new StudyCaster();
      try {
        configuration = StudyConfiguration.parseConfiguration(
            sc.getServerContext().downloadFile("studyconfig.xml"), configurationID);
      } catch (IOException e) {
        throw new StudyCasterException("Error retrieving configuration file", e);
      }
      scui.setInstructions(configuration.getInstructions());
      scui.setUploadFileFilter(configuration.getUploadFileFilter());

      scui.getProgressBarUI().setProgress(25);
      scui.getProgressBarUI().setTaskAppearance("Initializing user study app...", false);
      // TODO: Parameterize this.
      sc.startRecording(new ScreenCensor(
          configuration.getScreenCastWhiteList(),
          configuration.getScreenCastBlackList(),
          true, true, true));
      scui.getProgressBarUI().setProgress(50);
      scui.getProgressBarUI().setTaskAppearance("Downloading sample file...", false);

      openedFile = new File(new File(System.getProperty("java.io.tmpdir")),
          configuration.getOpenFileConfiguration().getLocalName());
      if (openedFile.exists()) {
        LOG.info("File to be downloaded already exists in temp directory");

        while (true) {
          download = scui.showDownloadOrExistingDialog(Util.getPathString(openedFile));
          LOG.log(Level.INFO, "User chose to {0}", (download ? "download new file" : "keep existing file"));
          // TODO: Move this mess out of here.
          if (download) {
            String path = openedFile.getPath();
            int dot = path.lastIndexOf('.');
            String basename = (dot < 0) ? path : path.substring(0, dot);
            String extension = (dot < 0) ? "" : path.substring(dot);
            File newName;
            int index = 1;
            do {
              newName = new File(basename + " (" + index + ")" + extension);
              index++;
            } while (newName.exists());
            if (!openedFile.renameTo(newName)) {
              scui.showMessageDialog("Open Sample File",
                  "<html>Failed to rename<br>" + Util.getPathString(openedFile) + "<br><br>" +
                  "If the file is still open, please close it and try again.</html>",
                  JOptionPane.WARNING_MESSAGE);
            } else {
              scui.showMessageDialog("Open Sample File",
                "<html>The old file<br>" + Util.getPathString(openedFile) + "<br>was renamed to<br>" +
                Util.getPathString(newName) + "</html>",
                JOptionPane.INFORMATION_MESSAGE);
              break;
            }
          } else {
            break;
          }
        }
      }
      if (download)
        sc.downloadFile(configuration.getOpenFileConfiguration().getRemoteName(), openedFile);
      scui.setDefaultFile(openedFile);
      lastModified1 = openedFile.lastModified();
      scui.getProgressBarUI().setProgress(75);
      scui.getProgressBarUI().setTaskAppearance("Opening sample file...", false);
      // TODO: Allow opening with a downloaded application.
      // TODO: When would this ever be the case?
      if (Util.fileAvailableExclusive(openedFile)) {
        Util.desktopOpenFile(openedFile,
            configuration.getOpenFileConfiguration().getErrorMessage());
      }
      lastModified2 = openedFile.lastModified();
      scui.getProgressBarUI().setProgress(100);
      scui.setUploadEnabled(true);
      scui.getProgressBarUI().setTaskAppearance("", false);
      scui.getProgressBarUI().setProgress(0);
    } catch (StudyCasterException e) {
      LOG.log(Level.SEVERE, "Can''t Load User Study", e);
      if (sc != null)
        sc.concludeStudy();
      scui.showMessageDialog("Can't Load User Study", e.getLocalizedMessage(),
          JOptionPane.WARNING_MESSAGE);
      scui.disposeUI();
      return;
    }
    LOG.log(Level.INFO, "User study loaded OK; experiment {0}", configuration.getName());

    UIAction action;
    boolean warnedAboutUnchanged = false;
    do {
      //scui.setMonitorStreamProgress(sc.getRecordingStream(), true);
      action = scui.waitForUserAction();
      //scui.setMonitorStreamProgress(sc.getRecordingStream(), false);
      if (action == UIAction.UPLOAD) {
        File selectedFile = scui.getSelectedFile();
        long nowLastModified = selectedFile.lastModified();
        boolean isSameFile = false;
        try {
          isSameFile = selectedFile.getCanonicalPath().equals(openedFile.getCanonicalPath());
        } catch (IOException e) {
          LOG.log(Level.WARNING, "Failed to check for filename equality", e);
        }
        if (download && isSameFile && (nowLastModified == lastModified1 || nowLastModified == lastModified2) && !warnedAboutUnchanged) {
          warnedAboutUnchanged = true;
          LOG.log(Level.INFO, "Got upload on unchanged file; exclusive={0}", Util.fileAvailableExclusive(selectedFile));

          scui.showMessageDialog("Upload",
                  "<html>Please edit, save, and close the file, then try again.<br><br>" +
                  "(The file should have opened in a new window, possibly in the background.)<br>" +
                  "</html>"
                  , JOptionPane.WARNING_MESSAGE);
        } else if (!Util.fileAvailableExclusive(selectedFile)) {
          LOG.info("Got upload on changed but still open file");
          scui.showMessageDialog("Upload",
                "<html>Please close the file, then try again.</html>"
                , JOptionPane.WARNING_MESSAGE);
        } else {
          LOG.info("Now starting upload");
          try {
            scui.getProgressBarUI().setTaskAppearance("Uploading file...", true);
            LOG.log(Level.INFO, "Uploading file {0}", selectedFile.getName());
            sc.uploadFile(selectedFile, "uploaded_" + Util.sanitizeFileNameComponent(selectedFile.getName()));
            scui.getProgressBarUI().setTaskAppearance("Finishing screencast upload...", true);
            sc.stopRecording();
            scui.setMonitorStreamProgress(sc.getRecordingStream(), true);
            sc.waitForScreenCastUpload();
            scui.setMonitorStreamProgress(sc.getRecordingStream(), false);
            scui.getProgressBarUI().setTaskAppearance("Uploading log...", true);
            LOG.info("Concluding after successful upload");
            sc.concludeStudy();
            scui.getProgressBarUI().setTaskAppearance("", false);
            scui.showConfirmationCodeDialog(sc.getServerContext().getLaunchTicket().toString(), true);
            scui.disposeUI();
          } catch (StudyCasterException e) {
            LOG.log(Level.SEVERE, "Failed to upload", e);
            scui.getProgressBarUI().setTaskAppearance("", false);
            scui.getProgressBarUI().setProgress(0);
            scui.showMessageDialog("Failed to Upload File", e.getLocalizedMessage(), JOptionPane.WARNING_MESSAGE);
          }
        }
      } else if (action == UIAction.CLOSE) {
        LOG.info("Got a close action");
      }
      scui.setUploadEnabled(true);
    } while (action != UIAction.CLOSE);
    sc.concludeStudy();
    // Shut down the force-shutdown thread in StudyCasterUI.
    System.exit(0);
  }
}
