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
import java.util.logging.Logger;

public final class RecordingConverter {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  private static final int   FRAMERATE_LIMIT = 15;
  public static final String FILE_EXTENSION  = "mp4";

  private RecordingConverter() { }

  public static void convert(InputStream input, String fileTo, int speedUpFactor)
      throws IOException
  {
    CaptureDecoder dec = new CaptureDecoder(input);
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
    outStreamCoder.setTimeBase(IRational.make(1, FRAMERATE_LIMIT));

    outStreamCoder.open();
    outContainer.writeHeader();

    long previousTimeStampMicros = -1;
    final BufferedImage image = new BufferedImage(
        dec.getDimension().width, dec.getDimension().height, BufferedImage.TYPE_3BYTE_BGR);
    try {
      while (dec.nextFrame(image)) {
        final long currentTimeStampMicros = (dec.getCurrentTimeMillis() * 1000L) / speedUpFactor;
        if (speedUpFactor > 1 && previousTimeStampMicros >= 0 &&
            currentTimeStampMicros - previousTimeStampMicros < 1000000L / FRAMERATE_LIMIT)
        {
          System.out.print(" ");
          continue;
        } else {
          System.err.print(".");
          dec.blinkIndicators();
        }

        final IPacket packet = IPacket.make();
        final IConverter converter = ConverterFactory.createConverter(image, pixelFormat);
        final IVideoPicture outFrame = converter.toPicture(image, currentTimeStampMicros);
        outStreamCoder.encodeVideo(packet, outFrame, -1);
        if (packet.isComplete())
          outContainer.writePacket(packet);
        previousTimeStampMicros = currentTimeStampMicros;
      }
    } catch (IOException e) {
      System.err.println("ops");
      if (!(e instanceof EOFException))
        e.printStackTrace();
      hadError = true;
      LOG.warning("Incomplete screencast file");
    }
    outContainer.writeTrailer();
    outStreamCoder.close();
    outContainer.close();
    input.close();
    if (!hadError)
      System.err.println("ok");
  }
}
