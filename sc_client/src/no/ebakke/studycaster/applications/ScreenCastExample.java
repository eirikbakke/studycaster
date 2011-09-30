package no.ebakke.studycaster.applications;

import java.awt.AWTException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.ebakke.studycaster.api.ServerContext;
import no.ebakke.studycaster.api.StudyCasterException;
import no.ebakke.studycaster.screencasting.ScreenRecorder;
import no.ebakke.studycaster.util.stream.NonBlockingOutputStream;

public final class ScreenCastExample {
  private static final int RECORDING_BUFFER_SZ = 4 * 1024 * 1024;
  private static final Logger log = Logger.getLogger("no.ebakke.studycaster");

  private ScreenCastExample() { }

  public static void main(String args[])
      throws IOException, StudyCasterException, AWTException, InterruptedException
  {
    System.setProperty("studycaster.server.uri", "http://localhost:8084/sc_server/client");

    NonBlockingOutputStream recordingStream = new NonBlockingOutputStream(RECORDING_BUFFER_SZ);
    ServerContext serverContext = new ServerContext();
    ScreenRecorder recorder = new ScreenRecorder(recordingStream, serverContext.getServerSecondsAhead());

    recordingStream.connect(serverContext.uploadFile("screencast.ebc"));

    recorder.start();
    log.log(Level.INFO, "Now recording for 30 seconds...");
    Thread.sleep(30000);

    log.log(Level.INFO, "Finished recording. The confirmation code (launchTicket) is {0}",
        serverContext.getLaunchTicket());
    recorder.stop();
    recorder.close();
    serverContext.close();

  }
}
