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
    List<Subject> ret = new ArrayList<Subject>();

    Linker<Request> linker = new Linker<Request>();
    // TODO: Only get DISTINCT the fields we care about for correlation.
    for (Request r : BackendUtil.getRequests()) {
      Map<Pair<String,Boolean>,String> links = ColUtil.newOrderedMap();
      links.put(Pair.of("remoteAddrHash", true ), r.getRemoteAddrHash());
      links.put(Pair.of("clientCookie"  , true ), r.getClientCookie());
      links.put(Pair.of("launchTicket"  , true ), r.getLaunchTicket());
      links.put(Pair.of("geoLocation"   , false), r.getGeoLocation());
      linker.addLink(links, r);
    }
    for (Linker.Node<Request> node : linker.getContent()) {
      System.out.println(node);
    }
    /*
    for (List<Request> list : linker.getContent()) {
      System.out.println("****** New node: ******");
      for (Request r : list) {
        System.out.println(r);
      }
    }*/

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
