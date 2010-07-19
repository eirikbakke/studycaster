package no.ebakke.studycaster.studies;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import no.ebakke.studycaster.StudyCaster;
import no.ebakke.studycaster.StudyCasterException;
import no.ebakke.studycaster.StudyCasterUI;
import no.ebakke.studycaster.StudyCasterUI.UIAction;
import no.ebakke.studycaster.util.Util;

public class ExcelLauncher {
  private static final String instructions =
    "<html><b>Instructions:</b><ol>" +
    "<li>Edit the spreadsheet that opened in another window<br>" +
    "(according to the instructions in the HIT)." +
    "<li>Save and close the spreadsheet<br>" +
    "<li>Click the button below." +
    "<li>Paste the confirmation code into the HIT." +
    "</ol><p align=\"right\">Thanks!<br><a href=\"mailto:ebakke@mit.edu\">ebakke@mit.edu</a></p></html>";

  public static void main(String args[]) {
    StudyCasterUI scui = new StudyCasterUI(instructions);
    StudyCaster sc = null;
    long lastModified1, lastModified2;
    File excelFile;
    try {
      scui.getProgressBarUI().setTaskAppearance("Initializing user study app...", false);
      sc = new StudyCaster("http://www.sieuferd.com/studycaster/server.php");
      scui.getProgressBarUI().setProgress(33);
      scui.getProgressBarUI().setTaskAppearance("Downloading sample document...", false);
      excelFile = sc.downloadFile("currencies.xls");
      lastModified1 = excelFile.lastModified();
      scui.getProgressBarUI().setProgress(67);
      scui.getProgressBarUI().setTaskAppearance("Opening sample document...", false);
      sc.desktopOpenFile(excelFile, "Excel or a compatible spreadsheet application");
      lastModified2 = excelFile.lastModified();
      scui.getProgressBarUI().setProgress(100);
      scui.setUploadEnabled(true);
      scui.getProgressBarUI().setTaskAppearance("", false);
      scui.getProgressBarUI().setProgress(0);
      sc.startRecording();
    } catch (StudyCasterException e) {
      StudyCaster.log.log(Level.SEVERE, "Can't Load User Study.", e);
      if (sc != null)
        sc.concludeStudy();
      scui.showMessageDialog("Can't Load User Study", e.getLocalizedMessage(), JOptionPane.WARNING_MESSAGE, true);
      scui.disposeUI();
      return;
    }

    UIAction action;
    do {
      action = scui.waitForUserAction();
      if (action == UIAction.UPLOAD) {
        long nowLastModified = excelFile.lastModified();
        if (nowLastModified == lastModified1 || nowLastModified == lastModified2) {
          StudyCaster.log.info("Got upload on unchanged document; exclusive=" + Util.fileAvailableExclusive(excelFile));

          scui.showMessageDialog("Upload",
                  "<html>Please edit, save, and close the Excel document, then try again.<br><br>" +
                  "(The document should have opened in a new window, possibly in the background.)<br>" +
                  "</html>"
                  , JOptionPane.WARNING_MESSAGE, false);
        } else if (!Util.fileAvailableExclusive(excelFile)) {
          StudyCaster.log.info("Got upload on changed but still open document.");
          scui.showMessageDialog("Upload",
                "<html>Please close the Excel document, then try again.</html>"
                , JOptionPane.WARNING_MESSAGE, false);
        } else {
          scui.getProgressBarUI().setTaskAppearance("Preparing to upload...", true);
          sc.stopRecording();
          try {
            scui.getProgressBarUI().setTaskAppearance("Uploading document...", true);
            StudyCaster.log.info("Uploaded file now last modified " + sc.getServerTimeFormat(nowLastModified));
            sc.uploadFile(excelFile,  "saveddoc_" + sc.getCurrentRunTicket() + ".xls");
            scui.getProgressBarUI().setTaskAppearance("Uploading screencast...", true);
            sc.uploadFile(sc.getRecordFile(), "screencast_" + sc.getCurrentRunTicket() + ".rec");
            scui.getProgressBarUI().setTaskAppearance("Uploading log...", true);
            sc.concludeStudy();
            scui.getProgressBarUI().setTaskAppearance("", false);
            scui.showConfirmationCodeDialog(sc.getCurrentRunTicket().toString(), true);
            scui.disposeUI();
          } catch (StudyCasterException e) {
            scui.showMessageDialog("Failed to upload file", e.getLocalizedMessage(), JOptionPane.WARNING_MESSAGE, false);
          }
          scui.getProgressBarUI().setTaskAppearance("", false);
        }
      }
      scui.setUploadEnabled(true);
    } while (action != UIAction.CLOSE);
    sc.concludeStudy();
    // Shut down the force-shutdown thread in StudyCasterUI.
    System.exit(0);
  }
}
