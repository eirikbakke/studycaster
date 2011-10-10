package no.ebakke.studycaster.configuration;

import java.util.List;
import no.ebakke.studycaster.api.StudyCasterException;
import org.w3c.dom.Element;

public class PageConfiguration {
  private final String                instructions;
  private final OpenFileConfiguration openFileConfiguration;

  public PageConfiguration(Element elm) throws StudyCasterException {
    instructions = ConfigurationUtil.getSwingCaption(elm, "instructions");
    Element openFileElm = ConfigurationUtil.getUniqueElement(elm, "openfile", true);
    openFileConfiguration = (openFileElm == null) ? null : new OpenFileConfiguration(openFileElm);
  }

  public static List<PageConfiguration> parse(Element parent) throws StudyCasterException {
    return ConfigurationUtil.parseElements(ConfigurationUtil.getElements(parent, "page", true),
        new ConfigurationUtil.ElementParser<PageConfiguration>()
    {
      public PageConfiguration parseElement(Element elm) throws StudyCasterException {
        return new PageConfiguration(elm);
      }
    });
  }

  public String getInstructions() {
    return instructions;
  }

  public OpenFileConfiguration getOpenFileConfiguration() {
    return openFileConfiguration;
  }
}
