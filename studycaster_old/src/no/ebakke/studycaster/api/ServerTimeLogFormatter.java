package no.ebakke.studycaster.api;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class ServerTimeLogFormatter extends Formatter {
  private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  private Long serverSecondsAhead;

  public void setServerSecondsAhead(long serverSecondsAhead) {
    this.serverSecondsAhead = serverSecondsAhead;
  }

  private static String formatTime(long millis) {
    return dateFormat.format(new Date(millis));
  }

  private static void formatThrowable(StringBuffer buf, Throwable e) {
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
    ret.append(formatTime(r.getMillis()) + " (client)");
    if (serverSecondsAhead != null)
      ret.append(" / " + formatTime(r.getMillis() + serverSecondsAhead * 1000L) + " (server)");
    ret.append(" " + r.getSourceClassName() + " " + r.getSourceMethodName() + "\n");
    ret.append(r.getLevel());
    ret.append(": ");
    ret.append(new MessageFormat(r.getMessage()).format(r.getParameters()));
    ret.append("\n");
    if (r.getThrown() != null) {
      ret.append("  exception was ");
      formatThrowable(ret, r.getThrown());
    }
    SimpleFormatter foo;
    return ret.toString();
  }
}
