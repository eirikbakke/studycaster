package no.ebakke.studycaster.configuration;

public enum UIStringKey {
  DIALOG_ALREADY_RUNNING_TITLE(false, false, false),
  DIALOG_ALREADY_RUNNING_MESSAGE(false, true, false),
  MAINFRAME_OPEN_BUTTON(true, true, false),
  MAINFRAME_UPLOAD_BUTTON(true, true, false),
  MAINFRAME_BACK_BUTTON(true, true, false),
  MAINFRAME_NEXT_BUTTON(true, true, false),
  DIALOG_CLOSE_TITLE(false, false, false),
  DIALOG_CLOSE_MESSAGE(false, true, false),
  OPENFILE_PROGRESS(false, false, false);

  private boolean usesMnemonic, htmlAllowed, usesMessageFormat;

  private UIStringKey(boolean usesMnemonic, boolean htmlAllowed, boolean usesMessageFormat) {
    this.usesMnemonic      = usesMnemonic;
    this.htmlAllowed       = htmlAllowed;
    this.usesMessageFormat = usesMessageFormat;
  }

  public boolean usesMnemonic() {
    return usesMnemonic;
  }

  public boolean isHtmlAllowed() {
    return htmlAllowed;
  }

  public boolean takesParameters() {
    return usesMessageFormat;
  }
}
