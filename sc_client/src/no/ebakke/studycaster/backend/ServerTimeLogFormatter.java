package no.ebakke.studycaster.backend;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/** Thread-safe. */
public class ServerTimeLogFormatter extends Formatter {
  private volatile TimeSource serverTimeSource;
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
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
      }
    };

  /** The returned DateFormat must only be used in the current thread. */
  public static DateFormat getServerDateFormat() {
    return SERVER_DATE_FORMAT.get();
  }

  public void setServerTimeSource(TimeSource serverTimeSource) {
    this.serverTimeSource = serverTimeSource;
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
    if (serverTimeSource != null) {
      ret.append(" / ");
      /* This assumes the log record is formatted immediately after being posted, which is not
      necessarily the case. However, the real log record time is shown as well above. */
      ret.append(SERVER_DATE_FORMAT.get().format(new Date(serverTimeSource.currentTimeMillis())));
      ret.append(" (server)");
    }
    ret.append(" ");
    ret.append(r.getSourceClassName()).append(" ");
    ret.append(r.getSourceMethodName()).append("\n");
    ret.append(r.getLevel());
    ret.append(": ");
    ret.append(formatMessage(r));
    ret.append("\n");
    if (r.getThrown() != null) {
      ret.append("  exception was ");
      formatThrowable(ret, r.getThrown());
    }
    return ret.toString();
  }
}
