package no.ebakke.studycaster.domain;

import java.util.List;
import java.util.Properties;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public final class DomainUtil {
  public static final String JDBC_URL_PROPERTY = "JDBC_CONNECTION_STRING";
  private static final SessionFactory sessionFactory;
  private static final Throwable sessionFactoryError;
  private static final String jdbcURLinUse;

  static {
    SessionFactory tmpSessionFactory      = null;
    Throwable      tmpSessionFactoryError = null;
    String         tmpJDBCURLinUse;
    tmpJDBCURLinUse = System.getProperty(JDBC_URL_PROPERTY);
    jdbcURLinUse = (tmpJDBCURLinUse == null || tmpJDBCURLinUse.isEmpty())
        ? null : tmpJDBCURLinUse;
    try {
      /*if (jdbcURLinUse == null) {
        throw new HibernateException(
                "The " + JDBC_URL_PROPERTY + " system property is not set.");
      }*/
      tmpSessionFactory = DomainUtil.buildSessionFactory(jdbcURLinUse);
    } catch (Throwable e) {
      tmpSessionFactoryError = e;
    }
    sessionFactoryError = tmpSessionFactoryError;
    sessionFactory      = tmpSessionFactory;
  }

  private DomainUtil() { }

  public static String getJDBCURLinUse() {
    return jdbcURLinUse;
  }

  public static SessionFactory getSessionFactory() {
    if (sessionFactory == null) {
      throw new HibernateException("Hibernate was not properly initialized.",
          sessionFactoryError);
    }
    return sessionFactory;
  }

  private static SessionFactory buildSessionFactory(String jdbcURL)
      throws HibernateException
  {
    Configuration conf = new Configuration();
    /* Take base configuration from hibernate.cfg.xml . */
    conf.configure();
    if (jdbcURL != null) {
      Properties p = new Properties();
      p.setProperty("hibernate.connection.url", jdbcURL);
      conf.addProperties(p);
    }
    return conf.buildSessionFactory();
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
