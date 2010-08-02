package no.ebakke.studycaster2;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.IRational;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

public class ScreenCastExperiments {
  public static void main(String args[]) throws Exception {
    /*
    Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    Robot r = new Robot();

    for (int i = 0; i < 100; i++) {
      System.out.println("Capturing a frame");
      BufferedImage bi = r.createScreenCapture(screenRect);
    }
    */

   final IRational FRAME_RATE = IRational.make(5, 1);
   final int SECONDS_TO_RUN_FOR = 5;

   final Robot robot = new Robot();
   final Toolkit toolkit = Toolkit.getDefaultToolkit();
   final Rectangle screenBounds = new Rectangle(toolkit.getScreenSize());

   // First, let's make a IMediaWriter to write the file.
   final IMediaWriter writer = ToolFactory.makeWriter("output.mp4");

   // We tell it we're going to add one video stream, with id 0,
   // at position 0, and that it will have a fixed frame rate of
   // FRAME_RATE.
   writer.addVideoStream(0, 0, FRAME_RATE, screenBounds.width, screenBounds.height);

   // Now, we're going to loop
   long startTime = System.nanoTime();
   for (int index = 0; index < SECONDS_TO_RUN_FOR * FRAME_RATE.getDouble(); index++) {
     // take the screen shot
     BufferedImage screen = robot.createScreenCapture(screenBounds);

     // convert to the right image type

     BufferedImage bgrScreen = convertToType(screen, BufferedImage.TYPE_3BYTE_BGR);

     // encode the image to stream #0
     writer.encodeVideo(0, bgrScreen, System.nanoTime()-startTime, TimeUnit.NANOSECONDS);
      System.out.println("encoded image: " +index);

     // sleep for framerate milliseconds
     Thread.sleep((long) (1000 / FRAME_RATE.getDouble()));
   }
   // Finally we tell the writer to close and write the trailer if needed.
   writer.close();

  }

  public static BufferedImage convertToType(BufferedImage sourceImage, int targetType) {
    BufferedImage image;
    // if the source image is already the target type, return the source image
    if (sourceImage.getType() == targetType) {
      image = sourceImage;
    } else {
      // otherwise create a new image of the target type and draw the new image
      image = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), targetType);
      image.getGraphics().drawImage(sourceImage, 0, 0, null);
    }

    return image;
  }

}
