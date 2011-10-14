package no.ebakke.studycaster.backend;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import com.maxmind.geoip.regionName;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import no.ebakke.studycaster.servlets.LifeCycle;
import no.ebakke.studycaster.servlets.ServletUtil;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public final class BackendUtil {
  private BackendUtil() { }

  public static void storeRequest(SessionFactory sf, Request r) {
    Session s = sf.getCurrentSession();
    s.beginTransaction();
    s.save(r);
    // TODO: What about the error case?
    s.getTransaction().commit();
  }

  public static void addPassword(SessionFactory sf, String password) {
    Session s = sf.getCurrentSession();
    s.beginTransaction();
    // TODO: Add salt.
    s.save(new ConfigurationProperty("passwordHash",
        ServletUtil.toHex(ServletUtil.sha1(password))));
    s.getTransaction().commit();
  }

  public static boolean passwordMatches(SessionFactory sf, String password) {
    Session s = sf.getCurrentSession();
    String hashed = ServletUtil.toHex(ServletUtil.sha1(password));
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

  public static List<Request> getRequests(SessionFactory sf) {
    Session s = sf.getCurrentSession();
    // TODO: Should I be doing something else because this is a read-only transaction?
    s.beginTransaction();
    @SuppressWarnings("unchecked")
    List<Request> ret = (List<Request>) s.createQuery("from Request order by time, id").list();
    s.getTransaction().commit();
    return ret;
  }

  public static String getGeoInfo(HttpServletRequest req) {
    LookupService lookupService = LifeCycle.getBackend(req).getLookupService();
    if (lookupService == null)
      return null;
    Location loc = lookupService.getLocation(req.getRemoteAddr());
    if (loc == null)
      return null;
    return loc.countryName + " / " +
        regionName.regionNameByCode(loc.countryCode, loc.region);
  }
  
  public static boolean isAdminLoggedIn(HttpServletRequest req, String password) {
    Backend backend = LifeCycle.getBackend(req);
    // TODO: Remove obvious security hole here.
    if (!LifeCycle.getBackend(req).wasDBproperlyInitialized())
      return true;
    if (password != null) {
      setAdminLoggedIn(req, BackendUtil.passwordMatches(backend.getSessionFactory(), password));
    }
    HttpSession hs = req.getSession(false);
    if (hs == null)
      return false;
    synchronized (hs) {
      Object attr = hs.getAttribute("adminLoggedIn");
      if ((attr instanceof Boolean) && ((Boolean) attr))
        return true;
      return false;
    }
  }

  public static void setAdminLoggedIn(HttpServletRequest req, boolean loggedIn) {
    HttpSession hs = req.getSession(loggedIn);
    if (hs == null)
      return;
    synchronized (hs) {
      if (loggedIn) {
        hs.setAttribute("adminLoggedIn", loggedIn);
      } else {
        hs.invalidate();
      }
    }
  }
}
