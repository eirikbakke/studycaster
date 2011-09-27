package no.ebakke.studycaster.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import no.ebakke.studycaster.util.XMLUtil;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


public class StudyConfiguration {
  private static final String XMLNS_SC = "namespace://no.ebakke/studycaster-configuration";

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
    configDoc.normalize();
    Element root = expectOneNode(configDoc, XMLNS_SC, "study");
    List<Element> config = XMLUtil.getElements(root, XMLNS_SC, "configuration");
    parseConfiguration(config.get(0));
  }

  private static Element expectOneNode(Node node, String namespaceURI, String localName)
      throws StudyCasterException
  {
    Element ret = XMLUtil.getElement(node, namespaceURI, localName);
    if (ret == null) {
      throw new StudyCasterException(
          "Expected a single <" + localName + " xmlns=\"" + namespaceURI + "\"> element");
    }
    return ret;
  }

  private static String swingCaption(Element elm) throws StudyCasterException {
    String  textContent = XMLUtil.getTextContent(elm);
    Element htmlContent = XMLUtil.getElement(elm, null, "html");
    
    if (textContent != null) {
      return textContent;
    } else if (htmlContent != null) {
      Transformer t;
      try {
        t = TransformerFactory.newInstance().newTransformer();
      } catch (TransformerConfigurationException ex) {
        throw new StudyCasterException("Problem while processing XML configuration", ex);
      }
      t.setOutputProperty(OutputKeys.METHOD, "xml");
      t.setOutputProperty(OutputKeys.INDENT, "no");
      t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      StringWriter ret = new StringWriter();
      try {
        t.transform(new DOMSource(htmlContent), new StreamResult(ret));
      } catch (TransformerException ex) {
        throw new StudyCasterException("Problem while processing XML configuration", ex);
      }
      return ret.toString();
    } else {
      throw new StudyCasterException(
          "UI caption values must be specified as text nodes/CDATA sections or a single" +
          "<html xmlns=\"\"> element.");
    }
  }

  private void parseConfiguration(Element configuration) throws StudyCasterException, IOException {
    instructions = swingCaption(expectOneNode(configuration, XMLNS_SC, "instructions"));
    System.out.println(instructions);
  }

  public String getInstructions() {
    return instructions;
  }
}
