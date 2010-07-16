package no.ebakke.studycaster.studies;

import java.io.File;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import no.ebakke.studycaster.StudyCaster;
import no.ebakke.studycaster.StudyCasterException;
import no.ebakke.studycaster.StudyCasterUI;
import no.ebakke.studycaster.StudyCasterUI.UserAction;
import no.ebakke.studycaster.util.Util;

public class ExcelLauncher {
  private static final String instructions =
    "<html><b>Instructions:</b><ol>" +
    "<li>Edit the spreadsheet that opened in another window<br>" +
    "(according to the instructions in the HIT)." +
    "<li>Save and close the spreadsheet." +
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
      excelFile = sc.downloadFile("exflat.xls");
      lastModified1 = excelFile.lastModified();
      scui.getProgressBarUI().setProgress(67);
      scui.getProgressBarUI().setTaskAppearance("Opening sample document...", false);
      sc.desktopOpenFile(excelFile, "Excel or a compatible spreadsheet application");
      lastModified2 = excelFile.lastModified();
      scui.getProgressBarUI().setProgress(100);
      scui.setUploadEnabled(true);
      scui.getProgressBarUI().setTaskAppearance("", false);
      scui.getProgressBarUI().setProgress(0);
    } catch (StudyCasterException e) {
      scui.showMessageDialog("Can't Load User Study", e.getLocalizedMessage(), JOptionPane.WARNING_MESSAGE);
      scui.disposeUI();
      e.printStackTrace();
      if (sc != null)
        sc.concludeStudy();
      return;
    }

    boolean exit = false;
    UserAction action;
    do {
      action = scui.waitForUserAction();
      if (action == UserAction.UPLOAD) {
        boolean unchanged = (excelFile.lastModified() == lastModified1 || excelFile.lastModified() == lastModified2);
        boolean stillOpen = Util.fileAvailableExclusive(excelFile);

        if (unchanged) {
          StudyCaster.log.info("Got upload on unchanged document.");
          scui.showMessageDialog("Upload",
                  "<html>Please edit, save, and close the Excel document, then try again.<br><br>" +
                  "(The document should have opened in a new window, possibly in the background.)</html>"
                  , JOptionPane.WARNING_MESSAGE);
          if (!stillOpen) {
            scui.getProgressBarUI().setTaskAppearance("Reopening document...", true);
            try {
              StudyCaster.log.info("Attempting to reopen document.");
              sc.desktopOpenFile(excelFile, "Excel or a compatible spreadsheet application");
              lastModified2 = excelFile.lastModified();
            } catch (StudyCasterException e) {
              StudyCaster.log.log(Level.WARNING, "Error trying to reopen document.", e);
            }
            scui.getProgressBarUI().setTaskAppearance("", false);
          }
        } else if (stillOpen) {
          StudyCaster.log.info("Got upload on changed but still open document.");
          scui.showMessageDialog("Upload",
                "<html>Please close the Excel document, then try again.</html>"
                , JOptionPane.WARNING_MESSAGE);
        } else {
          scui.getProgressBarUI().setTaskAppearance("Uploading document...", true);
          try {
            sc.uploadFile(excelFile);
            scui.showMessageDialog("Upload", "Upload successful", JOptionPane.INFORMATION_MESSAGE);
            //scui.disposeUI(); // Can't do this here.
          } catch (StudyCasterException e) {
            scui.showMessageDialog("Failed to upload file", e.getLocalizedMessage(), JOptionPane.WARNING_MESSAGE);
          }
          scui.getProgressBarUI().setTaskAppearance("", false);
        }
      }
    } while (action != UserAction.CLOSE);
    sc.concludeStudy();
  }
}
