package no.ebakke.studycaster.studies;

import no.ebakke.studycaster.StudyCaster;

public class ExcelLauncher {
  public static void main(String args[]) throws Exception {
    StudyCaster sc = new StudyCaster("http://www.sieuferd.com/studycaster/server.php");
    // StudyCaster.log.info("message from the launcher");
    /*
    try {
      new FileInputStream(new File(""));
    } catch (Exception e) {
      StudyCaster.log.log(Level.WARNING, "yo", e);
    }
    */
    sc.concludeStudy();
  }
}
