package no.ebakke.studycaster.configuration;

import java.text.MessageFormat;
import java.util.EnumMap;
import java.util.Map;
import no.ebakke.studycaster.backend.StudyCasterException;
import org.apache.commons.lang3.StringEscapeUtils;
import org.w3c.dom.Element;

public class UIStrings {
  private final Map<UIStringKey,UIString> strings;

  public UIStrings(Element elm) throws StudyCasterException {
    strings   = new EnumMap<UIStringKey,UIString>(UIStringKey.class);

    for (Element uiStringElm : ConfigurationUtil.getElements(elm, "uistring", false)) {
      String keyS = uiStringElm.getAttribute("key");
      UIStringKey key;
      try {
        key = UIStringKey.valueOf(keyS);
      } catch (IllegalArgumentException e) {
        throw new StudyCasterException("Unknown UI string key \"" + keyS + "\"");
      }
      strings.put(key, UIString.readOne(uiStringElm, key.isHtmlAllowed(), key.isMnemonicAllowed()));
    }
    for (UIStringKey key : UIStringKey.values()) {
      if (!strings.containsKey(key))
        throw new StudyCasterException("Missing UI string with key " + key.name());
    }
  }

  public String getString(UIStringKey key) {
    if (key.takesParameters())
      throw new IllegalArgumentException("UI string with key " + key + " takes parameters");
    return get(key).getString();
  }

  // TODO: Use designated HTML parameter tags instead of MessageFormat.
  public String getString(UIStringKey key, Object parameters[]) {
    if (!key.takesParameters())
      throw new IllegalArgumentException("UI string with key " + key + " does not take parameters");
    final Object escapedParameters[];
    final String msg = strings.get(key).getString();
    if (msg.startsWith("<html>")) {
      escapedParameters = new Object[parameters.length];
      for (int i = 0; i < parameters.length; i++) {
        escapedParameters[i] = StringEscapeUtils.escapeHtml3(
            parameters[i] == null ? "null" : parameters[i].toString());
      }
    } else {
      escapedParameters = parameters;
    }
    return new MessageFormat(msg).format(escapedParameters);
  }

  public UIString get(UIStringKey key) {
    return strings.get(key);
  }
}
