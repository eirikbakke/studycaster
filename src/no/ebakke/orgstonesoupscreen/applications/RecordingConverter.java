package no.ebakke.orgstonesoupscreen.applications;

import no.ebakke.orgstonesoupscreen.RecordingStream;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.Buffer;
import javax.media.ConfigureCompleteEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.DataSink;
import javax.media.EndOfMediaEvent;
import javax.media.Format;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.PrefetchCompleteEvent;
import javax.media.Processor;
import javax.media.RealizeCompleteEvent;
import javax.media.ResourceUnavailableEvent;
import javax.media.Time;
import javax.media.control.TrackControl;
import javax.media.datasink.DataSinkErrorEvent;
import javax.media.datasink.DataSinkEvent;
import javax.media.datasink.DataSinkListener;
import javax.media.datasink.EndOfStreamEvent;
import javax.media.format.VideoFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;
import javax.media.protocol.PullBufferDataSource;
import javax.media.protocol.PullBufferStream;

public class RecordingConverter implements ControllerListener, DataSinkListener {
  public static void main(String[] args) {
    try {
      RecordingConverter recordingConverter = new RecordingConverter();
      // FileTypeDescriptor.QUICKTIME  // MOV
      // FileTypeDescriptor.MSVIDEO    // AVI
      recordingConverter.process("z:\\rectest\\testfile.rec", "z:\\rectest\\testfile.mov",
              FileTypeDescriptor.QUICKTIME);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      System.exit(0);
    }
  }
  /**
   *
   * A DataSource to read from a list of JPEG image files and
   *
   * turn that into a stream of JMF buffers.
   *
   * The DataSource is not seekable or positionable.
   *
   */
  PlayerSourceStream playerSourceStream;

  class PlayerDataSource extends PullBufferDataSource {
    MediaLocator mediaLocator;
    PlayerSourceStream streams[];

    PlayerDataSource(String screenRecordingFileName, MediaLocator mediaLocator)
            throws IOException {
      this.mediaLocator = mediaLocator;
      streams = new PlayerSourceStream[1];
      playerSourceStream = new PlayerSourceStream(screenRecordingFileName);
      streams[0] = playerSourceStream;
    }

    @Override
    public void setLocator(MediaLocator source) {
      mediaLocator = source;
    }

    @Override
    public MediaLocator getLocator() {
      return mediaLocator;
    }

    /**
     *
     * Content type is of RAW since we are sending buffers of video
     *
     * frames without a container format.
     *
     */
    public String getContentType() {
      return ContentDescriptor.RAW;
    }

    public void connect() {
    }

    public void disconnect() {
    }

    public void start() {
    }

    public void stop() {
    }

    /**
     *
     * Return the PlayerSourceStreams.
     *
     */
    public PullBufferStream[] getStreams() {
      return streams;
    }

    /**
     *
     * We could have derived the duration from the number of
     *
     * frames and frame rate. But for the purpose of this program,
     *
     * it's not necessary.
     *
     */
    public Time getDuration() {
      return DURATION_UNKNOWN;
    }

    public Object[] getControls() {
      return new Object[0];
    }

    public Object getControl(String type) {
      return null;
    }
  }

  /**
   *
   * The source stream to go along with ImageDataSource.
   *
   */
  class PlayerSourceStream implements PullBufferStream {

    FileInputStream inStream;
    RecordingStream recStr;
    int width, height, frameRate;
    VideoFormat format;
    BufferedImage image;
    int nextImage = 0; // index of the next image to be read.
    boolean ended = false;

    public PlayerSourceStream(String screenRecordingFileName) throws IOException {
      inStream = new FileInputStream(screenRecordingFileName);
      recStr = new RecordingStream(inStream);
      width = (int) recStr.getArea().getWidth();
      height = (int) recStr.getArea().getHeight();
      frameRate = 4;
      format = new VideoFormat(VideoFormat.JPEG,
              new Dimension(width, height),
              Format.NOT_SPECIFIED,
              Format.byteArray,
              (float) frameRate);
    }

    /**
     *
     * We should never need to block assuming data are read from files.
     *
     */
    public boolean willReadBlock() {
      return false;
    }

