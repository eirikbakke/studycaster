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
import no.ebakke.studycaster.backend.StudyCasterException;
import no.ebakke.studycaster.configuration.UIStringKey;
import no.ebakke.studycaster.configuration.UIStrings;
import no.ebakke.studycaster.screencasting.Quilt.ValueRun;
import no.ebakke.studycaster.screencasting.WindowEnumerator.WindowInfo;
import no.ebakke.studycaster.util.ImageDebugFrame;

/** Not thread-safe. */
public final class ScreenCensor {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  // TODO: Make this configurable.
  public static final int MOSAIC_WIDTH = 5;

  private final List<String> whitelist, blacklist;
  private final Quilt<CensorType> nativeFail;
  private final WindowEnumerator windowEnumerator;
  private final boolean blackoutDesktop;

  @SuppressWarnings("NestedAssignment")
  public ScreenCensor(List<String> whiteList, List<String> blackList, boolean blacklistFileDialogs,
      boolean whiteListStudyCasterDialogs, boolean blackoutDesktop)
      throws StudyCasterException
  {
    this.whitelist       = new ArrayList<String>(whiteList);
    this.blacklist       = new ArrayList<String>(blackList);
    this.blackoutDesktop = blackoutDesktop;
    if (blacklistFileDialogs) {
      this.blacklist.add("Save");
      this.blacklist.add("Open");
      this.blacklist.add("Browse");
      String localized;
      if ((localized = UIManager.getString("FileChooser.saveDialogTitleText")) != null)
        this.blacklist.add(localized);
      if ((localized = UIManager.getString("FileChooser.openDialogTitleText")) != null)
        this.blacklist.add(localized);
    }
    if (whiteListStudyCasterDialogs)
      this.whitelist.add("StudyCaster");
    windowEnumerator = Win32WindowEnumerator.create();
    if (windowEnumerator == null) {
      LOG.log(Level.WARNING, "Can't initialize native library; applying mosaic to entire screen");
      nativeFail = new Quilt<CensorType>(CensorType.MOSAIC);
    } else {
      nativeFail = null;
    }
  }

  public Quilt<CensorType> getPermittedRecordingArea() {
    if (nativeFail != null)
      return nativeFail;

    List<WindowInfo> windows = windowEnumerator.getWindowList();
    Set<Integer> pidWhiteList = new LinkedHashSet<Integer>();
    for (WindowInfo wi : windows) {
      for (String whiteListItem : whitelist) {
        if (wi.getTitle().toLowerCase().contains(whiteListItem.toLowerCase()))
          pidWhiteList.add(wi.getPID());
      }
    }
    Quilt<CensorType> ret = new Quilt<CensorType>(
        blackoutDesktop ? CensorType.BLACKOUT : CensorType.MOSAIC);
    for (WindowInfo wi : windows) {
      boolean ok = pidWhiteList.contains(wi.getPID());
      if (ok) {
        for (String blackListItem : blacklist) {
          if (wi.getTitle().toLowerCase().contains(blackListItem.toLowerCase())) {
            ok = false;
            break;
          }
        }
      }
      ret.addPatch(wi.getBounds(), ok ? CensorType.NONE : CensorType.MOSAIC);
    }
    return ret;
  }

  public static void main(String args[]) throws StudyCasterException, AWTException {
    ScreenCensor censor = new ScreenCensor(
      Arrays.asList(new String[] {"Excel", "Calc", "Numbers", "Gnumeric", "KSpread", "Quattro", "Mesa"}),
      Arrays.asList(new String[] {"Firefox", "Internet Explorer", "Outlook", "Chrome", "Safari"}),
      true, true, true);
    Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    BufferedImage image = new Robot().createScreenCapture(screenRect);
    Quilt<CensorType> permitted = censor.getPermittedRecordingArea();
    
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
        int        censorRunLength = 0;
        CensorType censorRunType   = null;
        for (int x = 0; x < image.getWidth(); x++) {
          if (censorRunLength == 0) {
            ValueRun<CensorType> censorRun = permitted.getPatchRun(x, y);
            censorRunType   = censorRun.getValue();
            censorRunLength = censorRun.getRunLength();
          }
          //permitted.contains(x, y);
          //screenRect.contains(x, y);
          if        (censorRunType == CensorType.MOSAIC) {
            image.setRGB(x, y, image.getRGB(x, y) & 0xE0E00000);
          } else if (censorRunType == CensorType.BLACKOUT) {
            image.setRGB(x, y, image.getRGB(x, y) & 0x0000E000);
          }
          /*
          if (!permitted.contains(x, y))
            image.setRGB(x, y, image.getRGB(x, y) & 0xE0E00000);
          */
          censorRunLength--;
        }
      }
    }
    ImageDebugFrame.showImage(image);
  }

  public static enum CensorType {
    NONE,
    MOSAIC,
    BLACKOUT
  }
}
