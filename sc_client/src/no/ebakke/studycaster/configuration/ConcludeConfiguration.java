package no.ebakke.studycaster.configuration;

import java.util.Map;
import no.ebakke.studycaster.backend.StudyCasterException;
import org.w3c.dom.Element;

public class ConcludeConfiguration {
  private final UploadConfiguration uploadConfiguration;

  public ConcludeConfiguration(
      Map<String,OpenFileConfiguration> openFileConfigurations, Element elm)
      throws StudyCasterException
  {
    Element uploadElm = ConfigurationUtil.getUniqueElement(elm, "uploadfile", true);
    uploadConfiguration = (uploadElm == null) ? null :
        new UploadConfiguration(openFileConfigurations, uploadElm);
  }

  /** May be null. */
  public UploadConfiguration getUploadConfiguration() {
    return uploadConfiguration;
  }
}
