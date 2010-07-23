package no.ebakke.studycaster2;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.UIManager;

public final class NativeLibrary {
  public NativeLibrary() { }

  private static native void getWindowArea_internal(String titleMustInclude, String taboo[], int result[]);

  public static Rectangle getWindowArea(String titleMustInclude) {
    int result[] = new int[4];

    List<String> taboo = new ArrayList<String>();
    taboo.add("Save");
    taboo.add("Open");
    String localized;
    if ((localized = UIManager.getString("FileChooser.saveDialogTitleText")) != null)
      taboo.add(localized);
    if ((localized = UIManager.getString("FileChooser.openDialogTitleText")) != null)
      taboo.add(localized);

    getWindowArea_internal(titleMustInclude, taboo.toArray(new String[0]), result);
    return new Rectangle(result[0], result[1], result[2], result[3]);
  }
}
