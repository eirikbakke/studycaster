package no.ebakke.studycaster.domain;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.SessionFactoryImplementor;

public final class DomainUtil {
  public  static final String JDBC_URL_PROPERTY = "JDBC_CONNECTION_STRING";
  private static final String HCU_KEY = "hibernate.connection.url";
  private static final String HDA_KEY = "hibernate.hbm2ddl.auto";
  private static final SessionFactory   sessionFactory;
  private static final BackendException sessionFactoryError;

  static {
    SessionFactory   tmpSessionFactory      = null;
    BackendException tmpSessionFactoryError = null;
    try {
      tmpSessionFactory = buildSessionFactory(null, false);
    } catch (BackendException e) {
      tmpSessionFactoryError = e;
    }
    sessionFactoryError = tmpSessionFactoryError;
    sessionFactory      = tmpSessionFactory;
  }

  private DomainUtil() { }

  public static String getStatusMessage(
    SessionFactory sessionFactory, BackendException exception)
  {
    String ret;
    if (exception == null) {
      // See http://stackoverflow.com/questions/1571928/retrieve-auto-detected-hibernate-dialect .
      if (sessionFactory instanceof SessionFactoryImplementor) {
        ret = "Successful connection, dialect=" +
          ((SessionFactoryImplementor) sessionFactory).getDialect().toString();
      } else {
        ret = "Successful connection, sessionFactory.toString()";
      }
    } else {
      Throwable cause = exception;
      do {
        ret = cause.getMessage();
        if (!ret.contains("see the next exception for details"))
          break;
        cause = cause.getCause();
      } while (cause != null);
    }
    return ret;
  }

  public static String getStatus() {
    return getStatusMessage(sessionFactory, sessionFactoryError);
  }

  public static SessionFactory getSessionFactory() throws HibernateException {
    if (sessionFactoryError != null) {
      throw new HibernateException("Backend was not properly initialized.",
          sessionFactoryError);
    }
    return sessionFactory;
  }

  /** If connectionURL is null, take from environment. */
  public static SessionFactory buildSessionFactory(
      String connectionURL, boolean create) throws BackendException
  {
    try {
      Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
      Class.forName("com.mysql.jdbc.Driver");

      Configuration conf = createHibernateConfiguration(connectionURL, create);
      /* Try with regular JDBC first, as it gives better error messages for
      invalid URL parameters. */
      DriverManager.getConnection(conf.getProperty(HCU_KEY)).close();
      SessionFactory ret = conf.buildSessionFactory();
      if (create) {
        /* Hibernate may let schema creation fail silently, so reconnect in
        validate mode to be sure the operation succeeded. Aforementioned
        behavior observed when attempting to create a schema with which the
        specified user has read-only access. */
        ret.close();
        try {
          return buildSessionFactory(connectionURL, false);
        } catch (BackendException e) {
          throw new BackendException("Validation failed at reconnect", e);
        }
      } else {
        return ret;
      }
    } catch (ClassNotFoundException e) {
      throw new BackendException(e);
    } catch (SQLException e) {
      throw new BackendException(e);
    } catch (HibernateException e) {
      throw new BackendException(e);
    }
  }

  /** If connectionURL is null, take from environment. */
  private static Configuration createHibernateConfiguration(
    String connectionURL, boolean create)
    throws BackendException, HibernateException
  {
    Configuration ret = new Configuration();
    String setConnectionURL;
    if (connectionURL != null) {
      setConnectionURL = connectionURL;
    } else {
      try {
        ret.configure();
      } catch (HibernateException e) {
        /* Normal case for production; hibernate.cfg.xml is only used in
        development. */
      }
      String prop = System.getProperty(JDBC_URL_PROPERTY);
      if (ret.getProperty(HCU_KEY) != null) {
        setConnectionURL = null;
        // TODO: Use logger instead.
        if (prop != null) {
          System.err.println("Warning: " + HCU_KEY + " was already defined; " +
              "ignoring " + JDBC_URL_PROPERTY + " environment variable.");
        }
      } else {
        if (prop == null)
          throw new BackendException(JDBC_URL_PROPERTY + " not set.");
        setConnectionURL = prop;
      }
    }
    ret.configure("hibernate_common.cfg.xml");
    Properties p = new Properties();
    if (setConnectionURL != null)
      p.setProperty(HCU_KEY, setConnectionURL);
    if (ret.getProperty(HDA_KEY) != null) {
      throw new BackendException(
          "Hibernate configuration may not set " + HDA_KEY); 
    }
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
