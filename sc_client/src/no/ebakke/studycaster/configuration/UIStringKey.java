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
  PROGRESS_OPEN(false, false, false),
  DIALOG_OPEN_TITLE(false, false, false),
  DIALOG_OPEN_EXISTING_MESSAGE(false, true, true),
  DIALOG_OPEN_MODIFIED_MESSAGE(false, true, true),
  DIALOG_OPEN_NEW_BUTTON(false, true, false),
  DIALOG_OPEN_KEEP_BUTTON(false, true, false);

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
