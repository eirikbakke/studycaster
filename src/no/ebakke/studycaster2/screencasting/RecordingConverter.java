package no.ebakke.studycaster2.screencasting;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import no.ebakke.studycaster2.PostInputStream;
import no.ebakke.studycaster2.ServerContext;
import no.ebakke.studycaster2.StringSequenceGenerator;

public final class RecordingConverter {
  private RecordingConverter() { }
  
  public static void main(String args[]) throws Exception {
    newConvert("z:\\rectest\\testfile_newcodec.nc", "z:\\rectest\\testfile_newcodec.ogv");
  }

  public static void newConvert(String fileFrom, String fileTo) throws Exception {
    //ServerContext sc = new ServerContext(new URL("http://www.sieuferd.com/studycaster/server.php"));
    //InputStream inStream = new PostInputStream(new StringSequenceGenerator("uploads/ea712a69cda9/screencast_", ".ebc"), sc);
    InputStream inStream = new FileInputStream(fileFrom);

    CodecDecoder dec = new CodecDecoder(inStream);

    IContainer outContainer = IContainer.make();
    if (outContainer.open(fileTo, IContainer.Type.WRITE, null) < 0)
      throw new IOException("Could not open output file");
    IStream outStream = outContainer.addNewStream(0);
    IStreamCoder outStreamCoder = outStream.getStreamCoder();
    /*
     * com.xuggle.xuggler.ICodec@53610016[type=CODEC_TYPE_VIDEO;id=CODEC_ID_THEORA;name=theora
     * com.xuggle.xuggler.ICodec@53617504[type=CODEC_TYPE_VIDEO;id=CODEC_ID_THEORA;name=libtheora
     */
    //ICodec codec = ICodec.findDecodingCodec(ICodec.ID.CODEC_ID_THEORA);
    ICodec codec = ICodec.guessEncodingCodec(null, null, fileTo, null, ICodec.Type.CODEC_TYPE_VIDEO);
    System.out.println(codec);
    IRational frameRate = IRational.make(24, 1);
    IPixelFormat.Type pixelFormat = IPixelFormat.Type.YUV420P; /* YUV420P, YUV422P, YUV444P */
    outStreamCoder.setCodec(codec);
    outStreamCoder.setNumPicturesInGroupOfPictures(10);
    outStreamCoder.setBitRate(20000000);
    outStreamCoder.setBitRateTolerance(9000);
    outStreamCoder.setPixelType(pixelFormat);
    outStreamCoder.setWidth(dec.getDimension().width);
    outStreamCoder.setHeight(dec.getDimension().height);
    outStreamCoder.setFlag(IStreamCoder.Flags.FLAG_QSCALE, false);
    outStreamCoder.setGlobalQuality(0);
    outStreamCoder.setFrameRate(frameRate);
    outStreamCoder.setTimeBase(IRational.make(frameRate.getDenominator(), frameRate.getNumerator()));
    outStreamCoder.open();
    outContainer.writeHeader();


    BufferedImage image;
    int index = 0;
    while ((image = dec.nextFrame()) != null) {
      BufferedImage converted = convertToType(image, BufferedImage.TYPE_3BYTE_BGR);

      IPacket packet = IPacket.make();
      IConverter converter = ConverterFactory.createConverter(converted, pixelFormat);
      IVideoPicture outFrame = converter.toPicture(converted, dec.getCurrentTimeMillis() * 1000L);
      outFrame.setQuality(0);
      outStreamCoder.encodeVideo(packet, outFrame, 0);
      if (packet.isComplete())
        outContainer.writePacket(packet);

      //writer.encodeVideo(0, bgrScreen, dec.getCurrentTimeMillis(), TimeUnit.MILLISECONDS);
      System.out.println("encoded image: " + index++);
    }

    outContainer.writeTrailer();
    outStreamCoder.close();
    outContainer.close();
    inStream.close();
  }

  public static void convert(String fileFrom, String fileTo) throws IOException {
    InputStream inStream = new FileInputStream(fileFrom);
    CodecDecoder dec = new CodecDecoder(inStream);

    int width = dec.getDimension().width;
    int height = dec.getDimension().height;
    int frameRate = 0;
    int index = 1;

    IMediaWriter writer = ToolFactory.makeWriter(fileTo);
    //IStreamCoder.
    
    writer.addVideoStream(0, 0, IRational.make(frameRate, 1), width, height);


    BufferedImage image;
    while ((image = dec.nextFrame()) != null) {
      //image = new BufferedImage(recStr.getArea().width, recStr.getArea().height, BufferedImage.TYPE_INT_RGB);
      //image.setRGB(0, 0, recStr.getArea().width, recStr.getArea().height, recStr.getFrameData(), 0, recStr.getArea().width);

      BufferedImage bgrScreen = convertToType(image, BufferedImage.TYPE_3BYTE_BGR);
      
      writer.encodeVideo(0, bgrScreen, dec.getCurrentTimeMillis(), TimeUnit.MILLISECONDS);
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
