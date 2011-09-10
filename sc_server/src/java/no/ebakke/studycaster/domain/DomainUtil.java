package no.ebakke.studycaster.domain;

import java.util.List;
import java.util.Properties;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public final class DomainUtil {
  public  static final String JDBC_URL_PROPERTY = "JDBC_CONNECTION_STRING";
  private static final String HCU_KEY = "hibernate.connection.url";
  private static final String HDA_KEY = "hbm2ddl.auto";
  private static final SessionFactory sessionFactory;
  private static final Configuration  configurationInUse;
  private static final Throwable      sessionFactoryError;

  static {
    SessionFactory tmpSessionFactory      = null;
    Throwable      tmpSessionFactoryError = null;
    Configuration  tmpConfigurationInUse  = null;
    try {
      tmpConfigurationInUse = createHibernateConfiguration(null, false);
      tmpSessionFactory = tmpConfigurationInUse.buildSessionFactory();
    } catch (Throwable e) {
      e.printStackTrace();
      tmpSessionFactoryError = e;
    }
    sessionFactoryError = tmpSessionFactoryError;
    sessionFactory      = tmpSessionFactory;
    configurationInUse  = tmpConfigurationInUse;
  }

  private DomainUtil() { }

  public static String getConnectionURLinUse() {
    if (configurationInUse == null)
      return null;
    Object ret = configurationInUse.getProperty(HCU_KEY);
    if (ret == null)
      return null;
    return ret.toString();
  }

  public static SessionFactory getSessionFactory() {
    if (sessionFactoryError != null) {
      throw new HibernateException("Hibernate was not properly initialized.",
          sessionFactoryError);
    }
    return sessionFactory;
  }

  /** If connectionURL is null, take from environment. */
  private static Configuration createHibernateConfiguration(
        String connectionURL, boolean create)
  {
    Configuration ret = new Configuration();
    String setConnectionURL;
    if (connectionURL != null) {
      setConnectionURL = connectionURL;
    } else {
      try {
        ret.configure();
        System.out.println("CONFIGURE SUCCESS");
      } catch (HibernateException e) {
        /* Normal case for production; hibernate.cfg.xml is only used in
        development. */
        System.out.println("CONFIGURE FAIL");
      }
      String prop = System.getProperty(JDBC_URL_PROPERTY);
      if (ret.getProperties().containsKey(HCU_KEY)) {
        setConnectionURL = null;
        // TODO: Use logger instead.
        if (prop != null) {
          System.err.println("Warning: " + HCU_KEY + " was already defined; " +
              "ignoring " + JDBC_URL_PROPERTY + " environment variable.");
        }
      } else {
        setConnectionURL = prop;
      }
    }
    ret.configure("hibernate_common.cfg.xml");
    Properties p = new Properties();
    if (setConnectionURL != null)
      p.setProperty(HCU_KEY, setConnectionURL);
    if (ret.getProperties().contains(HDA_KEY))
      throw new HibernateException("Hibernate configuration may not set " + HDA_KEY);
    p.setProperty(HDA_KEY, create ? "create" : "validate");
    ret.addProperties(p);
    return ret;
  }

  public static void storeRequest(Request r) {
    // TODO: Should I not be calling getCurrentSession() every time?
    // TODO: Should I rather used "managed" sessions and close them explicitly?
    Session s = getSessionFactory().getCurrentSession();
    s.beginTransaction();
    s.save(r);
    // TODO: What about the error case?
    s.getTransaction().commit();
  }

  public static List<Request> getRequests() {
    Session s = getSessionFactory().getCurrentSession();
    s.beginTransaction();
    @SuppressWarnings("unchecked")
    List<Request> ret = (List<Request>) s.createQuery("from Request").list();
    // TODO: Do I need this for read-only queries?
    s.getTransaction().commit();
    return ret;
  }
}
