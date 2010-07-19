package no.ebakke.studycaster;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import no.ebakke.studycaster.util.Blocker;
import no.ebakke.studycaster.util.Pair;
import no.ebakke.studycaster.util.Util;
import org.one.stone.soup.screen.recorder.DesktopScreenRecorder;
import org.one.stone.soup.screen.recorder.ScreenRecorder;
import org.one.stone.soup.screen.recorder.ScreenRecorderListener;

public class StudyCaster {
  public static final Logger log = Logger.getLogger("no.ebakke.studycaster");
  private final List<LogRecord> logEntries = new ArrayList<LogRecord>();
  private DateFormat dateFormat;
  private URL    serverURL;
  private Ticket firstRunTicket;
  private Ticket currentRunTicket;
  private Ticket currentServerTicket;
  private Ticket firstServerTicket;
  private long   serverSecondsAhead;
  private ScreenRecorder recorder;
  private File recordFile;
  private boolean concluded = false;
  private Blocker recorderBlocker = new Blocker();
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

  public Ticket getFirstRunTicket() {
    return firstRunTicket;
  }

  public Ticket getCurrentRunTicket() {
    return currentRunTicket;
  }

  public Ticket getFirstServerTicket() {
    return firstServerTicket;
  }

  public Ticket getCurrentServerTicket() {
    return currentServerTicket;
  }

  private String allTickets() {
    return firstRunTicket + "\t" + currentRunTicket + "\t" +
           ((firstServerTicket   != null) ? firstServerTicket   : "N") + "\t" +
           ((currentServerTicket != null) ? currentServerTicket : "N");
  }

  /* Note: URL must point directly to PHP script, end with a slash to use index.php (otherwise POST requests fail). */
  public StudyCaster(String serverURLstring) throws StudyCasterException {
    dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT:00"));

    log.addHandler(logHandler);
    try {
      try {
        serverURL        = new URL(serverURLstring);
      } catch (MalformedURLException e) {
        throw new StudyCasterException(e);
      }
      currentRunTicket = new Ticket();

      File ticketStore = new File(System.getProperty("java.io.tmpdir") + File.separator + "sc_tickets.tmp");
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

  public String getServerTimeFormat(long millis) {
    return dateFormat.format(new Date(millis + serverSecondsAhead * 1000L));
  }

  public void concludeStudy() {
    synchronized (this) {
      if (concluded)
        return;
      concluded = true;
    }
    stopRecording();
    if (recordFile != null)
      recordFile.delete();
    try {
      Runtime.getRuntime().removeShutdownHook(shutdownHook);
    } catch (IllegalStateException e) {
    } catch (SecurityException e) {
    }
    log.info("Concluding study...");
    log.removeHandler(logHandler);

    StringBuffer logBuf = new StringBuffer();
    for (LogRecord r : logEntries) {
      logBuf.append(getServerTimeFormat(r.getMillis()) + "\t" + r.getLevel() + "\t" + r.getSourceClassName() + "\t" + r.getMessage() + "\n");
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
    File ret = null;
    try {
      int dot = remoteName.lastIndexOf(".");
      String extension = (dot == -1) ? "" : remoteName.substring(dot, remoteName.length());
      ret = File.createTempFile("sc_", extension);
      OutputStream os = null;
      InputStream  is = null;
      try {
        os = new FileOutputStream(ret);
        is = ServerRequest.downloadFile(serverURL, allTickets(), remoteName);
        Util.streamCopy(is, os);
        log.info("Downloaded to " + ret.getAbsolutePath());
      } finally {
        if (os != null)
          os.close();
        if (is != null)
          is.close();
      }
      return ret;
    } catch (IOException e) {
      if (ret != null)
        ret.delete();
      throw new StudyCasterException(e);
    }
  }

  public void uploadFile(File f, String remoteName) throws StudyCasterException {
    InputStream is = null;
    try {
      try {
        is = new FileInputStream(f);
      } catch (FileNotFoundException e) {
        throw new StudyCasterException(e);
      }
      ServerRequest.uploadFile(serverURL, allTickets(), remoteName, is);
      is.close();
      is = null;
    } catch (IOException e) {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e2) { }
      }
      throw new StudyCasterException(e);
    }
  }

  public void desktopOpenFile(File f, String requiredApp) throws StudyCasterException {
    try {
      Desktop.getDesktop().open(f);
    } catch (IOException e) {
      throw new StudyCasterException("Failed to open the file " + f.getName() + "; do you have " + requiredApp + " installed?", e);
    }
  }

  public void startRecording() throws StudyCasterException {
    if (recorder != null)
      throw new IllegalStateException();

    final OutputStream os;
    try {
      recordFile = File.createTempFile("sc_", ".tmp");
      os = new FileOutputStream(recordFile);
    } catch (IOException e) {
      StudyCaster.log.log(Level.SEVERE, "Can't open temporary file.", e);
      throw new StudyCasterException(e);
    }

    new Thread(new Runnable() {
      int frames = 0;

      public void run() {
        recorder = new DesktopScreenRecorder(os, new ScreenRecorderListener() {
          boolean loggedYet = false;

          public void frameRecorded(boolean fullFrame) throws IOException {
            if (!loggedYet)
              log.info("Recorded the first frame.");
            loggedYet = true;

            //System.err.println(recordFile.length());
            if (recordFile.length() > 5000000) {
              log.warning("Recording size passed limit.");
              stopRecording();
            }
          }

          public void recordingStopped() {
            log.info("Recording stopped.");
            recorderBlocker.releaseBlockingThread();
          }
        });
        recorder.startRecording();
      }
    }).start();
  }

  public void stopRecording() {
    if (recorder == null)
      return;
    new Thread(new Runnable() {
      public void run() {
        recorder.stopRecording();
      }
    }).start();
    recorderBlocker.blockUntilReleased(10000);
    recorder = null;
  }

  public File getRecordFile() {
    return recordFile;
  }
  /*
   // TODO: Redirect stdout/stderr to an actual log file, e.g. as follows:

    final PrintStream olderr = System.err;
    System.setErr(new PrintStream(new OutputStream() {
      @Override
      public void write(int b) throws IOException {
        byte ret[] = new byte[1];
        ret[0] = (byte) b;
        write(ret, 0, 1);
      }

      @Override
      public void write(byte[] b, int off, int len) throws IOException {
        olderr.write(b, off, len);
      }

      @Override
      public void flush() throws IOException {
        olderr.flush();
      }
    }));
  */

}
