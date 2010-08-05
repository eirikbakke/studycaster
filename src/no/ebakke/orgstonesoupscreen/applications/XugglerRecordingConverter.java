package no.ebakke.orgstonesoupscreen.applications;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.IRational;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import no.ebakke.orgstonesoupscreen.RecordingStream;

public final class XugglerRecordingConverter {
  private XugglerRecordingConverter() { }
  
  public static void main(String args[]) throws IOException {
    convert("C:\\DOCUME~1\\ADMINI~1\\LOCALS~1\\Temp\\sc_6585879656423605096.tmp", "z:\\rectest\\moo.mp4");
  }

  public static void convert(String fileFrom, String fileTo) throws IOException {
    InputStream inStream = new FileInputStream(fileFrom);
    RecordingStream recStr = new RecordingStream(inStream);
    int width = (int) recStr.getArea().getWidth();
    int height = (int) recStr.getArea().getHeight();
    int frameRate = 4;
    int index = 1;
    long time = 0;

    IMediaWriter writer = ToolFactory.makeWriter(fileTo);
    writer.addVideoStream(0, 0, IRational.make(frameRate, 1), width, height);

    BufferedImage image;
    while (!recStr.readFrameData()) {
      image = new BufferedImage(recStr.getArea().width, recStr.getArea().height, BufferedImage.TYPE_INT_RGB);
      image.setRGB(0, 0, recStr.getArea().width, recStr.getArea().height, recStr.getFrameData(), 0, recStr.getArea().width);

      BufferedImage bgrScreen = convertToType(image, BufferedImage.TYPE_3BYTE_BGR);

      writer.encodeVideo(0, bgrScreen, time, TimeUnit.MILLISECONDS);
      time += 1000 / frameRate;
      System.out.println("encoded image: " + index++);
    }

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
