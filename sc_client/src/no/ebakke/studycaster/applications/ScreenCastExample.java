package no.ebakke.studycaster.applications;

import java.awt.AWTException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.ebakke.studycaster.backend.ServerContext;
import no.ebakke.studycaster.backend.StudyCasterException;
import no.ebakke.studycaster.screencasting.ScreenRecorder;
import no.ebakke.studycaster.screencasting.ScreenRecorderConfiguration;
import no.ebakke.studycaster.util.stream.NonBlockingOutputStream;

/** Example command-line application that establishes a connection to the StudyCaster server,
records a 30 second screencast, and then exits gracefully. */
public final class ScreenCastExample {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  /** The maximum number of bytes of screencast data that can be stored in memory waiting to be
  written to the output stream. Once this limit is reached, the framerate will drop according to the
  maximum upload speed (if uploading to the server) or file I/O speed. */
  private static final int RECORDING_BUFFER_SZ = 4 * 1024 * 1024;

  private ScreenCastExample() { }

  public static void main(String args[])
      throws IOException, StudyCasterException, AWTException, InterruptedException
  {
    // Alternatively, can set studycaster.server.uri and call empty constructor.
    ServerContext serverContext = new ServerContext("http://localhost:8084/sc_server/client");
    NonBlockingOutputStream recordingStream = new NonBlockingOutputStream(RECORDING_BUFFER_SZ);
    /* Alternatively, a new ScreenRecorderConfiguration object can be created to set frame/pointer
    sampling rates and maximum CPU usage. */
    ScreenRecorder recorder = new ScreenRecorder(recordingStream,
        serverContext.getServerTimeSource(), ScreenRecorderConfiguration.DEFAULT);

    /* If you just want to record to a local file instead, don't bother with the ServerContext
    stuff, instead provide a FileOutputStream here. It's still probably a good idea to use the
    NonBlockingOutputStream, as this way the frame rate will be unaffected by the occational slow
    file I/O operation. */
    recordingStream.connect(serverContext.uploadFile("screencast.ebc"));

    recorder.start();
    LOG.log(Level.INFO, "Now recording for 30 seconds...");
    Thread.sleep(30000);

    LOG.log(Level.INFO, "Finished recording. The confirmation code (launchTicket) is {0}",
        serverContext.getLaunchTicket());
    /* If the recorder isn't stopped, it will keep on running in its own thread even if main()
    returns. So don't let that happen. */
    recorder.stop();
    recorder.close();
    serverContext.close();
  }
}
