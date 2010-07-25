package no.ebakke.studycaster2;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.UIManager;

public final class NativeLibrary {
  public NativeLibrary() { }

  // TODO: Change this interface to move more functionarily out of the native library (retrieve a window list with titles and coordinates instead).
  private static native void getPermittedRecordingArea_internal(String whiteList[], String blackList[], int result[]);

  public static Rectangle getPermittedRecordingArea(List<String> titleMustInclude, boolean excludeFileDialogs) {
    int result[] = new int[4];

    List<String> blackList = new ArrayList<String>();
    if (excludeFileDialogs) {
      blackList.add("Save");
      blackList.add("Open");
      String localized;
      if ((localized = UIManager.getString("FileChooser.saveDialogTitleText")) != null)
        blackList.add(localized);
      if ((localized = UIManager.getString("FileChooser.openDialogTitleText")) != null)
        blackList.add(localized);
    }

    getPermittedRecordingArea_internal(titleMustInclude.toArray(new String[0]), blackList.toArray(new String[0]), result);
    return new Rectangle(result[0], result[1], result[2], result[3]);
  }
}
