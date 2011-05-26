package no.ebakke.studycaster.applications;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.ebakke.studycaster.api.StudyConfiguration;
import no.ebakke.studycaster.api.ServerContext;
import no.ebakke.studycaster.api.StudyCasterException;

public class ConfigurableStudyCaster {
  public static Logger log = Logger.getLogger("no.ebakke.studycaster");
  private StudyConfiguration configuration;
  private ServerContext serverContext;

  public ConfigurableStudyCaster(String configurationFileName) throws StudyCasterException {
    serverContext = new ServerContext();
    try {
      configuration = new StudyConfiguration(serverContext.downloadFile(configurationFileName));
    } catch (IOException e) {
      throw new StudyCasterException("Error retrieving configuration file", e);
    }
  }

  public static void main(String args[]) {
    ConfigurableStudyCaster csc;
    URI uri;

    if (args.length != 1) {
      System.err.println("Usage: ConfigurateStudyCaster <remote configuration file name>");
      return;
    }
    try {
      csc = new ConfigurableStudyCaster(args[0]);
    } catch (StudyCasterException e) {
      log.log(Level.SEVERE, "Failed to initialize StudyCaster", e);
      return;
    }
  }
}