    /**
     *
     * This is called from the Processor to read a frame worth
     *
     * of video data.
     *
     */
    public void read(Buffer buffer) throws IOException {
      // Check if we've finished all the frames.
      if (recStr.isFinished()) {
        // We are done. Set EndOfMedia.
        System.err.println("Done reading all images.");
        buffer.setEOM(true);
        buffer.setOffset(0);
        buffer.setLength(0);
        ended = true;
        return;
      }
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      if (!recStr.readFrameData()) {
        image = new BufferedImage(recStr.getArea().width, recStr.getArea().height, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, recStr.getArea().width, recStr.getArea().height, recStr.getFrameData(), 0, recStr.getArea().width);
      }
      ImageIO.write(image, "JPEG", outputStream);
      byte[] data = outputStream.toByteArray();
      nextImage++;
      System.out.println("Recording frame " + nextImage);
      buffer.setData(data);
      buffer.setOffset(0);
      buffer.setLength(data.length);
      buffer.setFormat(format);
      buffer.setFlags(buffer.getFlags() | Buffer.FLAG_KEY_FRAME);
    }

    /**
     *
     * Return the format of each video frame. That will be JPEG.
     *
     */
    public Format getFormat() {
      return format;
    }

    public ContentDescriptor getContentDescriptor() {
      return new ContentDescriptor(ContentDescriptor.RAW);
    }

    public long getContentLength() {
      return 0;
    }

    public boolean endOfStream() {
      return ended;
    }

    public Object[] getControls() {
      return new Object[0];
    }

    public Object getControl(String type) {
      return null;
    }
  }

  public RecordingConverter() throws Exception {
  }

  public void process(String recordingFile, String movieFile, String type) throws Exception {
    MediaLocator mediaLocator = new MediaLocator(new File(movieFile).toURI().toURL());
    PlayerDataSource playerDataSource = new PlayerDataSource(recordingFile, mediaLocator);
    Processor processor = Manager.createProcessor(playerDataSource);
    processor.addControllerListener(this);
    processor.configure();
    if (!waitForState(processor, Processor.Configured)) {
      System.err.println("Failed to configure the processor.");
      return;
    }
    processor.setContentDescriptor(new ContentDescriptor(type));
    TrackControl trackControl[] = processor.getTrackControls();
    Format format[] = trackControl[0].getSupportedFormats();
    trackControl[0].setFormat(format[0]);
    processor.realize();
    if (!waitForState(processor, Processor.Realized)) {
      System.err.println("Failed to realize the processor.");
      return;
    }
    DataSource dataSource = processor.getDataOutput();
    DataSink dataSink = Manager.createDataSink(dataSource, mediaLocator);
    dataSink.addDataSinkListener(this);
    dataSink.open();
    processor.start();
    dataSink.start();
    // Wait for EndOfStream event.
    waitForFileDone();
    // Cleanup.
    dataSink.close();
    processor.removeControllerListener(this);
  }
  final Object waitFileSync = new Object();
  boolean fileDone = false;
  boolean fileSuccess = true;

  /**
   *
   * Block until file writing is done.
   *
   */
  boolean waitForFileDone() {
    synchronized (waitFileSync) {
      try {
        while (!fileDone) {
          waitFileSync.wait();
        }
      } catch (Exception e) {
      }
    }
    return fileSuccess;
  }

  /**
   *
   * Event handler for the file writer.
   *
   */
  public void dataSinkUpdate(DataSinkEvent evt) {
    if (evt instanceof EndOfStreamEvent) {
      synchronized (waitFileSync) {
        fileDone = true;
        waitFileSync.notifyAll();
      }
    } else if (evt instanceof DataSinkErrorEvent) {
      synchronized (waitFileSync) {
        fileDone = true;
        fileSuccess = false;
        waitFileSync.notifyAll();
      }
    }
  }
  final Object waitSync = new Object();
  boolean stateTransitionOK = true;

  /**
   *
   * Block until the processor has transitioned to the given state.
   *
   * Return false if the transition failed.
   *
   */
  boolean waitForState(Processor p, int state) {
    synchronized (waitSync) {
      try {
        while (p.getState() < state && stateTransitionOK) {
          waitSync.wait();
        }
      } catch (Exception e) {
      }
    }
    return stateTransitionOK;
  }

  /**
   *
   * Controller Listener.
   *
   */
  public void controllerUpdate(ControllerEvent evt) {
    if (evt instanceof ConfigureCompleteEvent
            || evt instanceof RealizeCompleteEvent
            || evt instanceof PrefetchCompleteEvent) {
      synchronized (waitSync) {
        stateTransitionOK = true;
        waitSync.notifyAll();
      }
    } else if (evt instanceof ResourceUnavailableEvent) {
      synchronized (waitSync) {
        stateTransitionOK = false;
        waitSync.notifyAll();
      }
    } else if (evt instanceof EndOfMediaEvent) {
      evt.getSourceController().stop();
      evt.getSourceController().close();
    }
  }
}
