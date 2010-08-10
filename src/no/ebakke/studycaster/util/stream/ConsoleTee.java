package no.ebakke.studycaster.util.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Logger;

public class ConsoleTee {
  private OutputStream out;
  private PrintStream oldStdOut, oldStdErr;
  private boolean disconnected = false;
  private TeePrintStream teeStdOut, teeStdErr;
  private ConsoleHandler oldLogHandler, newLogHandler;

  public ConsoleTee(OutputStream out, Formatter logFormatter) {
    this.out = out;
    oldStdOut = System.out;
    oldStdErr = System.err;
    Logger globalLogger = Logger.getLogger("");
    for (Handler h : globalLogger.getHandlers()) {
      if (h instanceof ConsoleHandler) {
        globalLogger.removeHandler(h);
        oldLogHandler = (ConsoleHandler) h;
        break;
      }
    }
    teeStdOut = new TeePrintStream(out, System.out);
    teeStdErr = new TeePrintStream(out, System.err);
    System.setOut(teeStdOut);
    System.setErr(teeStdErr);
    newLogHandler = new ConsoleHandler();
    newLogHandler.setFormatter(logFormatter);
    globalLogger.addHandler(newLogHandler);
  }

  public void close() throws IOException {
    if (disconnected)
      return;
    disconnected = true;
    Logger globalLogger = Logger.getLogger("");
    globalLogger.removeHandler(newLogHandler);
    System.setOut(oldStdOut);
    System.setErr(oldStdErr);
    globalLogger.addHandler(oldLogHandler);
    out.close();
  }
}
