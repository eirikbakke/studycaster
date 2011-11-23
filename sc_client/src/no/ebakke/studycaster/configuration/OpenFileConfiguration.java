package no.ebakke.studycaster.configuration;

import no.ebakke.studycaster.api.StudyCasterException;
import org.w3c.dom.Element;

public class OpenFileConfiguration {
  private final String  remoteName;
  private final String  localName;
  private final String  errorMessage;

  public OpenFileConfiguration(Element elm) throws StudyCasterException {
    remoteName   = ConfigurationUtil.getTextContent(elm, "remotename"  );
    localName    = ConfigurationUtil.getTextContent(elm, "localname"   );
    // TODO: Change to getSwingCaption once entire message is used.
    errorMessage = ConfigurationUtil.getTextContent(elm, "errormessage");
  }

  public String getRemoteName() {
    return remoteName;
  }

  public String getLocalName() {
    return localName;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
