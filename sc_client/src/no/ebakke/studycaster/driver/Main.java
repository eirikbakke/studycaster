package no.ebakke.studycaster.driver;

import javax.swing.JOptionPane;
import no.ebakke.studycaster.screencasting.WindowEnumerator;
import no.ebakke.studycaster.ui.OldStatusFrame;

public final class Main {
  private Main() { }
  
  public static void main(String args[]) {
    OldStatusFrame.setSystemLookAndFeel();
    System.out.println("Hello, World!");
    WindowEnumerator.test();
    OldStatusFrame osf = new OldStatusFrame("These are indeed some instructions.");
    osf.setVisible(true);
    JOptionPane.showMessageDialog(osf.getPositionDialog(), "Hello, World.");
  }
}
