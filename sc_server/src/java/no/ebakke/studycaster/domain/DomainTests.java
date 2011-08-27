package no.ebakke.studycaster.domain;

import java.util.Date;
import org.hibernate.Session;

public final class DomainTests {
  private DomainTests() { }

  public static void main(String args[]) {
    Request r1 = new Request(new Date(), "testReq");
    Request r2 = new Request(new Date(), "testReq");
    Request r3 = new Request(new Date(), "testReq2");
    System.out.println(r1);
    System.out.println(r2);
    System.out.println(r3);
    
    Session s = HibernateUtil.getSessionFactory().getCurrentSession();
    s.beginTransaction();
    s.save(r1);
    s.save(r2);
    s.save(r3);
    s.getTransaction().commit();
  }
}
