package no.ebakke.studycaster.driver;

import javax.swing.JOptionPane;
import no.ebakke.studycaster.screencasting.WindowEnumerator;
import no.ebakke.studycaster.ui.OldStatusFrame;

public final class Main {
  private Main() { }
  
  public static void main(String args[]) {
    OldStatusFrame.setSystemLookAndFeel();
    System.out.println("Hello, World!");
    OldStatusFrame osf = new OldStatusFrame("These are indeed some instructions.");
    osf.setVisible(true);
    String windows = WindowEnumerator.test();
    JOptionPane.showMessageDialog(osf.getPositionDialog(), "THESE ARE YOUR WINDOWS:\n" + windows);
  }
}
