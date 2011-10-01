package no.ebakke.studycaster.util;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import no.ebakke.studycaster.api.StudyCasterException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

public final class XMLUtil {
  private XMLUtil() { }

  private static List<Node> getNonEmptyChildNodes(Node parent) {
    NodeList nl = parent.getChildNodes();
    List<Node> ret = new ArrayList<Node>();
    for (int i = 0; i < nl.getLength(); i++) {
      if (!(nl.item(i) instanceof Text && ((Text) nl.item(i)).getTextContent().trim().length() == 0))
        ret.add(nl.item(i));
    }
    return ret;
  }

  public static List<Element> getElements(Node parent, String namespaceURI, String localName) {
    List<Element> ret = new ArrayList<Element>();
    for (Node node : getNonEmptyChildNodes(parent)) {
      if (!(node instanceof Element))
        continue;
      Element elm = (Element) node;
      if ((namespaceURI == null && elm.getNamespaceURI() == null ||
          namespaceURI != null && namespaceURI.equals(elm.getNamespaceURI())) &&
          elm.getLocalName().equals(localName))
      {
        ret.add((Element) node);
      }
    }
    return ret;
  }

  /** Returns concatenated, then trimmed text and CDATA sections, or null if any other kinds of
  child nodes are present. */
  public static String getTextContent(Node parent) {
    List<Node> childNodes = getNonEmptyChildNodes(parent);
    StringBuilder ret = new StringBuilder();
    /* This loop is necessary even under normalization, because text nodes may be adjacent to CDATA
    sections. */
    for (Node childNode : childNodes) {
      if (!(childNode instanceof Text))
        return null;
      ret.append(((Text) childNode).getTextContent());
    }
    return ret.toString().trim();
  }

  public static String getXMLString(Element elm, boolean htmlMode) throws TransformerException {
    Transformer t;
    t = TransformerFactory.newInstance().newTransformer();
    t.setOutputProperty(OutputKeys.METHOD              , htmlMode ? "html" : "xml");
    t.setOutputProperty(OutputKeys.INDENT              , "no");
    t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, htmlMode ? "yes" : "no");
    StringWriter ret = new StringWriter();
    t.transform(new DOMSource(elm), new StreamResult(ret));
    return ret.toString();
  }
}
