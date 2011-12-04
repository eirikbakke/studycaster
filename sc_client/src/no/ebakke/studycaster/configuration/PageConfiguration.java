package no.ebakke.studycaster.configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import no.ebakke.studycaster.api.StudyCasterException;
import org.w3c.dom.Element;

public class PageConfiguration {
  private final String                instructions;
  private final OpenFileConfiguration openFileConfiguration;
  private final ConcludeConfiguration concludeConfiguration;
  private final OpenURIConfiguration  openURIConfiguration;

  public PageConfiguration(Map<String,OpenFileConfiguration> openFileConfigurations, Element elm)
      throws StudyCasterException
  {
    instructions = ConfigurationUtil.getSwingCaption(elm, "instructions");
    Element openFileElm = ConfigurationUtil.getUniqueElement(elm, "openfile", true);
    openFileConfiguration = (openFileElm == null) ? null : new OpenFileConfiguration(openFileElm);
    if (openFileConfiguration != null)
      openFileConfigurations.put(openFileConfiguration.getClientName(), openFileConfiguration);
    Element openURIElm = ConfigurationUtil.getUniqueElement(elm, "openuri", true);
    openURIConfiguration = (openURIElm == null) ? null : new OpenURIConfiguration(openURIElm);
    Element concludeElm = ConfigurationUtil.getUniqueElement(elm, "conclude", true);
    concludeConfiguration = (concludeElm == null) ? null : new ConcludeConfiguration(
        openFileConfigurations, concludeElm);
  }

  public static List<PageConfiguration> parse(Element parent) throws StudyCasterException {
    final Map<String,OpenFileConfiguration> openFileConfigurations =
        new LinkedHashMap<String,OpenFileConfiguration>();

    return ConfigurationUtil.parseElements(ConfigurationUtil.getElements(parent, "page", true),
        new ConfigurationUtil.ElementParser<PageConfiguration>()
    {
      public PageConfiguration parseElement(Element elm) throws StudyCasterException {
        return new PageConfiguration(openFileConfigurations, elm);
      }
    });
  }

  public String getInstructions() {
    return instructions;
  }

  /** May be null. */
  public OpenFileConfiguration getOpenFileConfiguration() {
    return openFileConfiguration;
  }

  /** May be null. */
  public OpenURIConfiguration getOpenURIConfiguration() {
    return openURIConfiguration;
  }

  /** May be null. */
  public ConcludeConfiguration getConcludeConfiguration() {
    return concludeConfiguration;
  }
}
