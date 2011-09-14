package no.ebakke.studycaster.backend;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import com.maxmind.geoip.regionName;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import no.ebakke.studycaster.servlets.ServletUtil;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public final class BackendUtil {
  private BackendUtil() { }

  public static void storeRequest(Request r) {
    // TODO: Should I not be calling getCurrentSession() every time?
    // TODO: Should I rather used "managed" sessions and close them explicitly?
    Session s = Backend.INSTANCE.getSessionFactory().getCurrentSession();
    s.beginTransaction();
    s.save(r);
    // TODO: What about the error case?
    s.getTransaction().commit();
  }

  public static void addPassword(SessionFactory sf, String password) {
    Session s = sf.getCurrentSession();
    s.beginTransaction();
    // TODO: Add salt.
    // TODO: Don't abuse the Ticket class for this.
    s.save(new ConfigurationProperty("passwordHash",
        ServletUtil.toHex(ServletUtil.sha1(password))));
    s.getTransaction().commit();
  }

  public static boolean passwordMatches(String password) {
    String hashed = ServletUtil.toHex(ServletUtil.sha1(password));
    Session s = Backend.INSTANCE.getSessionFactory().getCurrentSession();
    s.beginTransaction();
    Query q = s.createQuery("from ConfigurationProperty where key=?");
    q.setString(0, "passwordHash");
    @SuppressWarnings("unchecked")
    List<ConfigurationProperty> pairs = (List<ConfigurationProperty>) q.list();
    boolean ret = false;
    // TODO: Balk on multiple occurences of single property, or no occurences.
    for (ConfigurationProperty pair : pairs) {
      if (pair.getValue().equals(hashed)) {
        ret = true;
        break;
      }
    }
    s.getTransaction().commit();
    return ret;
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

  public static String getGeoInfo(HttpServletRequest req) {
    LookupService lookupService = Backend.INSTANCE.getLookupService();
    if (lookupService == null)
      return null;
    Location loc = lookupService.getLocation(req.getRemoteAddr());
    if (loc == null)
      return null;
    return loc.countryName + " / " +
        regionName.regionNameByCode(loc.countryCode, loc.region);
  }
}
