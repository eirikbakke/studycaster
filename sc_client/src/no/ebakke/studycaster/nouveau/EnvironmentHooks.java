package no.ebakke.studycaster.nouveau;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jnlp.UnavailableServiceException;
import no.ebakke.studycaster.api.ServerTimeLogFormatter;
import no.ebakke.studycaster.util.stream.ConsoleTee;
import no.ebakke.studycaster.util.stream.NonBlockingOutputStream;

// TODO: Does this actually need to be thread-safe?
/** Thread-safe singleton. */
public final class EnvironmentHooks {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  private static final int    CONSOLE_BUFFER_SZ = 128 * 1024;
  private static EnvironmentHooks instance = new EnvironmentHooks();

  private SingleInstanceHandler   singleInstanceHandler;
  private ConsoleTee              consoleTee;
  private NonBlockingOutputStream consoleStream;
  private boolean                 open = false;

  private EnvironmentHooks() {
  }

  public NonBlockingOutputStream getConsoleStream() {
    return consoleStream;
  }

  private synchronized void init() {
    if (open)
      return;

    // Connect the ConsoleTee as the very first thing to do.
    /* TODO: Use an unlimited NonBlockingOutputStream to avoid the theoretical possibility of the
    console buffer filling up. */
    consoleStream = new NonBlockingOutputStream(CONSOLE_BUFFER_SZ);
    ServerTimeLogFormatter logFormatter = new ServerTimeLogFormatter();
    consoleTee = new ConsoleTee(consoleStream, logFormatter);
    // Entering initial log message to promote fail-fast behavior of potential ConsoleTee bugs.
    LOG.info("Connected console");

    // TODO: Test this feature.
    // Should be done as early as possible.
    try {
      singleInstanceHandler = new SingleInstanceHandler();
    } catch (UnavailableServiceException e) {
      LOG.log(Level.INFO,
          "Couldn''t create a SingleInstanceService (normal when run outside of JWS)", e);
    }

    open = true;
  }

  public static EnvironmentHooks createStudyCaster() {
    instance.init();
    return instance;
  }

  public synchronized void close() {
    if (singleInstanceHandler != null) {
      singleInstanceHandler.close();
      singleInstanceHandler = null;
    }
    if (consoleTee != null) {
      LOG.info("Disconnecting console");
      try {
        consoleTee.close();
      } catch (IOException e) {
        LOG.log(Level.WARNING, "Error while disconnecting console tee", e);
      }
      consoleTee = null;
    }
    open = false;
  }
}
