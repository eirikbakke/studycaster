package no.ebakke.studycaster.backend;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import com.maxmind.geoip.regionName;
import com.maxmind.geoip.timeZone;
import java.io.File;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import javax.servlet.ServletException;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.SessionFactoryImplementor;

public final class Backend {
  public  static final Backend INSTANCE = new Backend();
  private static final String HCU_KEY = "hibernate.connection.url";
  private static final String HDA_KEY = "hibernate.hbm2ddl.auto";
  private SessionFactory   sessionFactory;
  private BackendException sessionFactoryError;
  private File             storageDir;
  private BackendException storageDirError;
  private LookupService    lookupService;
  private BackendException lookupServiceError;

  public Backend() {
    this(new BackendConfiguration(), null);
  }

  public Backend(BackendConfiguration config, String createAndSetPassword) {
    try {
      sessionFactory = buildSessionFactory(config.getDatabaseURL(),
          createAndSetPassword);
    } catch (BackendException e) {
      sessionFactoryError = e;
    }
    try {
      storageDir = openStorageDir(config.getStorageDirPath());
    } catch (BackendException e) {
      storageDirError = e;
    }
    try {
      lookupService = initLookupService(config.getGeoIPDatabasePath());
    } catch (BackendException e) {
      lookupServiceError = e;
    }
  }

  private static File openStorageDir(String path) throws BackendException {
    File ret = new File(path);
    if (!ret.isDirectory())
      throw new BackendException("Not a directory");
    try {
      File.createTempFile("~permtest", "tmp", ret).delete();
    } catch (IOException e) {
      throw new BackendException("Access denied (" + e.getMessage() + ")");
    }
    return ret;
  }

  private static LookupService initLookupService(String path) throws BackendException {
    try {
      return new LookupService(path, LookupService.GEOIP_MEMORY_CACHE);
    } catch (IOException e) {
      throw new BackendException("Failed to open GeoIP database", e);
    }
  }

  // TODO: Move this presentation code out of here.
  public String getStatusMessage() {
    String ret = "";
    ret += "JDBC Database: ";
    if (sessionFactoryError != null) {
      String msg;
      Throwable cause = sessionFactoryError;
      do {
        msg = cause.getMessage();
        if (!msg.contains("see the next exception for details"))
          break;
        cause = cause.getCause();
      } while (cause != null);
      ret += msg;
    } else {
      // See http://stackoverflow.com/questions/1571928/retrieve-auto-detected-hibernate-dialect .
      if (sessionFactory instanceof SessionFactoryImplementor) {
        ret += "OK, " +
          ((SessionFactoryImplementor) sessionFactory).getDialect().toString();
      } else {
        ret += "OK, " + sessionFactory.toString();
      }
    }
    ret += "\n";
    ret += "Storage Directory: ";
    if (storageDirError != null) {
      ret += storageDirError.getMessage();
    } else {
      ret += "OK";
    }
    ret += "\n";
    ret += "GeoIP Database: ";
    if (lookupServiceError != null) {
      ret += lookupServiceError.getMessage();
    } else {
      ret += "OK, updated " + lookupService.getDatabaseInfo().getDate();
    }
    return ret;
  }

  public boolean wasDBproperlyInitialized() {
    return (sessionFactoryError == null);
  }

  public SessionFactory getSessionFactory() throws HibernateException {
    if (sessionFactoryError != null) {
      throw new HibernateException("Backend was not properly initialized",
          sessionFactoryError);
    }
    return sessionFactory;
  }

  public File getStorageDirectory() throws ServletException {
    if (storageDirError != null) {
      throw new ServletException("Backend was not properly initialized",
          storageDirError);
    }
    return storageDir;
  }

  public LookupService getLookupService() {
    return lookupService;
  }

  /** If connectionURL is null, take from environment. */
  private static SessionFactory buildSessionFactory(
      String connectionURL, String createAndSetPassword) throws BackendException
  {
    try {
      Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
      Class.forName("org.apache.derby.jdbc.ClientDriver");
      Class.forName("com.mysql.jdbc.Driver");

      Configuration conf = createHibernateConfiguration(connectionURL,
          createAndSetPassword != null);
      /* Try with regular JDBC first, as it gives better error messages for
      invalid URL parameters. */
      DriverManager.getConnection(conf.getProperty(HCU_KEY)).close();
      SessionFactory ret = conf.buildSessionFactory();

      if (createAndSetPassword != null) {
        BackendUtil.addPassword(ret, createAndSetPassword);
        /* Hibernate may let schema creation fail silently, so reconnect in
        validate mode to be sure the operation succeeded. Aforementioned
        behavior observed when attempting to create a schema with which the
        specified user has read-only access. */
        ret.close();
        try {
          ret = buildSessionFactory(connectionURL, null);
        } catch (BackendException e) {
          throw new BackendException("Validation failed at reconnect", e);
        }
      }
      return ret;
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
    ret.configure("hibernate.cfg.xml");
    Properties p = new Properties();
    p.setProperty(HCU_KEY, connectionURL);
    if (ret.getProperty(HDA_KEY) != null) {
      throw new BackendException(
          "Hibernate configuration may not set " + HDA_KEY); 
    }
    p.setProperty(HDA_KEY, create ? "create" : "validate");
    ret.addProperties(p);
    return ret;
  }

  public static void main(String[] args) throws IOException {
    LookupService cl = new LookupService("Z:/sc_remote/storage/GeoLiteCity.dat",
        LookupService.GEOIP_MEMORY_CACHE);
    Location l1 = cl.getLocation("213.52.50.8");
    Location l2 = cl.getLocation("128.30.5.63");
    System.out.println("countryCode: " + l2.countryCode
        + "\n countryName: " + l2.countryName
        + "\n region: " + l2.region
        + "\n regionName: " + regionName.regionNameByCode(l2.countryCode, l2.region)
        + "\n city: " + l2.city
        + "\n postalCode: " + l2.postalCode
        + "\n latitude: " + l2.latitude
        + "\n longitude: " + l2.longitude
        + "\n distance: " + l2.distance(l1)
        + "\n distance: " + l1.distance(l2)
        + "\n metro code: " + l2.metro_code
        + "\n area code: " + l2.area_code
        + "\n timezone: " + timeZone.timeZoneByCountryAndRegion(l2.countryCode, l2.region));

    cl.close();
  }
}
