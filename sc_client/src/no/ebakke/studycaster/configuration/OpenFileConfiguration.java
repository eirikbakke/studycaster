package no.ebakke.studycaster.configuration;

import no.ebakke.studycaster.backend.StudyCasterException;
import org.w3c.dom.Element;

public class OpenFileConfiguration {
  private final String  serverName;
  private final String  clientName;
  private final String  errorMessage;

  public OpenFileConfiguration(Element elm) throws StudyCasterException {
    serverName   = ConfigurationUtil.getTextContent(elm, "servername"  );
    clientName   = ConfigurationUtil.getTextContent(elm, "clientname"  );
    errorMessage = ConfigurationUtil.getTextContent(elm, "errormessage");
  }

  public String getServerName() {
    return serverName;
  }

  public String getClientName() {
    return clientName;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
