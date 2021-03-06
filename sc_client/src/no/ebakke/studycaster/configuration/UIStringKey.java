package no.ebakke.studycaster.configuration;

public enum UIStringKey {
  DIALOG_CONSENT_TITLE(false, false, false),
  DIALOG_CONSENT_QUESTION(false, true, false),
  DIALOG_ALREADY_RUNNING_TITLE(false, false, false),
  DIALOG_ALREADY_RUNNING_MESSAGE(false, true, false),
  MAINFRAME_BACK_BUTTON(true, true, false),
  MAINFRAME_NEXT_BUTTON(true, true, false),
  DIALOG_CLOSE_TITLE(false, false, false),
  DIALOG_CLOSE_MESSAGE(false, true, false),
  PROGRESS_OPEN_FILE(false, false, false),
  PROGRESS_OPEN_URI(false, false, false),
  PROGRESS_UPLOAD_FILE(false, false, false),
  PROGRESS_UPLOAD_SCREENCAST(false, false, false),
  DIALOG_OPEN_FILE_TITLE(false, false, false),
  DIALOG_OPEN_FILE_EXISTING_MESSAGE(false, true, true),
  DIALOG_OPEN_FILE_MODIFIED_MESSAGE(false, true, true),
  DIALOG_OPEN_FILE_RENAMED_MESSAGE(false, true, true),
  DIALOG_OPEN_FILE_RENAME_FAILED_MESSAGE(false, true, true),
  DIALOG_OPEN_FILE_ASSOCIATION_FAILED_MESSAGE(false, true, true),
  DIALOG_OPEN_FILE_ALREADY_MESSAGE(false, true, true),
  DIALOG_OPEN_FILE_NEW_BUTTON(false, true, false),
  DIALOG_OPEN_FILE_KEEP_BUTTON(false, true, false),
  DIALOG_CONCLUDE_TITLE(false, false, false),
  DIALOG_CONCLUDE_FILE_PATH_LABEL(true, true, false),
  DIALOG_CONCLUDE_FILE_INFO_LABEL(false, true, false),
  DIALOG_CONCLUDE_FILE_BROWSE_BUTTON(true, false, false),
  DIALOG_CONCLUDE_QUESTION(false, true, false),
  DIALOG_CONCLUDE_FILE_NOT_OPENED_MESSAGE(false, true, false),
  DIALOG_CONCLUDE_EMPTY_PATH_MESSAGE(false, true, false),
  DIALOG_CONCLUDE_FILE_NOT_EXISTS_MESSAGE(false, true, true),
  DIALOG_CONCLUDE_FILE_OPEN_MESSAGE(false, true, true),
  DIALOG_CONCLUDE_FILE_NOT_MODIFIED_MESSAGE(false, true, true),
  DIALOG_CONFIRMATION_TITLE(false, false, false),
  DIALOG_CONFIRMATION_CODEBOX_LABEL(true, true, false),
  DIALOG_CONFIRMATION_CLIPBOARD_MESSAGE(false, true, false);

  private boolean mnemonicAllowed, htmlAllowed, usesMessageFormat;

  private UIStringKey(boolean mnemonicAllowed, boolean htmlAllowed, boolean usesMessageFormat) {
    this.mnemonicAllowed   = mnemonicAllowed;
    this.htmlAllowed       = htmlAllowed;
    this.usesMessageFormat = usesMessageFormat;
  }

  public boolean isMnemonicAllowed() {
    return mnemonicAllowed;
  }

  public boolean isHtmlAllowed() {
    return htmlAllowed;
  }

  public boolean takesParameters() {
    return usesMessageFormat;
  }
}
