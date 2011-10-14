package no.ebakke.studycaster.reporting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import no.ebakke.studycaster.backend.Backend;
import no.ebakke.studycaster.backend.BackendUtil;
import no.ebakke.studycaster.backend.Request;
import no.ebakke.studycaster.servlets.ServletUtil;
import no.ebakke.studycaster.util.ColUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.SessionFactory;

public final class Reports {
  private Reports() { }

  public static List<Subject> getSubjectReport(SessionFactory sf) {
    // Each key is a launchTicket.
    Map<String,Date> firstRequest = ColUtil.newOrderedMap();
    Map<String,Date> lastRequest  = ColUtil.newOrderedMap();
    Map<String,Long> numRequests  = ColUtil.newOrderedMap();
    Map<String,Long> contentSize  = ColUtil.newOrderedMap();
    Map<String,Date> firstAddrRequest = ColUtil.newOrderedMap();

    Linker<Request> linker = new Linker<Request>();
    // TODO: Only get DISTINCT the fields we care about for correlation.
    for (Request r : BackendUtil.getRequests(sf)) {
      Map<Pair<String,Boolean>,String> links = ColUtil.newOrderedMap();
      links.put(Pair.of("remoteAddrHash", true ), r.getRemoteAddrHash());
      links.put(Pair.of("clientCookie"  , true ), r.getClientCookie());
      links.put(Pair.of("launchTicket"  , true ), r.getLaunchTicket());
      links.put(Pair.of("geoLocation"   , false), r.getGeoLocation());
      if ("jws".equals(r.getType()))
        links.put(Pair.of("versionString"  , false), r.getLogEntry());
      if ("cid".equals(r.getType()))
        links.put(Pair.of("configurationID", false), r.getLogEntry());
      linker.addLink(links, r);
      ColUtil.putExtreme(firstRequest, r.getLaunchTicket(), r.getTime(), false);
      ColUtil.putExtreme(lastRequest , r.getLaunchTicket(), r.getTime(), true );
      ColUtil.putSum(numRequests, r.getLaunchTicket(), 1L);
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
      subject.firstRequest    = firstOverall;
      subject.clientCookie    = node.getLinks("clientCookie");
      subject.remoteAddrHash  = node.getLinks("remoteAddrHash");
      subject.geoLocation     = node.getLinks("geoLocation");
      subject.versionString   = node.getLinks("versionString");
      // TODO: Associate this with launches instead.
      subject.configurationID = node.getLinks("configurationID");
      subject.launches        = ColUtil.newList();
      for (String launchTicket : node.getLinks("launchTicket")) {
        Launch launch = new Launch();
        launch.launchTicket = launchTicket;
        launch.firstRequest = firstRequest.get(launchTicket);
        launch.lastRequest  = lastRequest.get(launchTicket);
        launch.numRequests  = numRequests.get(launchTicket);
        // TODO: Get rid of the KB presentation detail.
        // Round contentSize up to avoid showing zero kilobytes for non-zero values.
        if (contentSize.get(launchTicket) != null)
          launch.contentSize  = (int) Math.ceil(((double) contentSize.get(launchTicket)) / 1024.0);
        subject.launches.add(launch);
      }
      Collections.sort(subject.launches);
      ret.add(subject);
    }
    Collections.sort(ret);
    return ret;
  }

  public static void main(String args[]) {
    Backend backend = new Backend();
    for (Subject s : getSubjectReport(backend.getSessionFactory()))
      System.out.println(s);
    backend.close();
  }

  public static class Subject implements Comparable<Subject> {
    private Date         firstRequest;
    private Set<String>  clientCookie;
    private Set<String>  remoteAddrHash;
    private Set<String>  geoLocation;
    private Set<String>  versionString;
    private Set<String>  configurationID;
    private List<Launch> launches;

    public String getFirstRequest() {
      return ServletUtil.getServerDateFormat().format(firstRequest);
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

    public Set<String> getVersionString() {
      return versionString;
    }

    public Set<String> getConfigurationID() {
      return configurationID;
    }

    public List<Launch> getLaunches() {
      return launches;
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

    @Override
    public int compareTo(Subject o) {
      return firstRequest.compareTo(o.firstRequest);
    }
  }

  // TODO: Move presentation details out of here.
  public static class Launch implements Comparable<Launch> {
    private String launchTicket;
    private Date   firstRequest;
    private Date   lastRequest;
    private long   numRequests;
    private long   contentSize;

    public String getLaunchTicket() {
      return launchTicket;
    }

    public String getFirstRequest() {
      return ServletUtil.getServerDateFormat().format(firstRequest);
    }

    public String getLastRequest() {
      return ServletUtil.getServerDateFormat().format(lastRequest);
    }

    public long getNumRequests() {
      return numRequests;
    }

    public long getContentSize() {
      return contentSize;
    }

    public boolean isLastRequestAfter(long secondsAgo) {
      return lastRequest.after(
        new Date(new Date().getTime() - secondsAgo * 1000L));
    }

    public String getTimeSinceLastRequest() {
      return ServletUtil.humanReadableInterval(
          new Date(new Date().getTime() - lastRequest.getTime()).getTime() / 1000);
    }

    public String getTotalDuration() {
      return ServletUtil.humanReadableInterval(
          new Date(lastRequest.getTime() - firstRequest.getTime()).getTime() / 1000);
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

    @Override
    public int compareTo(Launch o) {
      return firstRequest.compareTo(o.firstRequest);
    }
  }
}
