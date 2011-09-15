package no.ebakke.studycaster.servlets;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import no.ebakke.studycaster.backend.Backend;

public class ShutdownHook implements ServletContextListener {
  @Override
  public void contextInitialized(ServletContextEvent sce) {
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    System.err.println("Closing backend");
    Backend.INSTANCE.close();
    // See http://stackoverflow.com/questions/3320400/jdbc-driver-unregisted-when-the-web-application-stops
    Enumeration<Driver> drivers = DriverManager.getDrivers();
    while (drivers.hasMoreElements()) {
      Driver driver = drivers.nextElement();
      try {
        DriverManager.deregisterDriver(driver);
      } catch (SQLException e) {
        // TODO: Do proper logging.
        e.printStackTrace();
      }
    }
  }  
}
