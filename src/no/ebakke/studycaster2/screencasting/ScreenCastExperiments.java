package no.ebakke.studycaster2.screencasting;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.net.URL;
import no.ebakke.studycaster2.PostOutputStream;
import no.ebakke.studycaster2.ServerContext;
import no.ebakke.studycaster2.StringSequenceGenerator;

public class ScreenCastExperiments {
  public static void main(String args[]) throws Exception {
    Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    Robot r = new Robot();
    OutputStream os = new FileOutputStream("z:\\rectest\\testfile_newcodec.nc");
    //ServerContext sc = new ServerContext(new URL("http://www.sieuferd.com/studycaster/server.php"));
    //OutputStream os = new PostOutputStream(new StringSequenceGenerator("screencast_", ".ebc"), sc);
    CodecEncoder comp = new CodecEncoder(os, new Dimension(screenRect.width, screenRect.height));

    long bef, diff;
    BufferedImage bi = null;
    for (int i = 0; i < 300; i++) {
      bef = System.currentTimeMillis();
      //for (int j = 0; j < 10; j++) {
      bi = r.createScreenCapture(screenRect);
      //}
      diff = System.currentTimeMillis() - bef;
      System.out.println("capture took " + diff);
      /*
      bef = System.currentTimeMillis();
      convertToType(bi, BufferedImage.TYPE_BYTE_GRAY);
      diff = System.currentTimeMillis() - bef;
      System.out.println("conversion took " + diff);
      */
      /*
      int rawData[] = new int[screenRect.width * screenRect.height];
      bef = System.currentTimeMillis();
      for (int j = 0; j < 10; j++) {
        bi.getRGB(0, 0, screenRect.width, screenRect.height, rawData, 0, screenRect.width);
      }
      diff = System.currentTimeMillis() - bef;
      System.out.println("copy took " + diff);
      */
      bef = System.currentTimeMillis();
      //for (int j = 0; j < 10; j++) {
        comp.compressFrame(bi, System.currentTimeMillis());
      //}
      diff = System.currentTimeMillis() - bef;
      System.out.println("compress took " + diff);
    }
    os.close();

/*
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
*/
  }

  public static BufferedImage convertToType(BufferedImage sourceImage, int targetType) {
    BufferedImage image;
    // if the source image is already the target type, return the source image
    if (sourceImage.getType() == targetType) {
      image = sourceImage;
    } else {
      // otherwise create a new image of the target type and draw the new image
      image = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), targetType);
      if (!image.getGraphics().drawImage(sourceImage, 0, 0, null))
        throw new AssertionError("Expected immediate image conversion");
    }
    return image;
  }

}
