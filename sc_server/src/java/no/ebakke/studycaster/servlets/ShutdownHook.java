package no.ebakke.studycaster.servlets;

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
  }  
}
