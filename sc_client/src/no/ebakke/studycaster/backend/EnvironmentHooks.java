package no.ebakke.studycaster.backend;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jnlp.UnavailableServiceException;
import no.ebakke.studycaster.util.stream.ConsoleTee;
import no.ebakke.studycaster.util.stream.NonBlockingOutputStream;

/** Thread-safe singleton. */
public final class EnvironmentHooks {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  private static EnvironmentHooks instance;

  private final SingleInstanceHandler   singleInstanceHandler;
  private final ConsoleTee              consoleTee;
  private final NonBlockingOutputStream consoleStream;
  private final ServerTimeLogFormatter  logFormatter;

  public synchronized NonBlockingOutputStream getConsoleStream() {
    return consoleStream;
  }

  public synchronized ServerTimeLogFormatter getLogFormatter() {
    return logFormatter;
  }

  public synchronized SingleInstanceHandler getSingleInstanceHandler() {
    return singleInstanceHandler;
  }

  private EnvironmentHooks() {
    // Connect the ConsoleTee as the very first thing to do.
    consoleStream = new NonBlockingOutputStream();
    logFormatter  = new ServerTimeLogFormatter();
    consoleTee    = new ConsoleTee(consoleStream, logFormatter);
    // Enter initial log message to promote fail-fast behavior of potential ConsoleTee bugs.
    LOG.info("Connected console");

    // Should be done as early as possible.
    SingleInstanceHandler singleInstanceHandlerTry = null;
    try {
      singleInstanceHandlerTry = new SingleInstanceHandler();
    } catch (UnavailableServiceException e) {
      // Happens normally all the time during development, so don't include a full stack trace.
      LOG.log(Level.INFO,
          "Couldn''t create a SingleInstanceService (normal when run outside of JWS); {0}",
          e.getMessage());
    }
    singleInstanceHandler = singleInstanceHandlerTry;
  }

  public static synchronized EnvironmentHooks create() {
    if (instance == null)
      instance = new EnvironmentHooks();
    return instance;
  }

  public static synchronized void shutdown() {
    instance.close();
    instance = null;
  }

  private synchronized void close() {
    if (singleInstanceHandler != null)
      singleInstanceHandler.close();
    LOG.info("Disconnecting console");
    try {
      consoleTee.close();
    } catch (IOException e) {
      LOG.log(Level.WARNING, "Error while disconnecting console tee", e);
    }
  }
}
