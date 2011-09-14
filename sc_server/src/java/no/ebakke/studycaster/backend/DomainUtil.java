package no.ebakke.studycaster.backend;

import java.util.List;
import org.hibernate.Session;

public final class DomainUtil {
  private DomainUtil() { }

  public static void storeRequest(Request r) {
    // TODO: Should I not be calling getCurrentSession() every time?
    // TODO: Should I rather used "managed" sessions and close them explicitly?
    Session s = Backend.INSTANCE.getSessionFactory().getCurrentSession();
    s.beginTransaction();
    s.save(r);
    // TODO: What about the error case?
    s.getTransaction().commit();
  }

  public static List<Request> getRequests() {
    Session s = Backend.INSTANCE.getSessionFactory().getCurrentSession();
    s.beginTransaction();
    @SuppressWarnings("unchecked")
    List<Request> ret = (List<Request>) s.createQuery("from Request").list();
    // TODO: Do I need this for read-only queries?
    s.getTransaction().commit();
    return ret;
  }
}
