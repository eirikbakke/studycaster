package no.ebakke.studycaster.screencasting;
import no.ebakke.studycaster.screencasting.desktop.Win32DesktopLibrary;
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
import java.util.logging.Logger;
import javax.swing.UIManager;
import no.ebakke.studycaster.backend.StudyCasterException;
import no.ebakke.studycaster.screencasting.Quilt.ValueRun;
import no.ebakke.studycaster.screencasting.desktop.DesktopLibrary;
import no.ebakke.studycaster.screencasting.desktop.WindowInfo;
import no.ebakke.studycaster.ui.UIUtil;
import no.ebakke.studycaster.util.ImageDebugFrame;

/** Not thread-safe. */
public final class ScreenCensor {
  public static final Quilt<CensorType> NO_CENSOR  = new Quilt<CensorType>(CensorType.NONE);
  public static final Quilt<CensorType> ALL_MOSAIC = new Quilt<CensorType>(CensorType.MOSAIC);
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  // TODO: Make this configurable.
  public static final int MOSAIC_WIDTH = 5;

  private final List<String> whitelist, blacklist;
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
      try {
        this.blacklist.addAll(
            UIUtil.swingBlock(new UIUtil.CallableExt<List<String>,RuntimeException>()
        {
          public List<String> call() {
            List<String> ret = new ArrayList<String>();
            String localized;
            if ((localized = UIManager.getString("FileChooser.saveDialogTitleText")) != null)
              ret.add(localized);
            if ((localized = UIManager.getString("FileChooser.openDialogTitleText")) != null)
              ret.add(localized);
            return ret;
          }
        }));
      } catch (InterruptedException e) {
        throw new StudyCasterException(e);
      }
    }
    if (whiteListStudyCasterDialogs)
      this.whitelist.add("StudyCaster");
  }

  public Quilt<CensorType> getPermittedRecordingArea(List<WindowInfo> windowList) {
    Set<Integer> pidWhiteList = new LinkedHashSet<Integer>();
    for (WindowInfo wi : windowList) {
      for (String whiteListItem : whitelist) {
        if (wi.getTitle().toLowerCase().contains(whiteListItem.toLowerCase()))
          pidWhiteList.add(wi.getPID());
      }
    }
    Quilt<CensorType> ret = new Quilt<CensorType>(
        blackoutDesktop ? CensorType.BLACKOUT : CensorType.MOSAIC);
    for (WindowInfo wi : windowList) {
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
    DesktopLibrary windowEnumerator = Win32DesktopLibrary.create();
    if (windowEnumerator == null)
      throw new StudyCasterException("Can't load window library");
    Quilt<CensorType> permitted = censor.getPermittedRecordingArea(windowEnumerator.getWindowList());

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
