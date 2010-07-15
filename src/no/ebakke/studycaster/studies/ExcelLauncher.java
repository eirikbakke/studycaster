package no.ebakke.studycaster.studies;

import no.ebakke.studycaster.StudyCaster;
import no.ebakke.studycaster.StudyCasterUI;
import no.ebakke.studycaster.StudyDefinition;

public class ExcelLauncher {
  public static void main(String args[]) throws Exception {
    StudyDefinition def = new StudyDefinition() {
      @Override
      public String getServerURL() {
        return "http://www.sieuferd.com/studycaster/server.php";
      }

      @Override
      public void runStudy(StudyCaster sc) {
        throw new UnsupportedOperationException("Not supported yet.");
      }
    };
    StudyCasterUI scui = new StudyCasterUI(def);
  }
}
