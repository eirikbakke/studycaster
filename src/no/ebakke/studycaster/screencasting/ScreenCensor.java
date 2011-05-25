package no.ebakke.studycaster.screencasting;
import no.ebakke.studycaster.api.NativeLibrary;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.swing.UIManager;
import no.ebakke.studycaster.api.StudyCaster;
import no.ebakke.studycaster.api.StudyCasterException;

public class ScreenCensor {
  public static final int MOSAIC_WIDTH = 5;
  private List<String> whiteList, blackList;
  private boolean nativeFail;

  public ScreenCensor(List<String> whiteList, List<String> blackList, boolean excludeFileDialogs)
      throws StudyCasterException
  {
    try {
      NativeLibrary.initialize();
    } catch (Exception e) {
      StudyCaster.log.log(Level.WARNING,
          "Can't initialize native library; censoring entire screen area", e);
      nativeFail = true;
    }
    this.whiteList = new ArrayList<String>(whiteList);
    this.blackList = new ArrayList<String>(blackList);
    if (excludeFileDialogs) {
      this.blackList.add("Save");
      this.blackList.add("Open");
      String localized;
      if ((localized = UIManager.getString("FileChooser.saveDialogTitleText")) != null)
        this.blackList.add(localized);
      if ((localized = UIManager.getString("FileChooser.openDialogTitleText")) != null)
        this.blackList.add(localized);
    }
  }

  public Rectangle getPermittedRecordingArea() {
    if (nativeFail)
      return new Rectangle();
    return NativeLibrary.getPermittedRecordingArea(whiteList, blackList);
  }
}
