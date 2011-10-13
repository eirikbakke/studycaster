package no.ebakke.studycaster.configuration;

import no.ebakke.studycaster.api.StudyCasterException;
import org.w3c.dom.Element;

public class ConcludeConfiguration {
  private final UploadConfiguration uploadConfiguration;

  public ConcludeConfiguration(Element elm) throws StudyCasterException {
    Element uploadElm = ConfigurationUtil.getUniqueElement(elm, "uploadfile", true);
    uploadConfiguration = (uploadElm == null) ? null : new UploadConfiguration(uploadElm);
  }

  /** May be null. */
  public UploadConfiguration getUploadConfiguration() {
    return uploadConfiguration;
  }
}
