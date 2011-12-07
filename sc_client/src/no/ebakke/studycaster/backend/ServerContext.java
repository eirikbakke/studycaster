package no.ebakke.studycaster.backend;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.ebakke.studycaster.util.Util;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
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

/** Represents a connection to the StudyCaster server, and handles all details of the server API.
Thread-safe. */
public class ServerContext {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  private static final double SIMULATE_LATENCY_MILLIS = 0.0;
  private static final String SERVERURI_PROP_NAME = "studycaster.server.uri";
  private static final int    DEF_UPLOAD_CHUNK_SZ = 64 * 1024;
  // TODO: Rename this (legacy from earlier experiments).
  private static final String TICKET_STORE_FILENAME = "sc_7403204709139484951.tmp";
  private final URI    serverScriptURI;
  private final String launchTicket;
  private final long   serverMillisAhead;
  /* Keep the HttpClient in common for all requests, as is standard for this interface. This would
  also preserve any session cookies involved, though the API currently does not use any. */
  private final DefaultHttpClient httpClient;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  private static String getServerScriptURIfromProperty() throws StudyCasterException {
    String serverScriptURIs = System.getProperty(SERVERURI_PROP_NAME);
    if (serverScriptURIs == null)
      throw new StudyCasterException("Property " + SERVERURI_PROP_NAME + " not set");
    return serverScriptURIs;
  }

  public ServerContext() throws StudyCasterException {
    this(getServerScriptURIfromProperty());
  }

  private long getServerTimeMillis() throws StudyCasterException {
    HttpResponse response;
    try {
      if (SIMULATE_LATENCY_MILLIS > 0.0)
        Thread.sleep((int) (Math.random() * SIMULATE_LATENCY_MILLIS / 2.0));
      response = requestHelper("tim", null, null);
      if (SIMULATE_LATENCY_MILLIS > 0.0)
        Thread.sleep((int) (Math.random() * SIMULATE_LATENCY_MILLIS / 2.0));
      try {
        Header headerSTM = response.getFirstHeader("X-StudyCaster-ServerTime");
        if (headerSTM == null)
          throw new StudyCasterException("Missing server time response header");
        return Long.parseLong(headerSTM.getValue());
      } finally {
        EntityUtils.consume(response.getEntity());
      }
    } catch (InterruptedException e) {
      throw new StudyCasterException("Interrupted during latency simulation", e);
    } catch (NumberFormatException e) {
      throw new StudyCasterException("Got bad time format from server", e);
    } catch (IOException e) {
      throw new StudyCasterException("Failed to retrieve server time", e);
    }
  }

  @SuppressWarnings("AssignmentReplaceableWithOperatorAssignment")
  private long measureServerMillisAhead() throws StudyCasterException {
    final int MAX_ATTEMPTS = 15;
    // See http://en.wikipedia.org/wiki/Standard_deviation#Rapid_calculation_methods .
    // Running average, sum of squares, last number of samples, and last standard deviation.
    double A = 0, Q = 0, N = 0, stdev = Double.POSITIVE_INFINITY;
    for (int i = 1, attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
      // A single measurement.
      final double x;
      {
        final long nanosBefore = System.nanoTime();
        final long serverTime = getServerTimeMillis();
        final long nanosAfter  = System.nanoTime();
        final double requestLength = (nanosAfter - nanosBefore) / 1000000.0;
        if (requestLength > 1000.0 && !(N == 0 && attempt == MAX_ATTEMPTS - 1))
          continue;
        final double adjustedLocalTime = System.currentTimeMillis() - requestLength / 2.0;
        x = serverTime - adjustedLocalTime;
      }
      Q = Q + ((i - 1.0) / i) * (x - A) * (x - A);
      A = A + (x - A) / i;
      N = i;
      i++;
      stdev = (N == 1) ? Double.POSITIVE_INFINITY : Math.sqrt(Q / (N - 1.0));
      if (N >= 5 && stdev < 50.0)
        break;
    }
    LOG.log(Level.INFO,
        "Measured server time ahead by {0,number,#}+/-{1,number,#}ms with {2} samples",
        new Object[]{ A, stdev, N});
    return Math.round(A);
  }

  public ServerContext(String serverScriptURIs) throws StudyCasterException {
    LOG.log(Level.INFO, "Using server URI {0}", serverScriptURIs);
    try {
      serverScriptURI = new URI(serverScriptURIs + "/api");
    } catch (URISyntaxException e) {
      throw new StudyCasterException("Malformed server URI", e);
    }

    // Read ticket store.
    File ticketStore = new File(System.getProperty("java.io.tmpdir"), TICKET_STORE_FILENAME);
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
      LOG.log(Level.WARNING, "Problem reading ticket store.", e);
    }

