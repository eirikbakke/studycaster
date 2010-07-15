package no.ebakke.studycaster;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import no.ebakke.studycaster.util.Pair;

public class StudyCaster {
  public static final Logger log = Logger.getLogger("no.ebakke.studycaster");
  private final List<LogRecord> logEntries = new ArrayList<LogRecord>();
  private URL    serverURL;
  private Ticket firstRunTicket;
  private Ticket currentRunTicket;
  private Ticket currentServerTicket;
  private Ticket firstServerTicket;
  private long   serverSecondsAhead;
  private boolean concluded = false;
  private Handler logHandler = new Handler() {
      @Override
      public void publish(LogRecord record) {
        synchronized (logEntries) {
          logEntries.add(record);
        }
      }
      @Override public void flush() { }
      @Override public void close() { }
    };
  private Thread shutdownHook = new Thread(new Runnable() {
    public void run() {
      log.warning("Study not explicitly concluded; concluding via shutdown hook.");
      concludeStudy();
      }
    });

  private String allTickets() {
    return firstRunTicket + "\t" + currentRunTicket + "\t" +
           ((firstServerTicket   != null) ? firstServerTicket   : "N") + "\t" +
           ((currentServerTicket != null) ? currentServerTicket : "N");
  }

  /* Note: URL must point directly to PHP script, end with a slash to use index.php (otherwise POST requests fail). */
  public StudyCaster(String serverURLstring) throws StudyCasterException {
    // TODO: Remove after uploading.
    log.addHandler(logHandler);
    try {
      try {
        serverURL        = new URL(serverURLstring);
      } catch (MalformedURLException e) {
        throw new StudyCasterException(e);
      }
      currentRunTicket = new Ticket();

      File ticketStore = new File(System.getProperty("java.io.tmpdir") + File.separator + "~studycaster.txt");
      boolean writeTicketStore = false;
      try {
        if (ticketStore.exists()) {
          BufferedReader br = new BufferedReader(new FileReader(ticketStore));
          try {
            firstRunTicket    = new Ticket(br.readLine());
            firstServerTicket = new Ticket(br.readLine());
            log.info("Read from ticket store.");
          } finally {
            br.close();
          }
        } else {
          writeTicketStore = true;
        }
      } catch (Exception e) {
        writeTicketStore = true;
        log.log(Level.WARNING, "Problem reading ticket file.", e);
      }
      if (firstRunTicket == null)
        firstRunTicket = currentRunTicket;

      long timeBef = System.currentTimeMillis();
      Pair<Ticket, Long> si;
      try {
        si = ServerRequest.getServerInfo(serverURL, allTickets());
      } catch (IOException e) {
        throw new StudyCasterException(e);
      }
      long timeAft = System.currentTimeMillis();
      serverSecondsAhead = si.getLast() - ((timeBef / 2 + timeAft / 2) / 1000L);
      log.info("Server time ahead by " + serverSecondsAhead + " seconds.");
      currentServerTicket = si.getFirst();
      if (firstServerTicket == null)
        firstServerTicket = currentServerTicket;

      if (writeTicketStore) {
        try {
          FileWriter fw = new FileWriter(ticketStore);
          try {
            fw.write(firstRunTicket.toString() + "\n");
            fw.write(firstServerTicket.toString() + "\n");
            log.info("Wrote to ticket store.");
          } finally {
            fw.close();
          }
        } catch (Exception e) {
          log.log(Level.WARNING, "Problem writing ticket file.", e);
        }
      }

      log.info("first_run = " + firstRunTicket + ", current_run = " + currentRunTicket + ", first_server = " + firstServerTicket + ", cur_server = " + currentServerTicket);
    } catch (StudyCasterException e) {
      log.log(Level.SEVERE, "Error initializing StudyCaster.", e);
      log.removeHandler(logHandler);
      throw e;
    }
    Runtime.getRuntime().addShutdownHook(shutdownHook);
  }

  public void concludeStudy() {
    synchronized (this) {
      if (concluded)
        return;
      concluded = true;
    }
    try {
      Runtime.getRuntime().removeShutdownHook(shutdownHook);
    } catch (IllegalStateException e) {
    } catch (SecurityException e) {
    }
    log.info("Concluding study...");
    log.removeHandler(logHandler);

    StringBuffer logBuf = new StringBuffer();
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    df.setTimeZone(TimeZone.getTimeZone("GMT:00"));
    for (LogRecord r : logEntries) {
      String time = df.format(new Date(r.getMillis() + serverSecondsAhead * 1000L));
      logBuf.append(time + "\t" + r.getLevel() + "\t" + r.getSourceClassName() + "\t" + r.getMessage() + "\n");
      if (r.getThrown() != null) {
        logBuf.append("  " + r.getThrown().getClass().getName() + ": " + r.getThrown().getMessage() + "\n");
        for (StackTraceElement ste : r.getThrown().getStackTrace())
          logBuf.append("    at " + ste.toString() + "\n");
        if (r.getThrown().getCause() != null)
          logBuf.append("  cause was a " + r.getThrown().getCause().toString() + "\n");
      }
    }

    // TODO: See if there is a better way...
    InputStream is = new ByteArrayInputStream(logBuf.toString().getBytes());
    try {
      ServerRequest.uploadFile(serverURL, allTickets(), "clientlog_" + currentRunTicket + ".txt", is);
    } catch (IOException e) {
      log.log(Level.SEVERE, "Failed to upload log.", e);
    }
  }

  @Override
  protected void finalize() {
    concludeStudy();
  }

  public File downloadFile(String remoteName) throws StudyCasterException {
    try {
      File ret = ServerRequest.downloadFile(serverURL, allTickets(), remoteName);
      log.info("Downloaded to " + ret.getAbsolutePath());
      return ret;
    } catch (IOException e) {
      throw new StudyCasterException(e);
    }
  }
}
