package no.ebakke.studycaster;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
  public StudyCaster(String serverURLstr) throws Exception {
    // TODO: Remove after uploading.
    log.addHandler(logHandler);
    try {
      serverURL        = new URL(serverURLstr);
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
      Pair<Ticket, Long> si = ServerRequest.getServerInfo(serverURL, allTickets());
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
    } catch (Exception e) {
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
    /*
    for (LogRecord r : logEntries) {
      System.out.println("YEAH : " + r.getMessage());
    }
    */
  }

  @Override
  protected void finalize() throws Throwable {
    concludeStudy();
  }
}
