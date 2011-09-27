package no.ebakke.studycaster.api;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import no.ebakke.studycaster.util.XMLUtil;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class StudyConfiguration {
  private final boolean DEBUG_MACROS = false;
  private String name;
  private String instructions;

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
  }

  public String getInstructions() {
    return instructions;
  }

  public String getName() {
    return name;
  }
}
