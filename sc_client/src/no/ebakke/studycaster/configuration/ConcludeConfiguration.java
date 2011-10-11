package no.ebakke.studycaster.configuration;

import javax.swing.filechooser.FileFilter;
import no.ebakke.studycaster.api.StudyCasterException;
import no.ebakke.studycaster.util.MyFileNameExtensionFilter;
import org.w3c.dom.Element;

public class ConcludeConfiguration {
  private final FileFilter uploadFileFilter;

  public ConcludeConfiguration(Element elm) throws StudyCasterException {
    Element uploadElm = ConfigurationUtil.getUniqueElement(elm, "uploadfile", true);
    uploadFileFilter = (uploadElm == null) ? null :
        parseFileFilter(ConfigurationUtil.getUniqueElement(uploadElm, "filefilter"));
  }

  private static FileFilter parseFileFilter(Element elm) throws StudyCasterException{
    // TODO: Use Apache Commons file filter classes instead.
    return new MyFileNameExtensionFilter(
        ConfigurationUtil.getStrings(elm, "extension"),
        ConfigurationUtil.getTextContent(
        ConfigurationUtil.getUniqueElement(elm, "description")));
  }

  public FileFilter getUploadFileFilter() {
    return uploadFileFilter;
  }
}
