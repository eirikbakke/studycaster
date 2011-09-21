package no.ebakke.studycaster.reporting;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import no.ebakke.studycaster.backend.BackendUtil;
import no.ebakke.studycaster.backend.Request;
import no.ebakke.studycaster.util.ColUtil;
import org.apache.commons.lang3.tuple.Pair;

public final class Reports {
  private Reports() { }

  public static List<Subject> getSubjectReport() {
    // Each key is a launchTicket.
    Map<String,Date> firstRequest = ColUtil.newOrderedMap();
    Map<String,Date> lastRequest   = ColUtil.newOrderedMap();
    Map<String,Long> contentSize = ColUtil.newOrderedMap();
    Map<String,Date> firstAddrRequest = ColUtil.newOrderedMap();

    Linker<Request> linker = new Linker<Request>();
    // TODO: Only get DISTINCT the fields we care about for correlation.
    for (Request r : BackendUtil.getRequests()) {
      Map<Pair<String,Boolean>,String> links = ColUtil.newOrderedMap();
      links.put(Pair.of("remoteAddrHash", true ), r.getRemoteAddrHash());
      links.put(Pair.of("clientCookie"  , true ), r.getClientCookie());
      links.put(Pair.of("launchTicket"  , true ), r.getLaunchTicket());
      links.put(Pair.of("geoLocation"   , false), r.getGeoLocation());
      if ("jws".equals(r.getType()))
        links.put(Pair.of("versionString", false), r.getLogEntry());
      linker.addLink(links, r);
      ColUtil.putExtreme(firstRequest, r.getLaunchTicket(), r.getTime(), false);
      ColUtil.putExtreme(lastRequest , r.getLaunchTicket(), r.getTime(), true );
      ColUtil.putSum(contentSize, r.getLaunchTicket(), r.getContentSize());
      ColUtil.putExtreme(firstAddrRequest, r.getRemoteAddrHash(), r.getTime(), false);
    }
    List<Subject> ret = new ArrayList<Subject>();
    for (Linker.Node<Request> node : linker.getContent()) {
      Date firstOverall = null;
      for (String remoteAddrHash : node.getLinks("remoteAddrHash")) {
        Date time = firstAddrRequest.get(remoteAddrHash);
        if (firstOverall == null || (time != null && time.before(firstOverall)))
          firstOverall = time;
      }
      System.out.println("subject:");
      System.out.println("  firstRequest  : " + firstOverall);
      System.out.println("  clientCookie  : " + node.getLinks("clientCookie"));
      System.out.println("  remoteAddrHash: " + node.getLinks("remoteAddrHash"));
      System.out.println("  geoLocation   : " + node.getLinks("geoLocation"));
      System.out.println("  versionString : " + node.getLinks("versionString"));
      for (String launchTicket : node.getLinks("launchTicket")) {
        System.out.println("  launchCode    : " + launchTicket);
        System.out.println("    firstRequest: " + firstRequest.get(launchTicket));
        System.out.println("    lastRequest : " + lastRequest.get(launchTicket));
        System.out.println("    contentSize : " +
            (contentSize.get(launchTicket) / 1024) + "K");
      }
    }
    return ret;
  }

  public static void main(String args[]) {
    for (Subject s : getSubjectReport()) {
      System.out.println(s);
    }
  }

  public static class Subject {
    Date         firstRequest;
    List<Launch> launches;
    List<String> clientCookies;
    List<String> remoteAddrHashes;
  }

  public static class Launch {
    Date          firstRequest;
    String        launchTicket;
    List<Request> requests;
  }

  public static class Location {
    String remoteAddrHash;
    String geoLocation;
  }
}
