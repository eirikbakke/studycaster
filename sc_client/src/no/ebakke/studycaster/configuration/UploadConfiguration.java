package no.ebakke.studycaster.configuration;

import java.util.Map;
import javax.swing.filechooser.FileFilter;
import no.ebakke.studycaster.api.StudyCasterException;
import no.ebakke.studycaster.util.MyFileNameExtensionFilter;
import org.w3c.dom.Element;

public class UploadConfiguration {
  private final OpenFileConfiguration defaultFile;
  private final FileFilter fileFilter;
  private final String     unchangedWarning;

  public UploadConfiguration(Map<String,OpenFileConfiguration> openFileConfigurations, Element elm)
      throws StudyCasterException
  {
    Element fileFilterElm = ConfigurationUtil.getUniqueElement(elm, "filefilter");
    fileFilter = new MyFileNameExtensionFilter(
        ConfigurationUtil.getStrings(fileFilterElm, "extension"), ConfigurationUtil.getTextContent(
        ConfigurationUtil.getUniqueElement(fileFilterElm, "description")));
    Element defaultNameElm = ConfigurationUtil.getUniqueElement(elm, "default", true);
    if (defaultNameElm == null) {
      defaultFile = null;
    } else {
      String defaultFileName = ConfigurationUtil.getTextContent(defaultNameElm);
      defaultFile = openFileConfigurations.get(defaultFileName);
      if (defaultFile == null) {
        throw new StudyCasterException(
            "Upload configuration referenced non-existing open configuration for file \"" +
            defaultFileName + "\"");
      }
    }
    Element unchangedWarningElm = ConfigurationUtil.getUniqueElement(elm, "unchangedwarning", true);
    unchangedWarning = unchangedWarningElm == null ? null :
        ConfigurationUtil.getSwingCaption(unchangedWarningElm);
  }

  public FileFilter getFileFilter() {
    return fileFilter;
  }

  /** May be null. */
  public OpenFileConfiguration getDefaultFile() {
    return defaultFile;
  }

  /** May be null. */
  public String getUnchangedWarning() {
    return unchangedWarning;
  }
}
