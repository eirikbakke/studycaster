package no.ebakke.studycaster.backend;

import java.util.Date;
import org.hibernate.SessionFactory;

public final class DomainTests {
  private DomainTests() { }

  @SuppressWarnings({"unchecked", "unchecked"})
  public static void main(String args[]) {
    Request r1 = new Request(new Date(), "testReq", 42L, "someRemoteHash", "someGeoLoc", "someLaunchTicket", "someClientCookie", "A log entry!");
    Request r2 = new Request(new Date(), "testReq", null, null, null, null, null, "Another log entry!");
    Request r3 = new Request(new Date(), "testReq2", 65000L, null, null, null, null, "And a last one.");
    System.out.println(r1);
    System.out.println(r2);
    System.out.println(r3);

    Backend backend = new Backend();
    SessionFactory sf = backend.getSessionFactory();

    //BackendUtil.storeRequest(s, r1);
    //BackendUtil.storeRequest(s, r2);
    //BackendUtil.storeRequest(s, r3);

    System.out.println(BackendUtil.passwordMatches(sf, "This is a test."));
    System.out.println(BackendUtil.passwordMatches(sf, "This is a test2."));

    System.out.println("Loading requests:");
    for (Request r : BackendUtil.getRequests(sf))
      System.out.println(r);

    backend.close();
  }
}
