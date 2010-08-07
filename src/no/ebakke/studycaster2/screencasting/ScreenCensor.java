package no.ebakke.studycaster2.screencasting;
import java.awt.Rectangle;
import java.util.List;
import no.ebakke.studycaster.StudyCasterException;
import no.ebakke.studycaster2.NativeLibrary;

public class ScreenCensor {
  public static final int MOSAIC_WIDTH = 5;
  private List<String> titleMustInclude;
  private boolean excludeFileDialogs;

  public ScreenCensor(List<String> titleMustInclude, boolean excludeFileDialogs) throws StudyCasterException {
    NativeLibrary.initialize();
    this.titleMustInclude = titleMustInclude;
    this.excludeFileDialogs = excludeFileDialogs;
  }

  public Rectangle getPermittedRecordingArea() {
    return NativeLibrary.getPermittedRecordingArea(titleMustInclude, excludeFileDialogs);
  }
}
