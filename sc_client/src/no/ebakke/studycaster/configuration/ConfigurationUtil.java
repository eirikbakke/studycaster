package no.ebakke.studycaster.configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.transform.TransformerException;
import no.ebakke.studycaster.api.StudyCasterException;
import no.ebakke.studycaster.util.XMLUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

final class ConfigurationUtil {
  private static final String XMLNS_SC = "namespace://no.ebakke/studycaster-configuration";

  private ConfigurationUtil() { }

  public static void resolveMacrosInternal(Map<String,Element> macroDefs, Node parent)
      throws StudyCasterException
  {
    for (Element macroElm : getElements(parent, "macro")) {
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
    for (Element elm : getElements(root, "macrodef")) {
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

  public static List<Element> getElements(Node parent, String localName) {
    return XMLUtil.getElements(parent, XMLNS_SC, localName);
  }

  public static List<String> getStrings(Node parent, String localName) throws StudyCasterException {
    List<String> ret = new ArrayList<String>();
    for (Element elm : getElements(parent, localName))
      ret.add(getTextContent(elm));
    return ret;
  }

  public static Element getUniqueElement(Node parent, String localName, boolean optional,
      String attrName, String attrValue) throws StudyCasterException
  {
    List<Element> ret = XMLUtil.getElements(parent, XMLNS_SC, localName, attrName, attrValue);
    if (optional && ret.isEmpty())
      return null;
    if (ret.size() != 1) {
      // TODO: Escape.
      String attr = (attrName == null) ? "" : (" " + attrName + "=\"" + attrValue + "\"");
      throw new StudyCasterException(
          "Expected a single <" + localName + " xmlns=\"" + XMLNS_SC + "\"" + attr + "> element, " +
          "got " + ret.size());
    }
    return ret.get(0);
  }

  public static Element getUniqueElement(Node parent, String localName, boolean optional)
      throws StudyCasterException
  {
    return getUniqueElement(parent, localName, optional, null, null);
  }

  public static Element getUniqueElement(Node parent, String localName)
      throws StudyCasterException
  {
    return getUniqueElement(parent, localName, false, null, null);
  }

  public static String getSwingCaption(Node parent, String localName) throws StudyCasterException {
    return getSwingCaption(getUniqueElement(parent, localName, false));
  }

  public static String getTextContent(Element elm) throws StudyCasterException {
    String ret = XMLUtil.getTextContent(elm);
    if (ret == null)
      throw new StudyCasterException("Expected text content in <" + elm.getTagName() + "> element");
    return ret;
  }

  public static String getTextContent(Node parent, String localName)
      throws StudyCasterException
  {
    return getTextContent(getUniqueElement(parent, localName, false));
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

  public static <R> List<R> parseElements(List<Element> elms, ElementParser<R> parser) {
    List<R> ret = new ArrayList<R>();
    for (Element elm : elms)
      ret.add(parser.parseElement(elm));
    return ret;
  }

  public static interface ElementParser<R> {
    R parseElement(Element elm);
  }
}
