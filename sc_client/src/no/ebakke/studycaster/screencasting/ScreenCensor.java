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
import java.util.logging.Logger;
import javax.swing.UIManager;
import no.ebakke.studycaster.api.StudyCasterException;
import no.ebakke.studycaster.screencasting.WindowEnumerator.WindowInfo;
import no.ebakke.studycaster.util.ImageDebugFrame;

/** Not thread-safe. */
public final class ScreenCensor {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  // TODO: Make this configurable.
  public static final int MOSAIC_WIDTH = 5;

  private final List<String> whiteList, blackList;
  private final Quilt nativeFail;
  private final WindowEnumerator windowEnumerator;

  public ScreenCensor(List<String> whiteList, List<String> blackList, boolean blacklistFileDialogs,
      boolean whiteListStudyCasterDialogs)
      throws StudyCasterException
  {
    this.whiteList = new ArrayList<String>(whiteList);
    this.blackList = new ArrayList<String>(blackList);
    if (blacklistFileDialogs) {
      // TODO: Take these strings from a common place (below, too).
      this.blackList.add("Select File to Upload");
      this.blackList.add("Open Sample File");
      this.blackList.add("Upload and Retrieve Confirmation Code");
      this.blackList.add("Save");
      this.blackList.add("Open");
      String localized;
      if ((localized = UIManager.getString("FileChooser.saveDialogTitleText")) != null)
        this.blackList.add(localized);
      if ((localized = UIManager.getString("FileChooser.openDialogTitleText")) != null)
        this.blackList.add(localized);
    }
    if (whiteListStudyCasterDialogs)
      this.whiteList.add("StudyCaster");
    windowEnumerator = Win32WindowEnumerator.create();
    if (windowEnumerator == null) {
      LOG.log(Level.WARNING, "Can''t initialize native library; censoring entire screen area");
      nativeFail = new Quilt();
    } else {
      nativeFail = null;
    }
  }

  public Quilt getPermittedRecordingArea() {
    if (nativeFail != null)
      return nativeFail;

    List<WindowInfo> windows = windowEnumerator.getWindowList();
    Set<Integer> pidWhiteList = new LinkedHashSet<Integer>();
    for (WindowInfo wi : windows) {
      for (String whiteListItem : whiteList) {
        if (wi.getTitle().toLowerCase().contains(whiteListItem.toLowerCase()))
          pidWhiteList.add(wi.getPID());
      }
    }
    Quilt ret = new Quilt();
    for (WindowInfo wi : windows) {
      boolean ok = pidWhiteList.contains(wi.getPID());
      if (ok) {
        for (String blackListItem : blackList) {
          if (wi.getTitle().toLowerCase().contains(blackListItem.toLowerCase())) {
            ok = false;
            break;
          }
        }
      }
      ret.addPatch(wi.getBounds(), ok);
    }
    return ret;
  }

  public static void main(String args[]) throws StudyCasterException, AWTException {
    ScreenCensor censor = new ScreenCensor(
      Arrays.asList(new String[] {"Excel", "Calc", "Numbers", "Gnumeric", "KSpread", "Quattro", "Mesa"}),
      Arrays.asList(new String[] {"Firefox", "Internet Explorer", "Outlook", "Chrome", "Safari"}),
      true, true);
    Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    BufferedImage image = new Robot().createScreenCapture(screenRect);
    Quilt permitted = censor.getPermittedRecordingArea();
    
    /*
    permitted = new Quilt();
    permitted.addPatch(screenRect, true);
    permitted.addPatch(new Rectangle(100, 100, 800, 600), false);
    permitted.addPatch(new Rectangle(400, 150, 300, 200), true);
    */

    // Profiling code below.
    for (int i = 0; i < 10; i++) {
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
    ImageDebugFrame.showImage(image);
  }
}
