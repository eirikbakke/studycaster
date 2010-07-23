package no.ebakke.studycaster2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import no.ebakke.studycaster.StudyCasterException;

/** Handles protocol details specific to our server-side PHP script. */
public class ServerContext implements ServerScript {
  private static final String HEADER_STM = "X-StudyCaster-ServerTime";
  private static final String HEADER_STK = "X-StudyCaster-ServerTicket";
  private static final String HEADER_UPK = "X-StudyCaster-UploadOK";
  private static final String HEADER_DNK = "X-StudyCaster-DownloadOK";
  private static final String TICKET_STORE_FILENAME = "sc_7403204709139484951.tmp";
  private static final int CLIENT_TICKET_BYTES = 6;
  private static final int SERVER_TICKET_BYTES = 3;
  private URL    serverScriptURL;
  private Ticket ticketFC; // First client ticket on this machine
  private Ticket ticketCC; // Current client ticket
  private Ticket ticketFS; // First server ticket on this machine
  private Ticket ticketCS; // Current server ticket
  private long   serverSecondsAhead;

  private Map<String,String> serverParams(String cmd) {
    String allTickets =
            ticketFC + "," +
            ticketCC + "," +
           ((ticketFS != null) ? ticketFS : "") + "," +
           ((ticketCS != null) ? ticketCS : "");
    Map<String,String> ret = new LinkedHashMap<String,String>();
    ret.put("ct", allTickets);
    ret.put("cmd", cmd);
    return ret;
  }

  public ServerContext(URL serverScriptURL) throws StudyCasterException {
    this.serverScriptURL = serverScriptURL;
    ticketCC = new Ticket(CLIENT_TICKET_BYTES);

    // Read ticket store.
    File ticketStore = new File(System.getProperty("java.io.tmpdir") + File.separator + TICKET_STORE_FILENAME);
    System.out.println(ticketStore.getAbsolutePath());
    boolean writeTicketStore = true;
    try {
      BufferedReader br = new BufferedReader(new FileReader(ticketStore));
      try {
        ticketFC = new Ticket(br.readLine(), CLIENT_TICKET_BYTES);
        ticketFS = new Ticket(br.readLine(), SERVER_TICKET_BYTES);
      } finally {
        br.close();
      }
      writeTicketStore = false;
    } catch (FileNotFoundException e) {
      // Ignore.
    } catch (IOException e) {
      StudyCaster2.log.log(Level.WARNING, "Problem reading ticket store.", e);
    } catch (StudyCasterException e) {
      StudyCaster2.log.log(Level.WARNING, "Problem reading ticket store.", e);
    }
    if (ticketFC == null)
      ticketFC = ticketCC;

    // Get server info.
    Map<String,String> fromHeader = PostRequest.emptyMap();
    fromHeader.put(HEADER_STM, null);
    fromHeader.put(HEADER_STK, null);
    long timeBef, timeAft;
    try {
      timeBef = System.currentTimeMillis();
      PostRequest.issuePost(this.serverScriptURL, serverParams("gsi"), PostRequest.<Map.Entry<String,InputStream>>emptyMap(), fromHeader).close();
      timeAft = System.currentTimeMillis();
    } catch (IOException e) {
      throw new StudyCasterException("Cannot retrieve server info.", e);
    }
    try {
      serverSecondsAhead = Long.parseLong(fromHeader.get(HEADER_STM)) - ((timeBef / 2 + timeAft / 2) / 1000L);
      StudyCaster2.log.info("Server time ahead by " + serverSecondsAhead + " seconds.");
    } catch (NumberFormatException e) {
      StudyCaster2.log.log(Level.WARNING, "Got bad time format from server", e);
    }
    // Let this exception propagate.
    ticketCS = new Ticket(fromHeader.get(HEADER_STK));
    if (ticketFS == null)
      ticketFS = ticketCS;

    logEnvironmentInfo();

    // Write ticket store.
    if (writeTicketStore) {
      try {
        FileWriter fw = new FileWriter(ticketStore);
        try {
          fw.write(ticketFC.toString() + "\n");
          fw.write(ticketFS.toString() + "\n");
        } finally {
          fw.close();
        }
        StudyCaster2.log.info("Wrote to ticket store.");
      } catch (IOException e) {
        StudyCaster2.log.log(Level.WARNING, "Problem writing ticket file.", e);
      }
    }
  }

  public void uploadFile(String fileName, InputStream is) throws IOException {
    Map<String,String> fromHeader = new LinkedHashMap<String,String>();
    fromHeader.put(HEADER_UPK, null);
    PostRequest.issuePost(serverScriptURL, serverParams("upl"), PostRequest.oneFile("file", fileName, is), fromHeader).close();
  }

  public InputStream downloadFile(String remoteName) throws IOException {
    Map<String,String> fromHeader = new LinkedHashMap<String,String>();
    fromHeader.put(HEADER_DNK, null);
    Map<String,String> params = serverParams("dnl");
    params.put("file", remoteName);
    return PostRequest.issuePost(serverScriptURL, params, PostRequest.<Map.Entry<String,InputStream>>emptyMap(), fromHeader);
  }

  public Ticket getTicketCC() {
    return ticketCC;
  }

  private void logEnvironmentInfo() {
    StudyCaster2.log.info(String.format("Tickets: FC = %s, CC = %s, FS = %s, CS = %s", ticketFC, ticketCC, ticketFS, ticketCS));

    String propkeys[] = new String[]
      {"java.vendor", "java.version", "java.class.version", "os.name", "os.arch", "os.version", "user.language", "user.region", "user.timezone"};
    StringBuffer props = new StringBuffer();
    boolean first = true;
    for (String key : propkeys) {
      props.append((first ? "" : ", ") + key + "=" + System.getProperty(key));
      first = false;
    }
    StudyCaster2.log.info("Environment: " + props);
  }
}
