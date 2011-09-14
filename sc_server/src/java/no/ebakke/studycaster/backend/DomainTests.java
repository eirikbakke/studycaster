package no.ebakke.studycaster.backend;

import java.util.Date;

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

    // TODO: Should I rather used "managed" sessions and close them explicitly?
    DomainUtil.storeRequest(r1);
    DomainUtil.storeRequest(r2);
    DomainUtil.storeRequest(r3);

    System.out.println(DomainUtil.passwordMatches("This is a test."));
    System.out.println(DomainUtil.passwordMatches("This is a test2."));

    System.out.println("Loading requests:");
    for (Request r : DomainUtil.getRequests())
      System.out.println(r);
  }
}
