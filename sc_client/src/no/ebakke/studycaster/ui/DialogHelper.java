package no.ebakke.studycaster.ui;

import java.awt.Window;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import no.ebakke.studycaster.configuration.UIStringKey;
import no.ebakke.studycaster.configuration.UIStrings;
import no.ebakke.studycaster.ui.UIUtil.CallableExt;

// TODO: Pause tasks while showing dialog boxes.

/** Helper methods to display parameterized dialogs from any thread with proper logging. Public
methods in this class can be called from any thread, including the EDT. */
public class DialogHelper {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  private final Window parentWindow;
  private volatile UIStrings strings;
  private volatile boolean closed = false;

  public DialogHelper(Window parentFrame) {
    /** Returns a hidden child dialog that can be used as a parent of dialogs that need to be
    centered on the screen while still being descendants of the normally off-center MainFrame
    dialog. */
    this.parentWindow = parentFrame;
  }

  public void setStrings(UIStrings strings) {
    this.strings = strings;
  }

  public void setClosed(boolean closed) {
    this.closed = closed;
  }

  private int showDialogHelper(final Object message, final String title, final int optionType,
      final int messageType, final Object[] options, final Object initialValue)
  {
    if (closed) {
      LOG.log(Level.INFO, "Suppressing dialog due to earlier close action");
      return JOptionPane.CLOSED_OPTION;
    }
    try {
      return UIUtil.swingBlock(new CallableExt<Integer,RuntimeException>() {
        public Integer call() {
          return JOptionPane.showOptionDialog(parentWindow, message, title,
              optionType, messageType, null, options, initialValue);
        }
      });
    } catch (InterruptedException e) {
      LOG.severe("swingBlock() interrupted");
      Thread.currentThread().interrupt();
      return JOptionPane.CLOSED_OPTION;
    }
  }

  public int showCustomOptionDialog(String dialogName, Object message, UIStringKey titleKey,
      int optionType, int messageType, UIStringKey[] options, UIStringKey initialValue)
  {
    LOG.log(Level.INFO, "Showing dialog {0}", dialogName);
    Object optionsObj[]    = null;
    Object initialValueObj = null;
    if (options != null) {
      optionsObj = new Object[options.length];
      for (int i = 0; i < options.length; i++) {
        optionsObj[i] = strings.getString(options[i]);
        if (options[i].equals(initialValue))
          initialValueObj = optionsObj[i];
      }
    }
    int ret = showDialogHelper(
        message, strings.getString(titleKey), optionType, messageType, optionsObj, initialValueObj);
    final String choiceString =
        (options != null && ret >= 0 && ret < options.length) ? options[ret].toString() :
        UIUtil.jOptionPaneChoiceString(ret);
    LOG.log(Level.INFO, "User chose {0} at dialog {1}", new Object[] { choiceString, dialogName});
    return ret;
  }

  public int showOptionDialog(UIStringKey messageKey,
      Object messageParameters[], UIStringKey titleKey, int optionType, int messageType,
      UIStringKey[] options, UIStringKey initialValue)
  {
    final String message = (messageParameters != null) ?
        strings.getString(messageKey, messageParameters) : strings.getString(messageKey);
    return showCustomOptionDialog(messageKey.toString(), message, titleKey, optionType, messageType,
        options, initialValue);
  }

  public void showMessageDialog(UIStringKey messageKey,
      Object messageParameters[], UIStringKey titleKey, int messageType)
  {
    showOptionDialog(messageKey, messageParameters, titleKey, JOptionPane.DEFAULT_OPTION,
        messageType, null, null);
  }

  // TODO: Consider showing a confirmation code in this case.
  public void showErrorDialog(Exception e) {
    LOG.log(Level.SEVERE, "Unexpected error, showing dialog", e);
    int ret = showDialogHelper("There was an unexpected error:\n" + e.getMessage(), "Error",
        JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, null);
    LOG.log(Level.INFO, "User chose {0} at unexpected error dialog",
        UIUtil.jOptionPaneChoiceString(ret));
  }
}
