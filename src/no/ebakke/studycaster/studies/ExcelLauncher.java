package no.ebakke.studycaster.studies;

import java.io.File;
import no.ebakke.studycaster.StudyCaster;
import no.ebakke.studycaster.StudyCasterException;
import no.ebakke.studycaster.StudyCasterUI;

public class ExcelLauncher {
  private static final String instructions =
    "<html><b>Instructions:</b><ol>" +
    "<li>Edit the spreadsheet that opened in another window." +
    "<li>Save the spreadsheet." +
    "<li>Click the button below." +
    "<li>Paste the confirmation code into the HIT." +
    "</ol><p align=\"right\">Thanks!<br><a href=\"mailto:ebakke@mit.edu\">ebakke@mit.edu</a></p></html>";

  public static void main(String args[]) {
    StudyCasterUI scui = new StudyCasterUI(instructions);
    StudyCaster sc = null;
    try {
      scui.getProgressBarUI().setTaskAppearance("Initializing user study app...", false);
      sc = new StudyCaster("http://www.sieuferd.com/studycaster/server.php");
      scui.getProgressBarUI().setProgress(33);
      scui.getProgressBarUI().setTaskAppearance("Downloading sample document...", false);
      File excelFile = sc.downloadFile("exflat.xls");
      scui.getProgressBarUI().setProgress(67);
      scui.getProgressBarUI().setTaskAppearance("Opening sample document...", false);
      sc.desktopOpenFile(excelFile, "Excel or a compatible spreadsheet application");
      scui.getProgressBarUI().setProgress(100);
      scui.setUploadEnabled(true);
      scui.getProgressBarUI().setTaskAppearance("", false);
      scui.getProgressBarUI().setProgress(0);
    } catch (StudyCasterException e) {
      scui.exitWithError("Can't Load User Study", e);
      e.printStackTrace();
      if (sc != null)
        sc.concludeStudy();
      return;
    }
    sc.concludeStudy();
  }
}
