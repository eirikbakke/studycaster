package no.ebakke.studycaster.nouveau;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jnlp.UnavailableServiceException;
import no.ebakke.studycaster.api.ServerTimeLogFormatter;
import no.ebakke.studycaster.util.stream.ConsoleTee;
import no.ebakke.studycaster.util.stream.NonBlockingOutputStream;

/** Thread-safe singleton. */
public final class EnvironmentHooks {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  private static EnvironmentHooks instance = new EnvironmentHooks();

  private SingleInstanceHandler   singleInstanceHandler;
  private ConsoleTee              consoleTee;
  private NonBlockingOutputStream consoleStream;
  private ServerTimeLogFormatter  logFormatter;
  private boolean                 open = false;

  private EnvironmentHooks() {
  }

  public synchronized NonBlockingOutputStream getConsoleStream() {
    return consoleStream;
  }

  public synchronized ServerTimeLogFormatter getLogFormatter() {
    return logFormatter;
  }

  public synchronized SingleInstanceHandler getSingleInstanceHandler() {
    return singleInstanceHandler;
  }

  private synchronized void init() {
    if (open)
      return;

    // Connect the ConsoleTee as the very first thing to do.
    consoleStream = new NonBlockingOutputStream();
    logFormatter  = new ServerTimeLogFormatter();
    consoleTee    = new ConsoleTee(consoleStream, logFormatter);
    // Enter initial log message to promote fail-fast behavior of potential ConsoleTee bugs.
    LOG.info("Connected console");

    // TODO: Test this feature.
    // Should be done as early as possible.
    try {
      singleInstanceHandler = new SingleInstanceHandler();
    } catch (UnavailableServiceException e) {
      // Happens normally all the time during development, so don't include a full stack trace.
      LOG.log(Level.INFO,
          "Couldn''t create a SingleInstanceService (normal when run outside of JWS); {0}",
          e.getMessage());
    }

    open = true;
  }

  public static EnvironmentHooks create() {
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
