package no.ebakke.studycaster.configuration;

import javax.swing.filechooser.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import no.ebakke.studycaster.api.StudyCasterException;
import no.ebakke.studycaster.util.MyFileNameExtensionFilter;
import no.ebakke.studycaster.util.XMLUtil;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

// TODO: Have a way for the server to test-parse the configuration.
public class StudyConfiguration {
  private static final boolean DEBUG_MACROS = false;
  private final String name;
  private final FileFilter uploadFileFilter;
  private final List<PageConfiguration> pageConfiguration;
  private final List<String> screenCastWhiteList;
  private final List<String> screenCastBlackList;
  private final UIStrings uiStrings;

  public static StudyConfiguration parseConfiguration(InputStream xml, String configurationID)
      throws StudyCasterException, IOException
  {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db;
    Document configDoc;
    dbf.setNamespaceAware(true);
    try {
      db = dbf.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new StudyCasterException("Error initializing XML parser", e);
    }
    try {
      configDoc = db.parse(xml);
    } catch (SAXException e) {
      throw new StudyCasterException("Error parsing configuration file", e);
    }
    Element root = ConfigurationUtil.getUniqueElement(configDoc, "study");

    ConfigurationUtil.resolveMacros(root);
    if (DEBUG_MACROS) {
      System.err.println("========================================================");
      try {
        System.err.println(XMLUtil.getXMLString(root, false));
      } catch (TransformerException ex) {
        throw new StudyCasterException(ex);
      }
      System.err.println("========================================================");
    }
    // While we're only interested in one configuration here, parse all of them to detect errors.
    List<StudyConfiguration> ret = new ArrayList<StudyConfiguration>();
    for (Element elm : ConfigurationUtil.getElements(root, "configuration", true)) {
      StudyConfiguration conf = new StudyConfiguration(elm);
      if (elm.getAttribute("id").equals(configurationID))
        ret.add(conf);
    }
    if (ret.size() != 1)
      throw new StudyCasterException("Expected exactly one matching study configuration.");
    return ret.get(0);
  }

  private StudyConfiguration(Element conf) throws StudyCasterException {
    name              = ConfigurationUtil.getNonEmptyAttribute(conf, "name");
    pageConfiguration = PageConfiguration.parse(conf);

    Element uploadFile = ConfigurationUtil.getUniqueElement(conf, "uploadfile");
    Element fileFilter = ConfigurationUtil.getUniqueElement(uploadFile, "filefilter");

    // TODO: Use Apache Commons file filter classes instead.
    uploadFileFilter = new MyFileNameExtensionFilter(
        ConfigurationUtil.getStrings(fileFilter, "extension"),
        ConfigurationUtil.getTextContent(
        ConfigurationUtil.getUniqueElement(fileFilter, "description")));

    Element screencast = ConfigurationUtil.getUniqueElement(conf, "screencast");
    screenCastWhiteList = ConfigurationUtil.getStrings(screencast, "whitelist");
    screenCastBlackList = ConfigurationUtil.getStrings(screencast, "blacklist");

    uiStrings = new UIStrings(ConfigurationUtil.getUniqueElement(conf, "uistrings"));
  }

  // TODO: Get rid of this.
  public String getInstructions() {
    if (pageConfiguration.size() != 1)
      throw new UnsupportedOperationException();
    return pageConfiguration.get(0).getInstructions();
  }

  // TODO: Get rid of this.
  public OpenFileConfiguration getOpenFileConfiguration() {
    if (pageConfiguration.size() != 1)
      throw new UnsupportedOperationException();
    OpenFileConfiguration ret = pageConfiguration.get(0).getOpenFileConfiguration();
    if (ret == null)
      throw new UnsupportedOperationException();
    return ret;
  }

  public String getName() {
    return name;
  }

  public FileFilter getUploadFileFilter() {
    return uploadFileFilter;
  }

  public List<String> getScreenCastWhiteList() {
    return new ArrayList<String>(screenCastWhiteList);
  }

  public List<String> getScreenCastBlackList() {
    return new ArrayList<String>(screenCastBlackList);
  }

  public UIStrings getUIStrings() {
    return uiStrings;
  }
}
