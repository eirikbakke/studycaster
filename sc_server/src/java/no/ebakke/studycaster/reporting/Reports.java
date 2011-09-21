package no.ebakke.studycaster.reporting;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
      Subject subject = new Subject();
      Date firstOverall = null;
      for (String remoteAddrHash : node.getLinks("remoteAddrHash")) {
        Date time = firstAddrRequest.get(remoteAddrHash);
        if (firstOverall == null || (time != null && time.before(firstOverall)))
          firstOverall = time;
      }
      subject.firstRequest   = firstOverall;
      subject.clientCookie   = node.getLinks("clientCookie");
      subject.remoteAddrHash = node.getLinks("remoteAddrHash");
      subject.geoLocation    = node.getLinks("geoLocation");
      subject.versionString  = node.getLinks("versionString");
      subject.launches       = ColUtil.newList();
      for (String launchTicket : node.getLinks("launchTicket")) {
        Launch launch = new Launch();
        launch.launchTicket = launchTicket;
        launch.firstRequest = firstRequest.get(launchTicket);
        launch.lastRequest  = lastRequest.get(launchTicket);
        launch.contentSize  = contentSize.get(launchTicket) / 1024;
        subject.launches.add(launch);
      }
      ret.add(subject);
    }
    return ret;
  }

  public static void main(String args[]) {
    for (Subject s : getSubjectReport()) {
      System.out.println(s);
    }
  }

  public static class Subject {
    private Date         firstRequest;
    private Set<String>  clientCookie;
    private Set<String>  remoteAddrHash;
    private Set<String>  geoLocation;
    private Set<String>  versionString;
    private List<Launch> launches;

    public Date getFirstRequest() {
      return firstRequest;
    }

    public Set<String> getClientCookie() {
      return clientCookie;
    }

    public Set<String> getRemoteAddrHash() {
      return remoteAddrHash;
    }

    public Set<String> getGeoLocation() {
      return geoLocation;
    }

    public List<Launch> getLaunches() {
      return launches;
    }

    public Set<String> getVersionString() {
      return versionString;
    }

    @Override
    public String toString() {
      StringBuilder ret = new StringBuilder();
      ret.append("subject:\n");
      ret.append("  firstRequest  : ").append(firstRequest).append("\n");
      ret.append("  clientCookie  : ").append(clientCookie).append("\n");
      ret.append("  remoteAddrHash: ").append(remoteAddrHash).append("\n");
      ret.append("  geoLocation   : ").append(geoLocation).append("\n");
      ret.append("  versionString : ").append(versionString).append("\n");
      ret.append("  launches      :\n");
      for (Launch launch : launches)
        ret.append(launch.toString());
      return ret.toString();
    }
  }

  public static class Launch {
    private String launchTicket;
    private Date   firstRequest;
    private Date   lastRequest;
    private long   contentSize;

    public String getLaunchTicket() {
      return launchTicket;
    }

    public Date getFirstRequest() {
      return firstRequest;
    }

    public Date getLastRequest() {
      return lastRequest;
    }

    public long getContentSize() {
      return contentSize;
    }

    @Override
    public String toString() {
      StringBuilder ret = new StringBuilder();
      ret.append("    launchTicket: ").append(launchTicket).append("\n");
      ret.append("    firstRequest: ").append(firstRequest).append("\n");
      ret.append("    lastRequest : ").append(lastRequest).append("\n");
      ret.append("    contentSize : ").append(contentSize).append("\n");
      return ret.toString();
    }
  }
}
