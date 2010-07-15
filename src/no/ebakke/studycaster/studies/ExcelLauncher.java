package no.ebakke.studycaster.studies;

import no.ebakke.studycaster.StudyCaster;
import no.ebakke.studycaster.StudyCasterException;
import no.ebakke.studycaster.StudyCasterUI;

public class ExcelLauncher {
  public static void main(String args[]) {
    StudyCasterUI scui = new StudyCasterUI();
    StudyCaster sc = null;
    try {
      sc = new StudyCaster("http://www.sieuferd.com/studycaster/server.php");
      scui.getProgressBarUI().setTaskAppearance("", false);
      scui.setUploadEnabled(true);
      sc.downloadFile("exflat.xls");
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
