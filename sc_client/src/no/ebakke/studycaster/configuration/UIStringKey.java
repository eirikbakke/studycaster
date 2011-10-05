package no.ebakke.studycaster.configuration;

public enum UIStringKey {
  DIALOG_ALREADY_RUNNING_TITLE(false, false),
  DIALOG_ALREADY_RUNNING_MESSAGE(false, true),
  MAINFRAME_OPEN_BUTTON(true, true),
  MAINFRAME_UPLOAD_BUTTON(true, true),
  MAINFRAME_BACK_BUTTON(true, true),
  MAINFRAME_NEXT_BUTTON(true, true);

  private boolean hasMnemonic, htmlAllowed;

  private UIStringKey(boolean mnemonicRequired, boolean htmlAllowed) {
    this.hasMnemonic = mnemonicRequired;
    this.htmlAllowed      = htmlAllowed;
  }

  public boolean hasMnemonic() {
    return hasMnemonic;
  }

  public boolean isHtmlAllowed() {
    return htmlAllowed;
  }
}
