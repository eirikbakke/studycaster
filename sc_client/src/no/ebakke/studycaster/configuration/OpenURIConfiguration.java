package no.ebakke.studycaster.configuration;

import java.net.URI;
import java.net.URISyntaxException;
import no.ebakke.studycaster.backend.StudyCasterException;
import org.w3c.dom.Element;

public class OpenURIConfiguration {
  private final URI uri;

  public OpenURIConfiguration(Element elm) throws StudyCasterException {
    try {
      uri = new URI(ConfigurationUtil.getTextContent(elm, "uri"));
    } catch (URISyntaxException e) {
      throw new StudyCasterException("Invalid URI in <openuri> action", e);
    }
  }

  public URI getURI() {
    return uri;
  }
}
