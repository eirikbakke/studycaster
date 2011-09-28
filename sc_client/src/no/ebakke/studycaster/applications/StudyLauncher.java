package no.ebakke.studycaster.applications;

import java.io.File;
import javax.swing.filechooser.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import no.ebakke.studycaster.api.StudyConfiguration;
import no.ebakke.studycaster.api.ServerContext;
import no.ebakke.studycaster.api.StudyCaster;
import no.ebakke.studycaster.api.StudyCasterException;
import no.ebakke.studycaster.screencasting.ScreenCensor;
import no.ebakke.studycaster.ui.StudyCasterUI;
import no.ebakke.studycaster.ui.StudyCasterUI.UIAction;
import no.ebakke.studycaster.util.Util;

public class StudyLauncher {
  private static final Logger log = Logger.getLogger("no.ebakke.studycaster");
  private StudyConfiguration configuration;
  private ServerContext serverContext;

  public StudyLauncher(String configurationID) throws StudyCasterException {
    serverContext = new ServerContext();
    try {
      configuration =
          new StudyConfiguration(serverContext.downloadFile("studyconfig.xml"), configurationID);
    } catch (IOException e) {
      throw new StudyCasterException("Error retrieving configuration file", e);
    }
  }

  public static void main(String args[]) {
    StudyLauncher csc;

    // TODO: Get rid of this.
    args = new String[] { "5782" };

    if (args.length != 1) {
      System.err.println("Usage: ConfigurateStudyCaster <configuration ID>");
      return;
    }
    try {
      csc = new StudyLauncher(args[0]);
      csc.runStudy();
    } catch (StudyCasterException e) {
      log.log(Level.SEVERE, "Failed to initialize StudyCaster", e);
      return;
    }
  }

