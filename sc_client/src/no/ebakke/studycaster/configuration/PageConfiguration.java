package no.ebakke.studycaster.configuration;

import no.ebakke.studycaster.api.StudyCasterException;
import org.w3c.dom.Element;

public class PageConfiguration {
  private String                instructions;
  private OpenFileConfiguration openFileConfiguration;

  public PageConfiguration(Element elm) throws StudyCasterException {
    instructions = ConfigurationUtil.getSwingCaption(elm, "instructions");
    Element openFileElm = ConfigurationUtil.getUniqueElement(elm, "openFile", true);
    if (openFileElm != null)
      openFileConfiguration = new OpenFileConfiguration(openFileElm);
  }

  public String getInstructions() {
    return instructions;
  }

  public OpenFileConfiguration getopenFileConfiguration() {
    return openFileConfiguration;
  }
}
