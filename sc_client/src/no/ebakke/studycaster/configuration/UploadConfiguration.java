package no.ebakke.studycaster.configuration;

import javax.swing.filechooser.FileFilter;
import no.ebakke.studycaster.api.StudyCasterException;
import no.ebakke.studycaster.util.MyFileNameExtensionFilter;
import org.w3c.dom.Element;

public class UploadConfiguration {
  private final FileFilter fileFilter;
  private final String     defaultName;
  private final String     unchangedWarning;

  public UploadConfiguration(Element elm) throws StudyCasterException {
    Element fileFilterElm = ConfigurationUtil.getUniqueElement(elm, "filefilter");
    fileFilter = new MyFileNameExtensionFilter(
        ConfigurationUtil.getStrings(fileFilterElm, "extension"), ConfigurationUtil.getTextContent(
        ConfigurationUtil.getUniqueElement(fileFilterElm, "description")));
    Element defaultNameElm = ConfigurationUtil.getUniqueElement(elm, "default", true);
    defaultName = defaultNameElm == null ? null : ConfigurationUtil.getTextContent(defaultNameElm);
    Element unchangedWarningElm = ConfigurationUtil.getUniqueElement(elm, "unchangedwarning", true);
    unchangedWarning = unchangedWarningElm == null ? null :
        ConfigurationUtil.getSwingCaption(unchangedWarningElm);
  }

  public FileFilter getFileFilter() {
    return fileFilter;
  }

  /** May be null. */
  public String getDefaultName() {
    return defaultName;
  }

  /** May be null. */
  public String getUnchangedWarning() {
    return unchangedWarning;
  }
}