    // Setup launch session.
    Header headerLAT, headerCIE;
    try {
      ThreadSafeClientConnManager connectionManager = new ThreadSafeClientConnManager();
      httpClient = new DefaultHttpClient(connectionManager);
      httpClient.setHttpRequestRetryHandler(new HttpRequestRetryHandler() {
        public boolean retryRequest(IOException exception, int executionCount,
            HttpContext context)
        {
          if (exception instanceof NonRetriableException) {
            LOG.log(Level.WARNING, "Non-retriable error encountered in HttpRequestRetryHandler");
            return false;
          } else {
            LOG.log(Level.INFO, "Waiting to retry request ({0} times)", executionCount);
            try {
              Thread.sleep(10000);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
            return true;
          }
        }
      });

      HttpResponse response = requestHelper("gsi", null, clientCookie == null ? "" : clientCookie);
      try {
        headerLAT = response.getFirstHeader("X-StudyCaster-LaunchTicket");
        headerCIE = response.getFirstHeader("X-StudyCaster-ClientCookie");
      } finally {
        EntityUtils.consume(response.getEntity());
      }
      if (headerLAT == null || headerCIE == null)
        throw new StudyCasterException("Missing initialization headers.");
      if (clientCookie != null && !clientCookie.equals(headerCIE.getValue()))
        throw new StudyCasterException("Server returned odd client cookie");
      clientCookie = headerCIE.getValue();
    } catch (IOException e) {
      throw new StudyCasterException("Cannot retrieve server info.", e);
    }
    launchTicket = headerLAT.getValue();
    LOG.log(Level.INFO, "clientCookie = {0}, launchTicket = {1}",
        new Object[] {clientCookie, launchTicket});

    // Write ticket store.
    if (writeTicketStore) {
      try {
        FileWriter fw = new FileWriter(ticketStore);
        try {
          fw.write(clientCookie.toString() + "\n");
        } finally {
          fw.close();
        }
        LOG.info("Wrote to ticket store.");
      } catch (IOException e) {
        LOG.log(Level.WARNING, "Problem writing ticket file.", e);
      }
    }
    serverMillisAhead = measureServerMillisAhead();
    Util.logEnvironmentInfo();
  }

  private static String getContentString(HttpResponse resp) throws IOException {
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

  /** Callers must always remember to consume the response, or future requests may hang. */
  @SuppressWarnings("SleepWhileInLoop")
  private HttpResponse requestHelper(String cmd, ContentBody content, String arg) throws IOException
  {
    HttpResponse ret = null;
    do {
      try {
        ret = requestHelperSingle(cmd, content, arg);
      } catch (IOException e) {
        LOG.log(Level.WARNING, "Failed request beyond HttpClient", e);
        if (e instanceof NonRetriableException)
          throw e;
      }
      if (ret == null) {
        LOG.log(Level.INFO, "Waiting to retry request");
        try {
          Thread.sleep(10000);
        } catch (InterruptedException e) {
          throw new IOException("HTTP request retry thread interrupted");
        }
      }
    } while (ret == null);
    return ret;
  }

  private HttpResponse requestHelperSingle(String cmd, ContentBody content, String arg)
      throws IOException
  {
    HttpPost httpPost = new HttpPost(serverScriptURI);
    MultipartEntity params = new MultipartEntity();
    params.addPart("cmd", new StringBody(cmd));
    params.addPart("lt", new StringBody(launchTicket == null ? "" : launchTicket));
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
        Header disableRetryHeader = response.getFirstHeader("X-StudyCaster-DisableRetry");
        Header errorMessageHeader = response.getFirstHeader("X-StudyCaster-ErrorMessage");
        boolean disableRetry =
            disableRetryHeader == null ? false : disableRetryHeader.getValue().equals("true");
        String errorMessage =
            errorMessageHeader == null ? getContentString(response) : errorMessageHeader.getValue();
        String exceptionMessage = "Command " + cmd + " got bad status code " +
            response.getStatusLine().getReasonPhrase() + "; " + errorMessage;
        if (disableRetry) {
          throw new NonRetriableException(exceptionMessage);
        } else {
          throw new IOException(exceptionMessage);
        }
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

  public OutputStream uploadFile(final String remoteName) throws IOException {
    Util.checkClosed(closed);
    EntityUtils.consume(requestHelper("upc", new StringBody(remoteName), null).getEntity());

    return new BufferedOutputStream(new OutputStream() {
      private final AtomicBoolean closed  = new AtomicBoolean(false);
      private final AtomicLong    written = new AtomicLong(0);

      @Override
      public void write(int b) throws IOException {
         write(new byte[] { (byte) b }, 0, 1);
      }

      @Override
      public void write(byte[] b, int off, final int len) throws IOException {
        Util.checkClosed(closed);

        // TODO: Deduplicate with chunking logic in WriteOpQueue.
        for (int subOff = 0; subOff < len; ) {
          final int subLen = Math.min(len - subOff, DEF_UPLOAD_CHUNK_SZ);
          byte chunk[] = Util.copyOfRange(b, off + subOff, off + subOff + subLen);
          /* Use a ByteArrayBody rather than an InputStreamBody in case the
          request needs to be repeated. */
          ByteArrayBody bab = new ByteArrayBody(chunk, remoteName);
          EntityUtils.consume(requestHelper("upa", bab, Long.toString(written.get())).getEntity());
          subOff += subLen;
          written.addAndGet(subLen);
        }
      }

      @Override
      public void close() throws IOException {
        closed.set(true);
      }
    }, DEF_UPLOAD_CHUNK_SZ);
  }

  public InputStream downloadFile(String remoteName) throws IOException {
    final InputStream ret = requestHelper("dnl",
        new StringBody(remoteName), null).getEntity().getContent();
    return new InputStream() {
      private final AtomicBoolean closed = new AtomicBoolean(false);

      @Override
      public int read() throws IOException {
        Util.checkClosed(closed);
        return ret.read();
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        Util.checkClosed(closed);
        return ret.read(b, off, len);
      }

      @Override
      public void close() throws IOException {
        if (closed.getAndSet(true))
          return;
        ret.close();
      }
    };
  }

  public String getLaunchTicket() {
    return launchTicket;
  }

  public long getServerMillisAhead() {
    return serverMillisAhead;
  }

  public void close() {
    if (closed.getAndSet(true))
      return;
    httpClient.getConnectionManager().shutdown();
  }

  private static class NonRetriableException extends IOException {
    private static final long serialVersionUID = 1L;

    NonRetriableException(String s) {
      super(s);
    }
  }
}
