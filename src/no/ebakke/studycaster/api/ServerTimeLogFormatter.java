package no.ebakke.studycaster.api;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class ServerTimeLogFormatter extends Formatter {
  private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  private Long serverSecondsAhead;

  public void setServerSecondsAhead(long serverSecondsAhead) {
    this.serverSecondsAhead = serverSecondsAhead;
  }

  private void formatThrowable(StringBuffer buf, Throwable e) {
    buf.append(e.getClass().getName() + ": " + e.getMessage() + "\n");
    for (StackTraceElement ste : e.getStackTrace())
      buf.append("    at " + ste.toString() + "\n");
    if (e.getCause() != null) {
      buf.append("  caused by ");
      formatThrowable(buf, e.getCause());
    }
  }

  @Override
  public String format(LogRecord r) {
    StringBuffer ret = new StringBuffer();
    ret.append(dateFormat.format(new Date(r.getMillis())) + " (client)");
    if (serverSecondsAhead != null)
      ret.append(" / " + dateFormat.format(new Date(r.getMillis() + serverSecondsAhead * 1000L)) + " (server)");
    ret.append(" " + r.getSourceClassName() + " " + r.getSourceMethodName() + "\n");
    ret.append(r.getLevel() + ": " + r.getMessage() + "\n");
    if (r.getThrown() != null) {
      ret.append("  exception was ");
      formatThrowable(ret, r.getThrown());
    }
    return ret.toString();
  }

  public void install() {
    for (Handler h : Logger.getLogger("").getHandlers()) {
      if (h instanceof ConsoleHandler)
        h.setFormatter(this);
    }
  }
}
