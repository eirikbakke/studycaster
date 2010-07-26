package no.ebakke.studycaster2;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Deals with the protocol details of sending an HTTP POST request, possibly including file transfers. */
public final class PostRequest {
  private static int BUF_SIZE = 8192;

  private PostRequest() { }

  public static Map<String,Map.Entry<String,InputStream>> oneFile(String fieldName, String fileName, InputStream stream) {
    Map<String,Map.Entry<String,InputStream>> ret = emptyMap();
    ret.put(fieldName, new AbstractMap.SimpleEntry<String,InputStream>(fileName, stream));
    return ret;
  }

  public static <V> Map<String,V> emptyMap() {
    return new LinkedHashMap<String,V>();
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
    String boundary = new Ticket(20).toString();
    URLConnection conn = url.openConnection();
    conn.setDoOutput(true);
    conn.setDoInput(true);
    conn.setUseCaches(false);
    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
    BufferedOutputStream os = new BufferedOutputStream(conn.getOutputStream());
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
        byte ret[] = new byte[1];
        int got = read(ret);
        if (got != 1) {
          assert got == -1;
          return -1;
        }
        return ret[0] & 0xFF;
      }

      @Override
      public synchronized int read(byte[] b, int off, int len) throws IOException {
        if (remainingBytes == 0)
          return -1;
        int readMax = (remainingBytes >= 0 && len > remainingBytes) ? remainingBytes : len;
        int ret = retBuffer.read(b, off, readMax);
        if (ret < 0) {
          if (remainingBytes > 0)
            throw new IOException("Server returned too few bytes.");
        } else {
          assert ret > 0;
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
