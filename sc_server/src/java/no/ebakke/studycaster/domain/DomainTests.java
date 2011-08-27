package no.ebakke.studycaster.domain;

import java.util.Date;
import java.util.List;
import org.hibernate.Session;

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
    Session s = HibernateUtil.getSessionFactory().getCurrentSession();
    s.beginTransaction();
    s.save(r1);
    s.save(r2);
    s.save(r3);
    s.getTransaction().commit();

    System.out.println("Loading requests:");
    s = HibernateUtil.getSessionFactory().getCurrentSession();
    s.beginTransaction();

    for (Request r : (List<Request>) s.createQuery("from Request").list()) {
      System.out.println(r);
    }

    s.getTransaction().commit();
  }
}