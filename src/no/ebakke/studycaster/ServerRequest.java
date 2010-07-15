package no.ebakke.studycaster;

import no.ebakke.studycaster.util.Util;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import no.ebakke.studycaster.util.Pair;

public class ServerRequest {
  private static final String HEADER_STM = "X-StudyCaster-ServerTime";
  private static final String HEADER_STK = "X-StudyCaster-ServerTicket";
  private static final String HEADER_UPK = "X-StudyCaster-UploadOK";
  private static final String HEADER_DNK = "X-StudyCaster-DownloadOK";
  private static int BUF_SIZE = 8192;

  public static Pair<Ticket, Long> getServerInfo(URL url, String at) throws IOException, StudyCasterException {
    Map<String, String> fromHeader = new LinkedHashMap<String,String>();
    fromHeader.put(HEADER_STM, null);
    fromHeader.put(HEADER_STK, null);
    issuePost(url, standardParams(at, "gsi"), noFileData(), fromHeader).close();
    Long serverTime;
    try {
      serverTime = Long.parseLong(fromHeader.get(HEADER_STM));
    } catch (NumberFormatException e) {
      throw new IOException("Got bad time format from server", e);
    }
    return new Pair<Ticket, Long>(new Ticket(fromHeader.get(HEADER_STK)), serverTime);
  }

  public static void uploadFile(URL url, String at, String fileName, InputStream is) throws IOException {
    Map<String, String> fromHeader = new LinkedHashMap<String,String>();
    fromHeader.put(HEADER_UPK, null);
    issuePost(url, standardParams(at, "upl"), oneFile("file", fileName, is), fromHeader).close();
  }

  public static File downloadFile(URL url, String at, String remoteName) throws IOException {
    File ret = File.createTempFile("sc_", "_" + remoteName);
    Map<String, String> fromHeader = new LinkedHashMap<String,String>();
    fromHeader.put(HEADER_DNK, null);
    Map<String,String> params = standardParams(at, "dnl");
    params.put("file", remoteName);
    InputStream is = issuePost(url, params, noFileData(), fromHeader);
    FileOutputStream os = null;
    try {
      os = new FileOutputStream(ret);
      Util.streamCopy(is, os);
    } finally {
      if (os != null)
        os.close();
    }
    is.close();
    return ret;
  }

  private static Map<String,String> standardParams(String at, String cmd) {
    Map<String, String> ret = new LinkedHashMap<String, String>();
    ret.put("ct", at);
    ret.put("cmd", cmd);
    return ret;
  }

  private static Map<String,Map.Entry<String,InputStream>> oneFile(String fieldName, String fileName, InputStream stream) {
    Map<String,Map.Entry<String,InputStream>> ret = noFileData();
    ret.put(fieldName, new AbstractMap.SimpleEntry<String, InputStream>(fileName, stream));
    return ret;
  }

  public static Map<String,Map.Entry<String,InputStream>> noFileData() {
    return new LinkedHashMap<String,Map.Entry<String,InputStream>>();
  }

  /** Note: Caller's responsibility to close supplied InputStream objects. */
  public static InputStream issuePost(URL url,
          Map<String,String> textData,
          Map<String,Map.Entry<String,InputStream>> fileData,
          Map<String,String> fromHeader) throws IOException
  {
   /* See:
        http://stackoverflow.com/questions/2469451/upload-files-with-java/2469587#2469587
        http://stackoverflow.com/questions/2477449/simple-stream-read-write-question-in-java/2478127#2478127
        http://wiki.forum.nokia.com/index.php/HTTP_Post_multipart_file_upload_in_Java_ME
        http://www.faqs.org/rfcs/rfc1867.html
        http://www.w3.org/TR/html401/interact/forms.html
    */
    String boundary = new Ticket().toString();
    URLConnection conn = url.openConnection();
    conn.setDoOutput(true);
    conn.setDoInput(true);
    conn.setUseCaches(false);
    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
    BufferedOutputStream os = new BufferedOutputStream(conn.getOutputStream());
    //PrintStream os = System.out;
    os.write(("Content-Type: multipart/form-data; boundary=" + boundary + "\r\n\r\n").getBytes());
    try {
      // TODO: Form field names should be encoded using RFC 1522.
      for (Map.Entry<String,String> ent : textData.entrySet()) {
        os.write((
           "--" + boundary + "\r\n" +
          "Content-Disposition: form-data; name=\"" + ent.getKey() + "\"\r\n" +
          "Content-Type: text/plain; charset=UTF-8\r\n" +
          "\r\n" + ent.getValue() + "\r\n"
          ).getBytes());
      }
      for (Map.Entry<String,Map.Entry<String,InputStream>> ent : fileData.entrySet()) {
        os.write((
          "--" + boundary + "\r\n" +
          "Content-Disposition: form-data; name=\"" + ent.getKey() + "\"; filename=\"" + ent.getValue().getKey() + "\"\r\n" +
          "Content-Type: application/octet-stream\r\n" +
          "Content-Transfer-Encoding: binary\r\n" +
          "\r\n"
          ).getBytes());

        int  got;
        byte buf[] = new byte[BUF_SIZE];
        while ((got = ent.getValue().getValue().read(buf)) >= 0)
          os.write(buf, 0, got);
        os.write(("\r\n").getBytes());
      }
      os.write(("--" + boundary + "\r\n").getBytes());
      os.flush();
    } finally {
      os.close();
    }

    final BufferedInputStream retBuffer = new BufferedInputStream(conn.getInputStream());
    final int contentLength = conn.getContentLength();

    for (Map.Entry<String,String> ent : fromHeader.entrySet()) {
      ent.setValue(conn.getHeaderField(ent.getKey()));
      if (ent.getValue() == null) {
        String contentType = conn.getHeaderField("Content-Type");
        if (contentType != null && contentType.startsWith("text/")) {
          StringBuffer sb = new StringBuffer();
          System.out.println();
          BufferedReader br = null;
          try {
            br = new BufferedReader(new InputStreamReader(retBuffer));
            String line;
            while ((line = br.readLine()) != null)
              sb.append("  " + line + "\n");
            br.close();
          } catch (IOException e) {
            throw new IOException("Couldn't retrieve result after missing header " + ent.getKey() + ".", e);
          } finally {
            if (br != null)
              br.close();
          }
          throw new IOException("No header " + ent.getKey() + " returned, got the following result:\n" + sb.toString());
        } else {
          throw new IOException("No header " + ent.getKey() + " returned, got " +
                  (contentType != null ? ("content type " + contentType) : "unknown content type."));
        }
      }
    }

    return new InputStream() {
      private int remainingBytes = contentLength;

      @Override
      public int read() throws IOException {
        byte [] ret = new byte[1];
        read(ret, 0, 1);
        return ret[0];
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        if (remainingBytes == 0)
          return -1;
        int readMax = (remainingBytes >= 0 && len > remainingBytes) ? remainingBytes : len;
        int ret = retBuffer.read(b, off, readMax);
        if (ret < 0) {
          if (remainingBytes > 0)
            throw new IOException("Server returned too few bytes.");
        } else {
          if (remainingBytes > 0)
            remainingBytes -= ret;
        }
        return ret;
      }

      @Override
      public void close() throws IOException {
        retBuffer.close();
      }

      @Override
      public int available() throws IOException {
        return retBuffer.available();
      }
    };
  }
}
