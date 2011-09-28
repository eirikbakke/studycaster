package no.ebakke.studycaster.api;

import javax.swing.filechooser.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import no.ebakke.studycaster.util.MyFileNameExtensionFilter;
import no.ebakke.studycaster.util.XMLUtil;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class StudyConfiguration {
  private final boolean DEBUG_MACROS = false;
  private String name;
  private String instructions;
  private FileFilter uploadFileFilter;
  private String openFileRemoteName;
  private String openFileLocalName;
  private String openFileRequirement;

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

    Element conf = ConfigurationUtil.getUniqueElement(root, "configuration", "id", configurationID);
    name         = ConfigurationUtil.getNonEmptyAttribute(conf, "name");
    instructions = ConfigurationUtil.getSwingCaption(conf, "instructions");

    Element uploadFile = ConfigurationUtil.getUniqueElement(conf, "uploadfile");
    Element fileFilter = ConfigurationUtil.getUniqueElement(uploadFile, "filefilter");

    List<String> extensions = new ArrayList<String>();
    for (Element elm : ConfigurationUtil.getElements(fileFilter, "extension"))
      extensions.add(ConfigurationUtil.getTextContent(elm));
    // TODO: Use Apache Commons file filter classes instead.
    uploadFileFilter = new MyFileNameExtensionFilter(extensions,
        ConfigurationUtil.getTextContent(
        ConfigurationUtil.getUniqueElement(fileFilter, "description")));

    Element openFile = ConfigurationUtil.getUniqueElement(conf, "openfile");
    openFileLocalName = ConfigurationUtil.getTextContent(
        ConfigurationUtil.getUniqueElement(openFile, "localname"));
    openFileRemoteName = ConfigurationUtil.getTextContent(
        ConfigurationUtil.getUniqueElement(openFile, "remotename"));
    openFileRequirement = ConfigurationUtil.getTextContent(
        ConfigurationUtil.getUniqueElement(openFile, "requirement"));
  }

  public String getInstructions() {
    return instructions;
  }

  public String getName() {
    return name;
  }

  public FileFilter getUploadFileFilter() {
    return uploadFileFilter;
  }

  public String getOpenFileLocalName() {
    return openFileLocalName;
  }

  public String getOpenFileRemoteName() {
    return openFileRemoteName;
  }

  public String getOpenFileRequirement() {
    return openFileRequirement;
  }
}
