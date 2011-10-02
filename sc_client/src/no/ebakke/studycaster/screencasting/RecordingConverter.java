package no.ebakke.studycaster.screencasting;

import com.xuggle.xuggler.Configuration;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;
import java.awt.image.BufferedImage;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import no.ebakke.studycaster.api.StudyCaster;

public final class RecordingConverter {
  public static final String FILE_EXTENSION = "mp4";

  private RecordingConverter() { }

  public static void convert(InputStream input, String fileTo, int speedUpFactor)
      throws IOException
  {
    CaptureDecoder dec = new CaptureDecoder(input);
    System.err.format("Converting to %s at %dx speedup: \n", fileTo, speedUpFactor);
    boolean hadError = false;
    IContainer outContainer = IContainer.make();
    if (outContainer.open(fileTo, IContainer.Type.WRITE, null) < 0)
      throw new IOException("Could not open output file");

    IStream outStream = outContainer.addNewStream(0);
    IStreamCoder outStreamCoder = outStream.getStreamCoder();

    outStreamCoder.setCodec(ICodec.ID.CODEC_ID_H264);

    // TODO: Allow a preset file to be specified on the command line.
    InputStream in = RecordingConverter.class.getResourceAsStream(
        "/no/ebakke/studycaster/screencasting/libx264.ffpreset");
    if (in == null)
      throw new IOException("Could not find bundled ffmpeg preset file.");
    Properties ffpreset = new Properties();
    ffpreset.load(in);
    if (Configuration.configure(ffpreset, outStreamCoder) != 0)
      throw new IOException("Could not configure ffmpeg from preset file");

    final IPixelFormat.Type pixelFormat = IPixelFormat.Type.YUV420P; /* YUV420P, YUV422P, YUV444P */
    outStreamCoder.setPixelType(pixelFormat);
    outStreamCoder.setWidth(dec.getDimension().width);
    outStreamCoder.setHeight(dec.getDimension().height);
    outStreamCoder.setTimeBase(IRational.make(1, 24));

    outStreamCoder.open();
    outContainer.writeHeader();

    BufferedImage image;
    int index = 0;
    try {
      while ((image = dec.nextFrame()) != null) {
        //ImageDebugDialog.showImage(image);
        index++;
        if (index % speedUpFactor != 0)
          continue;

        BufferedImage converted = convertToType(image, BufferedImage.TYPE_3BYTE_BGR);
        IPacket packet = IPacket.make();
        IConverter converter = ConverterFactory.createConverter(converted, pixelFormat);
        IVideoPicture outFrame =
            converter.toPicture(converted, (dec.getCurrentTimeMillis() * 1000L) / speedUpFactor);
        outFrame.setQuality(0);
        outStreamCoder.encodeVideo(packet, outFrame, 0);
        if (packet.isComplete())
          outContainer.writePacket(packet);

        System.err.print(".");
      }
    } catch (IOException e) {
      System.err.println("ops");
      if (!(e instanceof EOFException))
        e.printStackTrace();
      hadError = true;
      StudyCaster.log.warning("Incomplete screencast file");
    }
    outContainer.writeTrailer();
    outStreamCoder.close();
    outContainer.close();
    input.close();
    if (!hadError)
      System.err.println("ok");
  }

  private static BufferedImage convertToType(BufferedImage sourceImage, int targetType) {
    // From xuggler documentation.
    if (sourceImage.getType() == targetType) {
      return sourceImage;
    } else {
      BufferedImage ret =
          new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), targetType);
      ret.getGraphics().drawImage(sourceImage, 0, 0, null);
      return ret;
    }
  }
}
