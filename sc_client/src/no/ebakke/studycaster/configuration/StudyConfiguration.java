package no.ebakke.studycaster.configuration;

import javax.swing.filechooser.FileFilter;
import java.io.IOException;
import java.io.InputStream;
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

/* TODO: Have a way for the server to test-parse the configuration, and read all configuration
sections as well. */
public class StudyConfiguration {
  private final boolean DEBUG_MACROS = false;
  private String name;
  private FileFilter uploadFileFilter;
  // TODO: Make this a list.
  private PageConfiguration pageConfiguration;
  private OpenFileConfiguration openFileConfiguration;
  private List<String> screenCastWhiteList;
  private List<String> screenCastBlackList;

  public StudyConfiguration(InputStream xmlConfiguration, String configurationID)
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
      configDoc = db.parse(xmlConfiguration);
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

    Element conf = ConfigurationUtil.getUniqueElement(root, "configuration", false, "id", configurationID);
    name         = ConfigurationUtil.getNonEmptyAttribute(conf, "name");
    pageConfiguration = new PageConfiguration(ConfigurationUtil.getUniqueElement(conf, "page"));

    Element uploadFile = ConfigurationUtil.getUniqueElement(conf, "uploadfile");
    Element fileFilter = ConfigurationUtil.getUniqueElement(uploadFile, "filefilter");

    // TODO: Use Apache Commons file filter classes instead.
    uploadFileFilter = new MyFileNameExtensionFilter(
        ConfigurationUtil.getStrings(fileFilter, "extension"),
        ConfigurationUtil.getTextContent(
        ConfigurationUtil.getUniqueElement(fileFilter, "description")));

    openFileConfiguration = new OpenFileConfiguration(ConfigurationUtil.getUniqueElement(conf, "openfile"));

    Element screencast = ConfigurationUtil.getUniqueElement(conf, "screencast");
    screenCastWhiteList = ConfigurationUtil.getStrings(screencast, "whitelist");
    screenCastBlackList = ConfigurationUtil.getStrings(screencast, "blacklist");
  }

  // TODO: Get rid of this.
  public String getInstructions() {
    return pageConfiguration.getInstructions();
  }

  public String getName() {
    return name;
  }

  public FileFilter getUploadFileFilter() {
    return uploadFileFilter;
  }

  public OpenFileConfiguration getOpenFileConfiguration() {
    return openFileConfiguration;
  }

  public List<String> getScreenCastWhiteList() {
    return screenCastWhiteList;
  }

  public List<String> getScreenCastBlackList() {
    return screenCastBlackList;
  }
}
