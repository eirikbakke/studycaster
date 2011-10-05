package no.ebakke.studycaster.configuration;

import java.util.EnumMap;
import java.util.Map;
import no.ebakke.studycaster.api.StudyCasterException;
import org.w3c.dom.Element;

public class UIStrings {
  private Map<UIStringKey,String>    strings;
  private Map<UIStringKey,Character> mnemonics;

  public UIStrings(Element elm) throws StudyCasterException {
    strings   = new EnumMap<UIStringKey,String>(UIStringKey.class);
    mnemonics = new EnumMap<UIStringKey,Character>(UIStringKey.class);
    for (Element uiStringElm : ConfigurationUtil.getElements(elm, "uistring", false)) {
      String keyS = uiStringElm.getAttribute("key");
      UIStringKey key;
      try {
        key = UIStringKey.valueOf(keyS);
      } catch (IllegalArgumentException e) {
        throw new StudyCasterException("Unknown UI string key \"" + keyS + "\"");
      }
      String value = (key.isHtmlAllowed()) ? ConfigurationUtil.getSwingCaption(uiStringElm) : 
          ConfigurationUtil.getTextContent(uiStringElm);
      strings.put(key, value);
      String mnemonic = uiStringElm.getAttribute("mnemonic");
      if        ( key.hasMnemonic() && mnemonic.length() != 1) {
        throw new StudyCasterException("UI string with key " + key.name() +
            " requires a single-character mnemonic");
      } else if (!key.hasMnemonic() && mnemonic.length() >  0) {
        throw new StudyCasterException("Unnecessary mnemonic for UI string with key " + key.name());
      }
      if (key.hasMnemonic()) {
        if (mnemonic.length() != 1) {
          throw new StudyCasterException("UI string with key " + key.name() +
                      " requires a single-character mnemonic");
        }
        mnemonics.put(key, mnemonic.charAt(0));
      } else if (mnemonic.length() > 0) {
        throw new StudyCasterException("Unnecessary mnemonic for UI string with key " + key.name());
      }

    }
    for (UIStringKey key : UIStringKey.values()) {
      if (!strings.containsKey(key))
        throw new StudyCasterException("Missing UI string with key " + key.name());
    }
  }

  public String getString(UIStringKey key) {
    return strings.get(key);
  }

  public char getMnemonic(UIStringKey key) {
    Character ret = mnemonics.get(key);
    if (ret == null)
      throw new IllegalArgumentException("UI string with key " + key + "does not use a mnemonic");
    return ret;
  }
}