  // TODO: Open the window before anything else, to allow force-quitting.
  public void runStudy() throws StudyCasterException {
    StudyCaster.log.info("Entering initial log message to promote fail-fast behavior of potential ConsoleTee bugs.");

    StudyCasterUI scui;

    try {
      FileFilter filter = configuration.getUploadFileFilter();
      scui = new StudyCasterUI(configuration.getInstructions(), filter);
    } catch (StudyCasterException e) {
      StudyCaster.log.log(Level.SEVERE, "Unexpected exception during UI initialization", e);
      System.exit(0);
      return;
    }

    if (scui.wasClosed()) {
      try {
        StudyCaster sc = new StudyCaster();
        sc.enterRemoteLogRecord("User declined at consent dialog");
        sc.concludeStudy();
      } catch (StudyCasterException e) {
        StudyCaster.log.warning("Failed to send consent-declined log message");
      }
      System.exit(0);
    }

    StudyCaster sc = null;
    long lastModified1, lastModified2;
    boolean download = true;
    File openedFile;
    try {
      scui.getProgressBarUI().setProgress(25);
      scui.getProgressBarUI().setTaskAppearance("Initializing user study app...", false);
      sc = new StudyCaster();
      sc.startRecording(new ScreenCensor(
          Arrays.asList(new String[] {"StudyCaster", "User Study Console", "Excel", "Calc", "Numbers", "Gnumeric", "KSpread", "Quattro", "Mesa", "Spreadsheet Study Application", "StudyCaster"}),
          Arrays.asList(new String[] {"Firefox", "Internet Explorer", "Outlook", "Chrome", "Safari", "Upload and Retrieve Confirmation Code",
          "Open Sample Document"}),
          true));
      scui.getProgressBarUI().setProgress(50);
      scui.getProgressBarUI().setTaskAppearance("Downloading sample document...", false);

      openedFile = new File(new File(System.getProperty("java.io.tmpdir")), configuration.getOpenFileLocalName());
      if (openedFile.exists()) {
        StudyCaster.log.info("File to be downloaded already exists in temp directory");

        while (true) {
          download = scui.showDownloadOrExistingDialog(Util.getPathString(openedFile));
          StudyCaster.log.log(Level.INFO, "User chose to {0}", (download ? "download new file" : "keep existing file"));
          // TODO: Move this mess out of here.
          if (download) {
            String path = openedFile.getPath();
            int dot = path.lastIndexOf(".");
            String basename = (dot < 0) ? path : path.substring(0, dot);
            String extension = (dot < 0) ? "" : path.substring(dot);
            File newName;
            int index = 1;
            do {
              newName = new File(basename + " (" + index + ")" + extension);
              index++;
            } while (newName.exists());
            if (!openedFile.renameTo(newName)) {
              scui.showMessageDialog("Open Sample Document",
                  "<html>Failed to rename<br>" + Util.getPathString(openedFile) + "<br><br>" +
                  "If the file is still open, please close it and try again.</html>",
                  JOptionPane.WARNING_MESSAGE);
            } else {
              scui.showMessageDialog("Open Sample Document",
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
        sc.downloadFile(configuration.getOpenFileRemoteName(), openedFile);
      scui.setDefaultFile(openedFile);
      lastModified1 = openedFile.lastModified();
      scui.getProgressBarUI().setProgress(75);
      scui.getProgressBarUI().setTaskAppearance("Opening sample document...", false);
      // TODO: Allow opening with a downloaded application.
      // TODO: When would this ever be the case?
      if (Util.fileAvailableExclusive(openedFile))
        Util.desktopOpenFile(openedFile, configuration.getOpenFileRequirement());
      lastModified2 = openedFile.lastModified();
      scui.getProgressBarUI().setProgress(100);
      scui.setUploadEnabled(true);
      scui.getProgressBarUI().setTaskAppearance("", false);
      scui.getProgressBarUI().setProgress(0);
    } catch (StudyCasterException e) {
      StudyCaster.log.log(Level.SEVERE, "Can't Load User Study", e);
      if (sc != null)
        sc.concludeStudy();
      sc.enterRemoteLogRecord("Failed to load user study");
      scui.showMessageDialog("Can't Load User Study", e.getLocalizedMessage(), JOptionPane.WARNING_MESSAGE);
      scui.disposeUI();
      return;
    }
    sc.enterRemoteLogRecord("User study loaded OK; experiment " + configuration.getName());

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
          StudyCaster.log.log(Level.WARNING, "Failed to check for filename equality", e);
        }
        if (download && isSameFile && (nowLastModified == lastModified1 || nowLastModified == lastModified2) && !warnedAboutUnchanged) {
          warnedAboutUnchanged = true;
          StudyCaster.log.log(Level.INFO, "Got upload on unchanged document; exclusive={0}", Util.fileAvailableExclusive(selectedFile));

          scui.showMessageDialog("Upload",
                  "<html>Please edit, save, and close the spreadsheet document, then try again.<br><br>" +
                  "(The document should have opened in a new window, possibly in the background.)<br>" +
                  "</html>"
                  , JOptionPane.WARNING_MESSAGE);
        } else if (!Util.fileAvailableExclusive(selectedFile)) {
          StudyCaster.log.info("Got upload on changed but still open document");
          scui.showMessageDialog("Upload",
                "<html>Please close the spreadsheet document, then try again.</html>"
                , JOptionPane.WARNING_MESSAGE);
        } else {
          sc.enterRemoteLogRecord("Now starting upload");
          StudyCaster.log.info("Now starting upload");
          try {
            scui.getProgressBarUI().setTaskAppearance("Uploading document...", true);
            StudyCaster.log.log(Level.INFO, "Uploading file {0}", selectedFile.getName());
            sc.uploadFile(selectedFile, "uploaded_" + Util.sanitizeFileNameComponent(selectedFile.getName()));
            scui.getProgressBarUI().setTaskAppearance("Finishing screencast upload...", true);
            sc.stopRecording();
            scui.setMonitorStreamProgress(sc.getRecordingStream(), true);
            sc.waitForScreenCastUpload();
            scui.setMonitorStreamProgress(sc.getRecordingStream(), false);
            scui.getProgressBarUI().setTaskAppearance("Uploading log...", true);
            sc.enterRemoteLogRecord("Concluding after successful upload");
            sc.concludeStudy();
            scui.getProgressBarUI().setTaskAppearance("", false);
            scui.showConfirmationCodeDialog(sc.getServerContext().getLaunchTicket().toString(), true);
            scui.disposeUI();
          } catch (StudyCasterException e) {
            StudyCaster.log.log(Level.SEVERE, "Failed to upload", e);
            scui.getProgressBarUI().setTaskAppearance("", false);
            scui.getProgressBarUI().setProgress(0);
            scui.showMessageDialog("Failed to Upload File", e.getLocalizedMessage(), JOptionPane.WARNING_MESSAGE);
          }
        }
      } else if (action == UIAction.CLOSE) {
        sc.enterRemoteLogRecord("Got a close action");
      }
      scui.setUploadEnabled(true);
    } while (action != UIAction.CLOSE);
    sc.concludeStudy();
    // Shut down the force-shutdown thread in StudyCasterUI.
    System.exit(0);
  }
}
