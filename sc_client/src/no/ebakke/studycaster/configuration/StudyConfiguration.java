package no.ebakke.studycaster.configuration;

import javax.swing.filechooser.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import no.ebakke.studycaster.api.StudyCasterException;
import no.ebakke.studycaster.util.XMLUtil;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

// TODO: Have a way for the server to test-parse the configuration.
public class StudyConfiguration {
  private static final boolean DEBUG_MACROS = false;
  private final String name, id;
  private final List<PageConfiguration> pageConfigurations;
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
    StudyConfiguration ret = null;
    final Set<String> ids = new LinkedHashSet<String>();
    for (Element elm : ConfigurationUtil.getElements(root, "configuration", true)) {
      StudyConfiguration conf = new StudyConfiguration(elm);
      if (!ids.add(conf.getID()))
        throw new StudyCasterException("Repeated configuration ID \"" + conf.getID() + "\"");
      if (conf.getID().equals(configurationID))
        ret = conf;
    }
    if (ret == null) {
      throw new StudyCasterException(
          "Could not find configuration with ID \"" + configurationID + "\"");
    }
    return ret;
  }

  private StudyConfiguration(Element conf) throws StudyCasterException {
    final Element screencast = ConfigurationUtil.getUniqueElement(conf, "screencast");
    name                = ConfigurationUtil.getNonEmptyAttribute(conf, "name");
    id                  = ConfigurationUtil.getNonEmptyAttribute(conf, "id"  );
    pageConfigurations  = PageConfiguration.parse(conf);
    screenCastWhiteList = ConfigurationUtil.getStrings(screencast, "whitelist");
    screenCastBlackList = ConfigurationUtil.getStrings(screencast, "blacklist");

    uiStrings = new UIStrings(ConfigurationUtil.getUniqueElement(conf, "uistrings"));
  }

  // TODO: Get rid of this.
  private PageConfiguration getLonePageConfiguration() {
    if (pageConfigurations.size() != 1)
      throw new UnsupportedOperationException();
    return pageConfigurations.get(0);
  }

  // TODO: Get rid of this.
  public String getInstructions() {
    return getLonePageConfiguration().getInstructions();
  }

  // TODO: Get rid of this.
  public OpenFileConfiguration getOpenFileConfiguration() {
    OpenFileConfiguration ret = getLonePageConfiguration().getOpenFileConfiguration();
    if (ret == null)
      throw new UnsupportedOperationException();
    return ret;
  }

  // TODO: Get rid of this.
  public FileFilter getUploadFileFilter() {
    ConcludeConfiguration conclude = getLonePageConfiguration().getConcludeConfiguration();
    if (conclude == null)
      throw new UnsupportedOperationException();
    UploadConfiguration ret = conclude.getUploadConfiguration();
    if (ret == null)
      throw new UnsupportedOperationException();
    return ret.getFileFilter();
  }

  public String getName() {
    return name;
  }

  public String getID() {
    return id;
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

  public List<PageConfiguration> getPageConfigurations() {
    return new ArrayList<PageConfiguration>(pageConfigurations);
  }
}
