package no.ebakke.studycaster.util;

import java.util.ArrayList;
import java.util.List;
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
          elm.getNamespaceURI().equals(namespaceURI)) && elm.getLocalName().equals(localName)) {
        ret.add((Element) node);
      }
    }
    return ret;
  }

  public static Element getElement(Node parent, String namespaceURI, String localName) {
    List<Element> ret = getElements(parent, namespaceURI, localName);
    return ret.size() == 1 ? ret.get(0) : null;
  }

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
}
