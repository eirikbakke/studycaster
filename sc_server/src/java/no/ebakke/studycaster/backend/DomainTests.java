package no.ebakke.studycaster.backend;

import java.util.Date;

public final class DomainTests {
  private DomainTests() { }

  @SuppressWarnings({"unchecked", "unchecked"})
  public static void main(String args[]) {
    Request r1 = new Request(new Date(), "testReq");
    Request r2 = new Request(new Date(), "testReq");
    Request r3 = new Request(new Date(), "testReq2");
    System.out.println(r1);
    System.out.println(r2);
    System.out.println(r3);

    // TODO: Should I rather used "managed" sessions and close them explicitly?
    DomainUtil.storeRequest(r1);
    DomainUtil.storeRequest(r2);
    DomainUtil.storeRequest(r3);

    System.out.println("Loading requests:");
    for (Request r : DomainUtil.getRequests())
      System.out.println(r);
  }
}
