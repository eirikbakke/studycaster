package no.ebakke.studycaster2.screencasting;

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

public final class RecordingConverter {
  private RecordingConverter() { }
  
  public static void main(String args[]) throws Exception {
    //ServerContext sc = new ServerContext(new URI("http://www.sieuferd.com/studycaster/server.php"));
    //newConvert(sc.downloadFile("uploads/1875c96d2d6c/screencast.ebc"), "z:\\rectest\\downconv.ogv");
    newConvert(new FileInputStream("z:\\rectest\\screencast.ebc"), "z:\\rectest\\fileconv.ogv");
  }

  public static void newConvert(InputStream inStream, String fileTo) throws Exception {
    CodecDecoder dec = new CodecDecoder(inStream);

    IContainer outContainer = IContainer.make();
    if (outContainer.open(fileTo, IContainer.Type.WRITE, null) < 0)
      throw new IOException("Could not open output file");
    IStream outStream = outContainer.addNewStream(0);
    IStreamCoder outStreamCoder = outStream.getStreamCoder();
    // com.xuggle.xuggler.ICodec@53610016[type=CODEC_TYPE_VIDEO;id=CODEC_ID_THEORA;name=theora
    // com.xuggle.xuggler.ICodec@53617504[type=CODEC_TYPE_VIDEO;id=CODEC_ID_THEORA;name=libtheora
    //ICodec codec = ICodec.findDecodingCodec(ICodec.ID.CODEC_ID_THEORA);
    ICodec codec = ICodec.guessEncodingCodec(null, null, fileTo, null, ICodec.Type.CODEC_TYPE_VIDEO);
    System.out.println(codec);
    IRational frameRate = IRational.make(15, 1);
    IPixelFormat.Type pixelFormat = IPixelFormat.Type.YUV420P; /* YUV420P, YUV422P, YUV444P */
    outStreamCoder.setCodec(codec);
    outStreamCoder.setNumPicturesInGroupOfPictures(100);
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
      if (index++ % 3 != 0)
        continue;

      BufferedImage converted = convertToType(image, BufferedImage.TYPE_3BYTE_BGR);
      IPacket packet = IPacket.make();
      IConverter converter = ConverterFactory.createConverter(converted, pixelFormat);
      IVideoPicture outFrame = converter.toPicture(converted, dec.getCurrentTimeMillis() * 1000L);
      outFrame.setQuality(0);
      outStreamCoder.encodeVideo(packet, outFrame, 0);
      if (packet.isComplete())
        outContainer.writePacket(packet);

      System.out.println("encoded frame: " + index);
    }
    outContainer.writeTrailer();
    outStreamCoder.close();
    outContainer.close();
    inStream.close();
  }

  private static BufferedImage convertToType(BufferedImage sourceImage, int targetType) {
    // From xuggler documentation.
    if (sourceImage.getType() == targetType) {
      return sourceImage;
    } else {
      BufferedImage ret = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), targetType);
      ret.getGraphics().drawImage(sourceImage, 0, 0, null);
      return ret;
    }
  }
}
