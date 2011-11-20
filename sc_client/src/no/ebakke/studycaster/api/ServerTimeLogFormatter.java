package no.ebakke.studycaster.api;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

// TODO: Reverse the MessageFormat design decision below.
/** Thread-safe. For consistency in logging statements throughout the codebase, this class always
uses MessageFormat for formatting, regardless of whether the log record includes a parameter or any
parameter references in the message string. */
public class ServerTimeLogFormatter extends Formatter {
  private volatile Long serverSecondsAhead;
  /* SimpleDateFormat is not thread-safe. The following is the standard way of dealing with it.
  See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4264153 . */
  private static final ThreadLocal<DateFormat> SERVER_DATE_FORMAT =
    new ThreadLocal<DateFormat>() {
      @Override
      public DateFormat initialValue() {
        // If changing this, also change the corresponding server implementation in ServletUtil.
        DateFormat ret = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'");
        ret.setTimeZone(TimeZone.getTimeZone("GMT"));
        return ret;
      }
    };
  private static final ThreadLocal<DateFormat> CLIENT_DATE_FORMAT =
    new ThreadLocal<DateFormat>() {
      @Override
      public DateFormat initialValue() {
        DateFormat ret = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
        return ret;
      }
    };

  /** The returned DateFormat must only be used in the current thread. */
  public static DateFormat getServerDateFormat() {
    return SERVER_DATE_FORMAT.get();
  }

  public void setServerSecondsAhead(long serverSecondsAhead) {
    this.serverSecondsAhead = serverSecondsAhead;
  }

  private static void formatThrowable(StringBuffer buf, Throwable e) {
    buf.append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n");
    for (StackTraceElement ste : e.getStackTrace())
      buf.append("    at ").append(ste.toString()).append("\n");
    if (e.getCause() != null) {
      buf.append("  caused by ");
      formatThrowable(buf, e.getCause());
    }
  }

  @Override
  public String format(LogRecord r) {
    StringBuffer ret = new StringBuffer();
    ret.append(CLIENT_DATE_FORMAT.get().format(new Date(r.getMillis())));
    ret.append(" (client)");
    if (serverSecondsAhead != null) {
      ret.append(" / ");
      ret.append(SERVER_DATE_FORMAT.get().format(
          new Date(r.getMillis() + serverSecondsAhead * 1000L)));
      ret.append(" (server)");
    }
    ret.append(" ");
    ret.append(r.getSourceClassName()).append(" ");
    ret.append(r.getSourceMethodName()).append("\n");
    ret.append(r.getLevel());
    ret.append(": ");
    ret.append(new MessageFormat(r.getMessage()).format(r.getParameters())).append("\n");
    if (r.getThrown() != null) {
      ret.append("  exception was ");
      formatThrowable(ret, r.getThrown());
    }
    return ret.toString();
  }
}
