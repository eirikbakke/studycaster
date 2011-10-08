package no.ebakke.studycaster.api;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/** Thread-safe. For consistency in logging statements throughout the codebase, this class always
uses MessageFormat for formatting, regardless of whether the log record includes a parameter or any
parameter references in the message string. */
public class ServerTimeLogFormatter extends Formatter {
  private volatile Long serverSecondsAhead;
  /* SimpleDateFormat is not thread-safe. The following is the standard way of dealing with it.
  See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4264153 . */
  private static final ThreadLocal<DateFormat> dateFormat = 
    new ThreadLocal<DateFormat>() {
      @Override
      public DateFormat initialValue() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      }
    };

  public void setServerSecondsAhead(long serverSecondsAhead) {
    this.serverSecondsAhead = serverSecondsAhead;
  }

  private static String formatTime(long millis) {
    return dateFormat.get().format(new Date(millis));
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
    return ret.toString();
  }
}
