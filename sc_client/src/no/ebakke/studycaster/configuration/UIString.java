package no.ebakke.studycaster.configuration;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import no.ebakke.studycaster.backend.StudyCasterException;
import org.w3c.dom.Element;

public class UIString {
  private final String  string;
  private final Integer mnemonicIndex;
  private final boolean mnemonicAllowed;

  public UIString(String string, Integer mnemonicIndex) {
    this.string          = string;
    this.mnemonicIndex   = mnemonicIndex;
    this.mnemonicAllowed = true;
  }

  public UIString(String string) {
    this.string          = string;
    this.mnemonicIndex   = null;
    this.mnemonicAllowed = false;
  }

  public static UIString readOne(Element elm, boolean htmlAllowed, boolean mnemonicAllowed)
      throws StudyCasterException
  {
    String string = htmlAllowed ? ConfigurationUtil.getSwingCaption(elm) :
          ConfigurationUtil.getTextContent(elm);
    final String mnemonicAtt      = elm.getAttribute("mnemonic");
    final String mnemonicIndexAtt = elm.getAttribute("mnemonicindex");
    Character mnemonicChar  = null;
    Integer   mnemonicIndex = null;

    if        (mnemonicAtt.length() == 1) {
      mnemonicChar = mnemonicAtt.charAt(0);
    } else if (mnemonicAtt.length() > 1) {
      throw new StudyCasterException("The mnemonic attribute must be a single character");
    }
    if (mnemonicIndexAtt.length() > 0) {
      try {
        mnemonicIndex = Integer.parseInt(mnemonicIndexAtt);
      } catch (NumberFormatException e) {
        throw new StudyCasterException("The specified mnemonic index is not a number", e);
      }
    }
    if (string.startsWith("<html>") && (mnemonicChar != null || mnemonicIndex != null))
      throw new StudyCasterException("Mnemonics cannot be specified for HTML strings");
    if (mnemonicIndex != null && (mnemonicIndex < 0 || mnemonicIndex >= string.length()))
      throw new StudyCasterException("Mnemonic index out of range");
    if (mnemonicChar != null) {
      if (mnemonicIndex != null)
        throw new StudyCasterException("Cannot specify both mnemonic character and mnemonic index");
      mnemonicIndex = ConfigurationUtil.findDisplayedMnemonicIndex(string, mnemonicChar);
      if (mnemonicIndex < 0)
        throw new StudyCasterException("Cannot locate mnemonic character");
    }
    if (mnemonicIndex != null && !mnemonicAllowed)
      throw new StudyCasterException("Mnemonic not allowed for this UI string");

    return (mnemonicAllowed) ? new UIString(string, mnemonicIndex) : new UIString(string);
  }

  private int getMnemonicIndex() {
    if (!mnemonicAllowed)
      throw new IllegalStateException("Mnemonic not used for this UI string");
    return mnemonicIndex == null ? -1 : mnemonicIndex;
  }

  public String getString() {
    return string;
  }

  public void setOnButton(AbstractButton button) {
    if (!mnemonicAllowed)
      throw new AssertionError("Mnemonic should have been allowed for this UI string");
    button.setText(string);
    button.setDisplayedMnemonicIndex(getMnemonicIndex());
  }

  public void setOnLabel(JLabel label) {
    label.setText(string);
    if (mnemonicAllowed)
      label.setDisplayedMnemonicIndex(getMnemonicIndex());
  }
}
