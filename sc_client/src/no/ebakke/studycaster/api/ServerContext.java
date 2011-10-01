package no.ebakke.studycaster.api;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import no.ebakke.studycaster.util.Util;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

/** Handles protocol details specific to the server-side PHP script. */
public class ServerContext {
  private static final String SERVERURI_PROP_NAME = "studycaster.server.uri";
  // TODO: Don't expose this.
  public  static final int    DEF_UPLOAD_CHUNK_SZ = 64 * 1024;
  private static final String TICKET_STORE_FILENAME = "sc_7403204709139484951.tmp";
  private URI    serverScriptURI;
  private String launchTicket;
  private long   serverSecondsAhead;
  /* Keep the HttpClient in common for all requests to preserve the session cookie. */
  private DefaultHttpClient httpClient;

  public ServerContext() throws StudyCasterException {
    String serverScriptURIs = System.getProperty(SERVERURI_PROP_NAME);
    if (serverScriptURIs == null) {
      throw new StudyCasterException(
          "Property " + SERVERURI_PROP_NAME + " not set");
    }
    init(serverScriptURIs);
  }

  public ServerContext(String serverScriptURIs) throws StudyCasterException {
    init(serverScriptURIs);
  }

  private void init(String serverScriptURIs) throws StudyCasterException {
    StudyCaster.log.log(Level.INFO, "Using server URI {0}", serverScriptURIs);
    try {
      serverScriptURI = new URI(serverScriptURIs + "/api");
    } catch (URISyntaxException e) {
      throw new StudyCasterException("Malformed server URI", e);
    }

    // Read ticket store.
    File ticketStore = new File(System.getProperty("java.io.tmpdir") +
        File.separator + TICKET_STORE_FILENAME);
    boolean writeTicketStore = true;
    String clientCookie = null;
    try {
      BufferedReader br = new BufferedReader(new FileReader(ticketStore));
      try {
        clientCookie = br.readLine();
      } finally {
        br.close();
      }
      writeTicketStore = false;
    } catch (FileNotFoundException e) {
      // Ignore.
    } catch (IOException e) {
      StudyCaster.log.log(Level.WARNING, "Problem reading ticket store.", e);
    }

    // Get server info.
    Header headerSTM, headerLAT, headerCIE;
    long timeBef, timeAft;
    try {
      timeBef = System.currentTimeMillis();
      ThreadSafeClientConnManager connectionManager =
          new ThreadSafeClientConnManager();
      httpClient = new DefaultHttpClient(connectionManager);
      httpClient.setHttpRequestRetryHandler(new HttpRequestRetryHandler() {
        public boolean retryRequest(IOException exception, int executionCount,
            HttpContext context)
        {
          StudyCaster.log.log(Level.INFO,
              "Waiting to retry request ({0} times)", executionCount);
          try {
            Thread.sleep(10000);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          return true;
        }
      });
      
      HttpResponse response = requestHelper(httpClient, "gsi", null,
          clientCookie == null ? "" : clientCookie);
      timeAft = System.currentTimeMillis();
      headerSTM = response.getFirstHeader("X-StudyCaster-ServerTime");
      headerLAT = response.getFirstHeader("X-StudyCaster-LaunchTicket");
      headerCIE = response.getFirstHeader("X-StudyCaster-ClientCookie");
      EntityUtils.consume(response.getEntity());
      if (headerSTM == null || headerLAT == null || headerCIE == null)
        throw new StudyCasterException("Missing initialization headers.");
      if (clientCookie != null && !clientCookie.equals(headerCIE.getValue()))
        throw new StudyCasterException("Server returned odd client cookie");
      clientCookie = headerCIE.getValue();
    } catch (IOException e) {
      throw new StudyCasterException("Cannot retrieve server info.", e);
    }
    launchTicket = headerLAT.getValue();
    try {
      // TODO: Do this only for a single successful request.
      serverSecondsAhead = (Long.parseLong(headerSTM.getValue()) -
          (timeBef / 2 + timeAft / 2)) / 1000L;
      StudyCaster.log.log(Level.INFO, "Server time ahead by {0} seconds.",
          serverSecondsAhead);
    } catch (NumberFormatException e) {
      StudyCaster.log.log(Level.WARNING, "Got bad time format from server", e);
    }
    StudyCaster.log.info(String.format("clientCookie = %s, launchTicket = %s",
        clientCookie, launchTicket));
    Util.logEnvironmentInfo();

    // Write ticket store.
    if (writeTicketStore) {
      try {
        FileWriter fw = new FileWriter(ticketStore);
        try {
          fw.write(clientCookie.toString() + "\n");
        } finally {
          fw.close();
        }
        StudyCaster.log.info("Wrote to ticket store.");
      } catch (IOException e) {
        StudyCaster.log.log(Level.WARNING, "Problem writing ticket file.", e);
      }
    }
  }

  public static String getContentString(HttpResponse resp) throws IOException {
    StringWriter ret = new StringWriter();
    InputStream errContent = resp.getEntity().getContent();
    try {
      Header encoding = resp.getEntity().getContentEncoding();
      IOUtils.copy(errContent, ret,
          (encoding == null) ? "UTF-8" : encoding.getValue());
    } finally {
      errContent.close();
    }
    return ret.toString();
  }

  private HttpResponse requestHelper(HttpClient httpClient, String cmd,
      ContentBody content, String arg) throws IOException
  {
    HttpResponse ret = null;
    do {
      try {
        ret = requestHelperSingle(httpClient, cmd, content, arg);
      } catch (IOException e) {
        StudyCaster.log.log(Level.WARNING, "Failed request beyond HttpClient", e);
      }
      if (ret == null) {
        StudyCaster.log.log(Level.INFO, "Waiting to retry request");
        try {
          Thread.sleep(10000);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    } while (ret == null);
    return ret;
  }

  private HttpResponse requestHelperSingle(HttpClient httpClient, String cmd,
      ContentBody content, String arg) throws IOException
  {
    HttpPost httpPost = new HttpPost(serverScriptURI);
    MultipartEntity params = new MultipartEntity();
    params.addPart("cmd", new StringBody(cmd));
    params.addPart("lt",
        new StringBody(launchTicket == null ? "" : launchTicket));
    if (content != null)
      params.addPart("content", content);
    if (arg != null)
      params.addPart("arg", new StringBody(arg));
    httpPost.setEntity(params);
    HttpResponse response = httpClient.execute(httpPost);
    if (response.getEntity() == null)
      throw new IOException("Failed to get response entity.");
    try {
      if (response.getStatusLine().getStatusCode() != 200) {
        if (response.getStatusLine().getStatusCode() == 404)
          throw new FileNotFoundException();

        throw new IOException("Command " + cmd + " got bad status code " +
            response.getStatusLine().getReasonPhrase() + " (" +
            getContentString(response) + ")");
      }
      Header okHeader = response.getFirstHeader("X-StudyCaster-OK");
      if (okHeader == null)
        throw new IOException("Failed to get StudyCaster response header");
      if (!okHeader.getValue().equals(cmd))
        throw new IOException("Got invalid StudyCaster response header: " + okHeader.getValue());
      return response;
    } catch (IOException e) {
      EntityUtils.consume(response.getEntity());
      throw e;
    } catch (RuntimeException e) {
      EntityUtils.consume(response.getEntity());
      throw e;
    }
  }

  public void enterRemoteLogRecord(final String msg) {
    StudyCaster.log.log(Level.INFO, "Queueing remote log entry \"{0}\"", msg);
    new Thread(new Runnable() {
      public void run() {
        try {
          EntityUtils.consume(requestHelper(httpClient, "log",
              new StringBody(msg), Integer.toString((int) (Math.random() *
              Integer.MAX_VALUE))).getEntity());
        } catch (IOException e) {
          StudyCaster.log.log(Level.WARNING, "Failed to enter remote log entry \"" + msg + "\"", e);
        }
      }
    }, "log-cmd").start();
  }

  public OutputStream uploadFile(final String remoteName) throws IOException {
    EntityUtils.consume(requestHelper(httpClient, "upc",
        new StringBody(remoteName), null).getEntity());

    return new BufferedOutputStream(new OutputStream() {
      private boolean closed;
      private long written = 0;

      @Override
      public void write(int b) throws IOException {
         write(new byte[] { (byte) b }, 0, 1);
      }

      @Override
      public void write(byte[] b, int off, final int len) throws IOException {
        if (closed)
          throw new IOException("Stream closed");

        for (int subOff = 0; subOff < len; ) {
          final int subLen = Math.min(len - subOff, DEF_UPLOAD_CHUNK_SZ);
          byte chunk[] = Util.copyOfRange(b, off + subOff, off + subOff + subLen);
          /* Use a ByteArrayBody rather than an InputStreamBody in case the
          request needs to be repeated. */
          ByteArrayBody bab = new ByteArrayBody(chunk, remoteName);
          EntityUtils.consume(requestHelper(httpClient, "upa", bab,
              Long.toString(written)).getEntity());
          subOff += subLen;
          written += subLen;
        }
      }

      @Override
      public void close() throws IOException {
        if (closed)
          return;
        closed = true;
      }
    }, DEF_UPLOAD_CHUNK_SZ);
  }

  public InputStream downloadFile(String remoteName) throws IOException {
    final InputStream ret = requestHelper(httpClient, "dnl",
        new StringBody(remoteName), null).getEntity().getContent();
    return new InputStream() {
      private boolean closed;

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
        ret.close();
      }
    };
  }

  public String getLaunchTicket() {
    return launchTicket;
  }

  public long getServerSecondsAhead() {
    return serverSecondsAhead;
  }

  // TODO: Finish the implementation of this, and have clients call it.
  public void close() {
    httpClient.getConnectionManager().shutdown();
  }
}
