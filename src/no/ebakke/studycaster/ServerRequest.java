package no.ebakke.studycaster;

import no.ebakke.studycaster2.Ticket;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import no.ebakke.studycaster.util.Pair;
import no.ebakke.studycaster2.PostRequest;

public class ServerRequest {
  private static final String HEADER_STM = "X-StudyCaster-ServerTime";
  private static final String HEADER_STK = "X-StudyCaster-ServerTicket";
  private static final String HEADER_UPK = "X-StudyCaster-UploadOK";
  private static final String HEADER_DNK = "X-StudyCaster-DownloadOK";
  private static int BUF_SIZE = 8192;

  public static Pair<Ticket, Long> getServerInfo(URL url, String at) throws IOException, StudyCasterException {
    Map<String,String> fromHeader = new LinkedHashMap<String,String>();
    fromHeader.put(HEADER_STM, null);
    fromHeader.put(HEADER_STK, null);
    PostRequest.issuePost(url, standardParams(at, "gsi"), PostRequest.<Map.Entry<String,InputStream>>emptyMap(), fromHeader).close();
    Long serverTime;
    try {
      serverTime = Long.parseLong(fromHeader.get(HEADER_STM));
    } catch (NumberFormatException e) {
      throw new IOException("Got bad time format from server", e);
    }
    return new Pair<Ticket, Long>(new Ticket(fromHeader.get(HEADER_STK)), serverTime);
  }

  public static void uploadFile(URL url, String at, String fileName, InputStream is) throws IOException {
    Map<String,String> fromHeader = new LinkedHashMap<String,String>();
    fromHeader.put(HEADER_UPK, null);
    PostRequest.issuePost(url, standardParams(at, "upl"), PostRequest.oneFile("file", fileName, is), fromHeader).close();
  }

  public static InputStream downloadFile(URL url, String at, String remoteName) throws IOException {
    Map<String,String> fromHeader = new LinkedHashMap<String,String>();
    fromHeader.put(HEADER_DNK, null);
    Map<String,String> params = standardParams(at, "dnl");
    params.put("file", remoteName);
    return PostRequest.issuePost(url, params, PostRequest.<Map.Entry<String,InputStream>>emptyMap(), fromHeader);
  }

  private static Map<String,String> standardParams(String at, String cmd) {
    Map<String,String> ret = new LinkedHashMap<String,String>();
    ret.put("tickets", at);
    ret.put("cmd", cmd);
    return ret;
  }
}
