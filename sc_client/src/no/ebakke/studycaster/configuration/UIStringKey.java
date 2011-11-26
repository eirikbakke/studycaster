package no.ebakke.studycaster.configuration;

public enum UIStringKey {
  DIALOG_ALREADY_RUNNING_TITLE(false, false),
  DIALOG_ALREADY_RUNNING_MESSAGE(false, true),
  MAINFRAME_OPEN_BUTTON(true, true),
  MAINFRAME_UPLOAD_BUTTON(true, true),
  MAINFRAME_BACK_BUTTON(true, true),
  MAINFRAME_NEXT_BUTTON(true, true),
  DIALOG_CLOSE_TITLE(false, false),
  DIALOG_CLOSE_MESSAGE(false, true),
  OPENFILE_PROGRESS(false, false);

  private boolean hasMnemonic, htmlAllowed;

  private UIStringKey(boolean hasMnemonic, boolean htmlAllowed) {
    this.hasMnemonic = hasMnemonic;
    this.htmlAllowed = htmlAllowed;
  }

  public boolean hasMnemonic() {
    return hasMnemonic;
  }

  public boolean isHtmlAllowed() {
    return htmlAllowed;
  }
}
