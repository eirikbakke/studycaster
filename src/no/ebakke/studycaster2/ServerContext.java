package no.ebakke.studycaster2;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.logging.Level;
import no.ebakke.studycaster.StudyCasterException;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

/** Handles protocol details specific to our server-side PHP script. */
public class ServerContext {
  private static final int DEF_UPLOAD_CHUNK_SZ = 32 * 1024;
  private static final String TICKET_STORE_FILENAME = "sc_7403204709139484951.tmp";
  private static final int CLIENT_TICKET_BYTES = 6;
  private static final int SERVER_TICKET_BYTES = 3;
  private URI    serverScriptURI;
  private Ticket ticketFC; // First client ticket on this machine
  private Ticket ticketCC; // Current client ticket
  private Ticket ticketFS; // First server ticket on this machine
  private Ticket ticketCS; // Current server ticket
  private long   serverSecondsAhead;

  public ServerContext(URI serverScriptURI) throws StudyCasterException {
    this.serverScriptURI = serverScriptURI;
    ticketCC = new Ticket(CLIENT_TICKET_BYTES);

    // Read ticket store.
    File ticketStore = new File(System.getProperty("java.io.tmpdir") + File.separator + TICKET_STORE_FILENAME);
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
    Header headerSTM, headerSTK;
    long timeBef, timeAft;
    try {
      timeBef = System.currentTimeMillis();
      HttpClient httpClient = new DefaultHttpClient();
      try {
        HttpResponse response = requestHelper(httpClient, "gsi", null);
        timeAft = System.currentTimeMillis();
        headerSTM = response.getFirstHeader("X-StudyCaster-ServerTime");
        headerSTK = response.getFirstHeader("X-StudyCaster-ServerTicket");
        response.getEntity().consumeContent();
      } finally {
        httpClient.getConnectionManager().shutdown();
      }
      if (headerSTM == null || headerSTK == null)
        throw new StudyCasterException("Server response missing initialization headers.");
    } catch (IOException e) {
      throw new StudyCasterException("Cannot retrieve server info.", e);
    }
    try {
      serverSecondsAhead = Long.parseLong(headerSTM.getValue()) - ((timeBef / 2 + timeAft / 2) / 1000L);
      StudyCaster2.log.info("Server time ahead by " + serverSecondsAhead + " seconds.");
    } catch (NumberFormatException e) {
      StudyCaster2.log.log(Level.WARNING, "Got bad time format from server", e);
    }
    // Let this exception propagate.
    ticketCS = new Ticket(headerSTK.getValue());
    if (ticketFS == null)
      ticketFS = ticketCS;

    StudyCaster2.log.info(String.format("Tickets: FC = %s, CC = %s, FS = %s, CS = %s", ticketFC, ticketCC, ticketFS, ticketCS));
    Util.logEnvironmentInfo();

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

  private HttpResponse requestHelper(HttpClient httpClient, String cmd, ContentBody fileBody) throws IOException {
    // System.out.println("Called with cmd " + cmd + " fileBody " + fileBody);
    HttpPost httpPost = new HttpPost(serverScriptURI);
    MultipartEntity params = new MultipartEntity();
    String allTickets =
            ticketFC + "," +
            ticketCC + "," +
           ((ticketFS != null) ? ticketFS : "") + "," +
           ((ticketCS != null) ? ticketCS : "");
    params.addPart("tickets", new StringBody(allTickets));
    params.addPart("cmd", new StringBody(cmd));
    if (fileBody != null)
      params.addPart("file", fileBody);
    httpPost.setEntity(params);
    HttpResponse response = httpClient.execute(httpPost);
    if (response.getEntity() == null)
      throw new IOException("Failed to get response entity.");
    try {
      if (response.getStatusLine().getStatusCode() != 200) {
        if (response.getStatusLine().getStatusCode() == 404)
          throw new FileNotFoundException();
        throw new IOException("Got bad status code " + response.getStatusLine().getReasonPhrase());
      }
      Header okHeader = response.getFirstHeader("X-StudyCaster-OK");
      if (okHeader == null)
        throw new IOException("Failed to get StudyCaster response header");
      if (!okHeader.getValue().equals(cmd))
        throw new IOException("Got invalid StudyCaster response header: " + okHeader.getValue());
      return response;
    } catch (IOException e) {
      response.getEntity().consumeContent();
      throw e;
    } catch (RuntimeException e) {
      response.getEntity().consumeContent();
      throw e;
    }
  }

  public OutputStream uploadFile(final String remoteName) throws IOException {
    final HttpClient httpClient = new DefaultHttpClient();
    requestHelper(httpClient, "upc", new StringBody(remoteName)).getEntity().consumeContent();
    
    return new BufferedOutputStream(new OutputStream() {
      private boolean closed;

      @Override
      public void write(int b) throws IOException {
         write(new byte[] { (byte) b }, 0, 1);
      }

      @Override
      public void write(byte[] b, int off, final int len) throws IOException {
        if (closed)
          throw new IOException("Stream closed");
        InputStreamBody isb = new InputStreamBody(new ByteArrayInputStream(b, off, len), remoteName) {
          @Override
          public long getContentLength() {
            return len;
          }
        };
        requestHelper(httpClient, "upa", isb).getEntity().consumeContent();
      }

      @Override
      public void close() throws IOException {
        if (closed)
          return;
        closed = true;
        httpClient.getConnectionManager().shutdown();
      }
    }, DEF_UPLOAD_CHUNK_SZ);
  }

  public InputStream downloadFile(String remoteName) throws IOException {
    final HttpClient httpClient = new DefaultHttpClient();
    final InputStream ret = requestHelper(httpClient, "dnl", new StringBody(remoteName)).getEntity().getContent();
    return new InputStream() {
      boolean closed;

      @Override
      public int read() throws IOException {
        return ret.read();
      }
      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        return ret.read(b, off, len);
      }
      @Override
      public void close() throws IOException {
        if (closed)
          return;
        closed = true;
        try {
          ret.close();
        } finally {
          httpClient.getConnectionManager().shutdown();
        }
      }
    };
  }

  public Ticket getTicketCC() {
    return ticketCC;
  }

  public long getServerSecondsAhead() {
    return serverSecondsAhead;
  }
}
