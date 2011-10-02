package no.ebakke.studycaster.servlets;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequest;
import no.ebakke.studycaster.backend.Backend;
import org.hibernate.SessionFactory;

public class LifeCycle implements ServletContextListener {
  private static final Logger LOG          = Logger.getLogger("no.ebakke.studycaster");
  private static final String BACKEND_ATTR = "StudyCasterBackend";

  public static SessionFactory getSessionFactory(ServletRequest req) {
    return getBackend(req).getSessionFactory();
  }

  public static Backend getBackend(ServletRequest req) {
    return getBackend(req.getServletContext());
  }

  public static Backend getBackend(ServletContext context) {
    Object ret = context.getAttribute(BACKEND_ATTR);
    if (ret == null || !(ret instanceof Backend))
      throw new IllegalStateException("Backend context was not properly initialized");
    return (Backend) ret;
  }

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    /* TODO: Don't bundle JDBC drivers; require them to be installed on the server instead. */
    try {
      Class.forName("org.apache.derby.jdbc.ClientDriver");
      Class.forName("com.mysql.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      LOG.log(Level.SEVERE, "Failed to register bundled JDBC driver; {0}", e.getMessage());
    }

    ServletContext ctx = sce.getServletContext();
    synchronized (ctx) {
      if (ctx.getAttribute(BACKEND_ATTR) != null)
        throw new IllegalStateException("Backend context attempted initialized twice");
      ctx.setAttribute(BACKEND_ATTR, new Backend());
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    ServletContext ctx = sce.getServletContext();
    synchronized (ctx) {
      getBackend(ctx).close();
      ctx.removeAttribute(BACKEND_ATTR);
    }
    Enumeration<Driver> drivers = DriverManager.getDrivers();
    while (drivers.hasMoreElements()) {
      Driver driver = drivers.nextElement();
      try {
        if (driver instanceof org.apache.derby.jdbc.ClientDriver ||
            driver instanceof com.mysql.jdbc.Driver) {
          DriverManager.deregisterDriver(driver);
        }
      } catch (SQLException e) {
        LOG.log(Level.SEVERE, "Could not deregister JDBC driver; {0}", e.getMessage());
      }
    }

    // Avoid a benign Tomcat warning about a potential memory leak.
    for (Thread thread : Thread.getAllStackTraces().keySet()) {
      if (thread.getName().equals("Resource Destroyer in BasicResourcePool.close()")) {
        try {
          thread.join();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }  
}
