package no.ebakke.studycaster.screencasting;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.IntByReference;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Platform-dependent window detector. */
final class Win32WindowEnumerator implements WindowEnumerator {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  private static int STR_BUF_SZ = 32768;

  private Win32WindowEnumerator() {
    getWindowList();
  }

  public static WindowEnumerator create() {
    try {
      return new Win32WindowEnumerator();
    } catch (UnsatisfiedLinkError e) {
      LOG.log(Level.WARNING, "Screen censor library unavailable", e);
      return null;
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Unknown screen censor library error", e);
      return null;
    }
  }

  /* See http://stackoverflow.com/questions/4478624 . */
  public List<WindowInfo> getWindowList() {
    final List<WindowInfo> ret = new ArrayList<WindowInfo>();
    User32.INSTANCE.EnumWindows(new WinUser.WNDENUMPROC() {
      public boolean callback(HWND hWnd, Pointer pntr) {
        if (!User32.INSTANCE.IsWindowVisible(hWnd))
          return true;
        // Add in reverse order.
        ret.add(0, new WindowInfo(
            getWindowBounds(hWnd), getWindowTitle(hWnd), getWindowPID(hWnd)));
        return true;
      }
    }, null);
    return ret;
  }

  private static Rectangle getWindowBounds(HWND hWnd) {
    RECT ret = new RECT();
    User32.INSTANCE.GetWindowRect(hWnd, ret);
    return new Rectangle(
        ret.left, ret.top, ret.right - ret.left, ret.bottom - ret.top);
  }

  private static String getWindowTitle(HWND hWnd) {
    /* Could use GetWindowTextLength() here, but there would be a race condition
    in any case, so don't bother. */
    char ret[] = new char[STR_BUF_SZ];
    /* I considered using the underlying GetWindowTextW() instead, but JNA
    doesn't define it; or maybe it is actually the function being called
    somehow. In any case, this seems to work even with Chinese or other wide
    characters. */
    User32.INSTANCE.GetWindowText(hWnd, ret, ret.length);
    return Native.toString(ret);
  }

  private static int getWindowPID(HWND hWnd) {
    IntByReference ret = new IntByReference();
    User32.INSTANCE.GetWindowThreadProcessId(hWnd, ret);
    return ret.getValue();
  }

  public static void main(String args[]) {
    for (WindowInfo wi : create().getWindowList())
      System.out.println(wi);
  }
}
