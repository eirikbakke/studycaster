package no.ebakke.studycaster.configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.transform.TransformerException;
import no.ebakke.studycaster.backend.StudyCasterException;
import no.ebakke.studycaster.util.XMLUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

final class ConfigurationUtil {
  private static final String XMLNS_SC = "namespace://ebakke.no/studycaster-configuration";

  private ConfigurationUtil() { }

  private static void insertChildrenBefore(Node target, Node parentOfNewChildren, Node refChild) {
    NodeList nl = parentOfNewChildren.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++)
      target.insertBefore(nl.item(i).cloneNode(true), refChild);
  }

  public static void resolveMacros(Map<String,Element> macroDefs, Node parent)
      throws StudyCasterException
  {
    NodeList nl = parent.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
      Node node = nl.item(i);
      if (node instanceof Element) {
        Element elm = (Element) node;
        resolveMacros(new LinkedHashMap<String,Element>(macroDefs), elm);
        if (XMLNS_SC.equals(elm.getNamespaceURI()) && "macrodef".equals(elm.getTagName())) {
          Element stored = (Element) elm.cloneNode(true);
          parent.removeChild(elm);
          macroDefs.put(elm.getAttribute("id"), stored);
        }
        if (XMLNS_SC.equals(elm.getNamespaceURI()) && "macro".equals(elm.getTagName())) {
          String macroID = elm.getAttribute("id");
          Element macroDef = macroDefs.get(macroID);
          // TODO: Escape error messages.
          if (macroDef == null)
            throw new StudyCasterException("Unknown configuration macro \"" + macroID + "\"");
          insertChildrenBefore(parent, macroDef, elm);
          parent.removeChild(elm);
        }
      }
    }
  }


  public static String getNonEmptyAttribute(Element elm, String attrName)
      throws StudyCasterException
  {
    String ret = elm.getAttribute(attrName);
    if (ret.length() == 0) {
      throw new StudyCasterException(
          "Expected an attribute \"" + attrName + "\" in element <" + elm.getTagName() + ">");
    }
    return ret;
  }

  public static boolean getBooleanAttribute(Element elm, String attrName)
      throws StudyCasterException
  {
    String ret = getNonEmptyAttribute(elm, attrName);
    if        (ret.equals("true" )) {
      return true;
    } else if (ret.equals("false")) {
      return false;
    } else {
      throw new StudyCasterException(
          "Expected either \"true\" or \"false\" for attribute " + attrName);
    }
  }

  public static List<Element> getElements(Node parent, String localName, boolean required)
      throws StudyCasterException
  {
    List<Element> ret = XMLUtil.getElements(parent, XMLNS_SC, localName);
    if (required && ret.isEmpty()) {
      throw new StudyCasterException(
          "Expected one or more <" + localName + " xmlns=\"" + XMLNS_SC + "> elements");
    }
    return ret;
  }

  public static List<String> getStrings(Node parent, String localName) throws StudyCasterException {
    return parseElements(getElements(parent, localName, false), new ElementParser<String>() {
      public String parseElement(Element elm) throws StudyCasterException {
        return getTextContent(elm);
      }
    });
  }

  public static Element getUniqueElement(Node parent, String localName, boolean optional)
      throws StudyCasterException
  {
    List<Element> ret = XMLUtil.getElements(parent, XMLNS_SC, localName);
    if (optional && ret.isEmpty())
      return null;
    if (ret.size() != 1) {
      throw new StudyCasterException(
          "Expected a single <" + localName + " xmlns=\"" + XMLNS_SC + "\"> element, got " +
          ret.size());
    }
    return ret.get(0);
  }

  public static Element getUniqueElement(Node parent, String localName)
      throws StudyCasterException
  {
    return getUniqueElement(parent, localName, false);
  }

  public static String getTextContent(Element elm) throws StudyCasterException {
    String ret = XMLUtil.getTextContent(elm);
    if (ret == null)
      throw new StudyCasterException("Expected text content in <" + elm.getTagName() + "> element");
    // Collapse whitespace.
    return ret.replaceAll("\\s+", " ");
  }

  public static String getTextContent(Node parent, String localName)
      throws StudyCasterException
  {
    return getTextContent(getUniqueElement(parent, localName, false));
  }

  public static String getSwingCaption(Element elm) throws StudyCasterException {
    String  textContent = XMLUtil.getTextContent(elm);
    if (textContent != null)
      return textContent;

    List<Element> htmlContent = XMLUtil.getElements(elm, null, "html");
    if (htmlContent.size() != 1) {
      throw new StudyCasterException(
          "UI caption values must be specified as text nodes/CDATA sections or a single" +
          "<html xmlns=\"\"> element.");
    } else {
      /* TODO: Consider always including a <base> tag that points to a set of image resources
               configured to be automatically downloaded. */
      try {
        /* Collapse whitespace. Newlines in HTML markup seems to confuse Swing. */
        return XMLUtil.getXMLString(htmlContent.get(0), true).replaceAll("\\s+", " ");
      } catch (TransformerException e) {
        throw new StudyCasterException("Unexpected XML transformation error", e);
      }
    }
  }

  public static <R> List<R> parseElements(List<Element> elms, ElementParser<R> parser)
      throws StudyCasterException
  {
    List<R> ret = new ArrayList<R>();
    for (Element elm : elms)
      ret.add(parser.parseElement(elm));
    return ret;
  }

  public static interface ElementParser<R> {
    R parseElement(Element elm) throws StudyCasterException;
  }

  // Copied from the package-private method with the same name in SwingUtilities.
  public static int findDisplayedMnemonicIndex(String text, int mnemonic) {
    if (text == null || mnemonic == '\0')
        return -1;
    char uc = Character.toUpperCase((char) mnemonic);
    char lc = Character.toLowerCase((char) mnemonic);

    int uci = text.indexOf(uc);
    int lci = text.indexOf(lc);

    if        (uci == -1) {
      return lci;
    } else if (lci == -1) {
      return uci;
    } else {
      return (lci < uci) ? lci : uci;
    }
  }
}
