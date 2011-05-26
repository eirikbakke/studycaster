package no.ebakke.studycaster.api;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class StudyConfiguration {
  private static final String XMLNS_SC = "http://www.sieuferd.com/namespaces/studycaster-configuration";

  public StudyConfiguration(InputStream xmlConfiguration) throws StudyCasterException, IOException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db;
    Document configurationFile;
    dbf.setNamespaceAware(true);
    try {
      db = dbf.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new StudyCasterException("Error initializing XML parser", e);
    }
    try {
      configurationFile = db.parse(xmlConfiguration);
    } catch (SAXException e) {
      throw new StudyCasterException("Error parsing configuration file", e);
    }
    configurationFile.normalize();
    parseConfiguration(configurationFile);
  }

  private static Element expectOneNode(NodeList nl, String localName) throws StudyCasterException {
    if (nl.getLength() != 1 || !(nl.item(0) instanceof Element))
      throw new StudyCasterException("Expected a single <" + localName + " xmlns=\"" + XMLNS_SC + "\"> element");
    return (Element) nl.item(0);
  }

  private static Element expectOneNode(Document doc, String localName) throws StudyCasterException {
    return expectOneNode(doc.getElementsByTagNameNS(XMLNS_SC, localName), localName);
  }

  private static Element expectOneNode(Element e, String localName) throws StudyCasterException {
    return expectOneNode(e.getElementsByTagNameNS(XMLNS_SC, localName), localName);
  }

  private static void parseConfiguration(Document doc) throws StudyCasterException {
    Element root = expectOneNode(doc, "study");
    Element instructions = expectOneNode(root, "instructions");
    
    System.out.println(instructions.getTextContent());
  }
}
