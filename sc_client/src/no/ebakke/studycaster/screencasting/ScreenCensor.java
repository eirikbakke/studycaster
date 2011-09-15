package no.ebakke.studycaster.screencasting;
import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.swing.UIManager;
import no.ebakke.studycaster.api.StudyCaster;
import no.ebakke.studycaster.api.StudyCasterException;
import no.ebakke.studycaster.screencasting.WindowEnumerator.WindowInfo;
import no.ebakke.studycaster.util.ImageDebugDialog;

public final class ScreenCensor {
  public static final int MOSAIC_WIDTH = 5;

  private List<String> whiteList, blackList;
  private Quilt nativeFail;
  private WindowEnumerator windowEnumerator;

  public ScreenCensor(List<String> whiteList, List<String> blackList, boolean excludeFileDialogs)
      throws StudyCasterException
  {
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
    windowEnumerator = Win32WindowEnumerator.create();
    if (windowEnumerator == null) {
      StudyCaster.log.log(Level.WARNING,
          "Can't initialize native library; censoring entire screen area");
      nativeFail = new Quilt();
    }
  }

  public Quilt getPermittedRecordingArea() {
    if (nativeFail != null)
      return nativeFail;

    List<WindowInfo> windows = windowEnumerator.getWindowList();
    Set<Integer> pidWhiteList = new LinkedHashSet<Integer>();
    for (WindowInfo wi : windows) {
      for (String whiteListItem : whiteList) {
        if (wi.title.toLowerCase().contains(whiteListItem.toLowerCase()))
          pidWhiteList.add(wi.pid);
      }
    }
    Quilt ret = new Quilt();
    for (WindowInfo wi : windows) {
      boolean ok = pidWhiteList.contains(wi.pid);
      if (ok) {
        for (String blackListItem : blackList) {
          if (wi.title.toLowerCase().contains(blackListItem.toLowerCase())) {
            ok = false;
            break;
          }
        }
      }
      ret.addPatch(wi.location, ok);
    }
    return ret;
  }

  public static void main(String args[]) throws StudyCasterException, AWTException {
    ScreenCensor censor = new ScreenCensor(
      Arrays.asList(new String[] {"User Study Console", "Excel", "Calc", "Numbers", "Gnumeric", "KSpread", "Quattro", "Mesa", "Spreadsheet Study Application"}),
      Arrays.asList(new String[] {"Firefox", "Internet Explorer", "Outlook", "Chrome", "Safari", "Upload and Retrieve Confirmation Code",
      "Open Sample Document"}),
      true);
    Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    BufferedImage image = new Robot().createScreenCapture(screenRect);
    Quilt permitted = censor.getPermittedRecordingArea();
    
    permitted = new Quilt();
    permitted.addPatch(screenRect, true);
    permitted.addPatch(new Rectangle(100, 100, 800, 600), false);
    permitted.addPatch(new Rectangle(400, 150, 300, 200), true);

    // Profiling code below.
    for (int i = 0; i < 100; i++) {
      System.out.println(i);
      for (int y = 0; y < image.getHeight(); y++) {
        int     censorRun        = 0;
        boolean censorRunAllowed = false;
        for (int x = 0; x < image.getWidth(); x++) {
          if (censorRun == 0) {
            censorRun = permitted.getHorizontalRunLength(x, y);
            if (censorRun > 0) {
              censorRunAllowed = true;
            } else {
              censorRunAllowed = false;
              censorRun        = -censorRun;
            }
          }
          //permitted.contains(x, y);
          //screenRect.contains(x, y);
          if (!censorRunAllowed)
            image.setRGB(x, y, image.getRGB(x, y) & 0xE0E00000);
          /*
          if (!permitted.contains(x, y))
            image.setRGB(x, y, image.getRGB(x, y) & 0xE0E00000);
          */
          censorRun--;
        }
      }
    }
    ImageDebugDialog.showImage(image);
  }
}
