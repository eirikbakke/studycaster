package no.ebakke.studycaster.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.transform.TransformerException;
import no.ebakke.studycaster.util.XMLUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

final class ConfigurationUtil {
  private static final String XMLNS_SC = "namespace://no.ebakke/studycaster-configuration";

  private ConfigurationUtil() { }

  public static void resolveMacrosInternal(Map<String,Element> macroDefs, Element parent)
      throws StudyCasterException
  {
    for (Element macroElm : XMLUtil.getElements(parent, XMLNS_SC, "macro")) {
      String macroID = macroElm.getAttribute("id");
      Element macroDef = macroDefs.get(macroID);
      // TODO: Escape error message.
      if (macroDef == null)
        throw new StudyCasterException("Unknown configuration macro \"" + macroID + "\"");
      NodeList nl = macroDef.getChildNodes();
      for (int i = 0; i < nl.getLength(); i++)
        parent.insertBefore(nl.item(i).cloneNode(true), macroElm);
      parent.removeChild(macroElm);
    }
    NodeList nl = parent.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
      Node node = nl.item(i);
      if (node instanceof Element) {
        Element elm = (Element) node;
        resolveMacrosInternal(macroDefs, elm);
      }
    }
  }

  public static void resolveMacros(Element root) throws StudyCasterException {
    Map<String,Element> macroDefs = new LinkedHashMap<String,Element>();
    for (Element elm : XMLUtil.getElements(root, XMLNS_SC, "macrodef")) {
      Element stored = (Element) elm.cloneNode(true);
      root.removeChild(elm);
      resolveMacrosInternal(macroDefs, stored);
      macroDefs.put(elm.getAttribute("id"), stored);
    }
    resolveMacrosInternal(macroDefs, root);
  }

  public static String getNonEmptyAttribute(Element elm, String attrName)
      throws StudyCasterException
  {
    String ret = elm.getAttribute(attrName);
    if (ret.equals("")) {
      throw new StudyCasterException(
          "Expected an attribute " + attrName + " in element <" + elm.getTagName() + ">");
    }
    return ret;
  }

  public static Element getUniqueElement(Node parent, String localName, String attrName, String attrValue)
      throws StudyCasterException
  {
    List<Element> ret = XMLUtil.getElements(parent, XMLNS_SC, localName, attrName, attrValue);
    if (ret.size() != 1) {
      // TODO: Escape.
      String attr = (attrName == null) ? "" : (" " + attrName + "=\"" + attrValue + "\"");
      throw new StudyCasterException(
          "Expected a single <" + localName + " xmlns=\"" + XMLNS_SC + "\"" + attr + "> element, " +
          "got " + ret.size());
    }
    return ret.get(0);
  }

  public static Element getUniqueElement(Node parent, String localName) throws StudyCasterException {
    return getUniqueElement(parent, localName, null, null);
  }

  public static String getSwingCaption(Node parent, String localName) throws StudyCasterException {
    return getSwingCaption(getUniqueElement(parent, localName));
  }

  private static String getSwingCaption(Element elm) throws StudyCasterException {
    String  textContent = XMLUtil.getTextContent(elm);
    if (textContent != null)
      return textContent;

    List<Element> htmlContent = XMLUtil.getElements(elm, null, "html");
    if (htmlContent.size() != 1) {
      throw new StudyCasterException(
          "UI caption values must be specified as text nodes/CDATA sections or a single" +
          "<html xmlns=\"\"> element.");
    } else {
      try {
        return XMLUtil.getXMLString(htmlContent.get(0), true);
      } catch (TransformerException e) {
        throw new StudyCasterException("Unexpected XML transformation error", e);
      }
    }
  }
}
