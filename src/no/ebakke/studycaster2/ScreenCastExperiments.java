package no.ebakke.studycaster2;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

public class ScreenCastExperiments {
  public static void main(String args[]) throws Exception {
    Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    Robot r = new Robot();

    for (int i = 0; i < 100; i++) {
      System.out.println("Capturing a frame");
      BufferedImage bi = r.createScreenCapture(screenRect);
    }


  }
}
