package no.ebakke.studycaster.screencasting;
import no.ebakke.studycaster.api.NativeLibrary;
import java.awt.Rectangle;
import java.util.List;
import java.util.logging.Level;
import no.ebakke.studycaster.api.StudyCaster;
import no.ebakke.studycaster.api.StudyCasterException;

public class ScreenCensor {
  public static final int MOSAIC_WIDTH = 5;
  private List<String> titleMustInclude;
  private boolean excludeFileDialogs;
  private boolean nativeFail;

  public ScreenCensor(List<String> titleMustInclude, boolean excludeFileDialogs) throws StudyCasterException {
    try {
      NativeLibrary.initialize();
    } catch (Exception e) {
      StudyCaster.log.log(Level.WARNING, "Can't initialize native library; censoring entire screen area", e);
      nativeFail = true;
    }
    this.titleMustInclude = titleMustInclude;
    this.excludeFileDialogs = excludeFileDialogs;
  }

  public Rectangle getPermittedRecordingArea() {
    if (nativeFail)
      return new Rectangle();
    return NativeLibrary.getPermittedRecordingArea(titleMustInclude, excludeFileDialogs);
  }
}
