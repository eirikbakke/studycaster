package no.ebakke.studycaster.configuration;

import no.ebakke.studycaster.api.StudyCasterException;
import org.w3c.dom.Element;

public class OpenFileConfiguration {
  private final String  serverName;
  private final String  clientName;
  private final String  errorMessage;

  public OpenFileConfiguration(Element elm) throws StudyCasterException {
    serverName   = ConfigurationUtil.getTextContent(elm, "servername"  );
    clientName   = ConfigurationUtil.getTextContent(elm, "clientname"  );
    // TODO: Change to getSwingCaption once entire message is used.
    errorMessage = ConfigurationUtil.getTextContent(elm, "errormessage");
  }

  // TODO: Rename to clientName/serverName.
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
