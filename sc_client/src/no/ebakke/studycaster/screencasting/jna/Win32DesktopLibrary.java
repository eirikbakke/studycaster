package no.ebakke.studycaster.screencasting.jna;

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
import no.ebakke.studycaster.screencasting.jna.MyUser32.LastInputInfo;

/** Platform-dependent window detector. */
public final class Win32DesktopLibrary implements DesktopLibrary {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  private final MyUser32   user32;
  private final MyKernel32 kernel32;
  private static final int STR_BUF_SZ = 32768;

  private Win32DesktopLibrary() {
    user32   = (MyUser32)   Native.loadLibrary("user32"  , MyUser32.class  );
    kernel32 = (MyKernel32) Native.loadLibrary("kernel32", MyKernel32.class);
    // Fail-fast by exercising all library calls.
    getWindowList();
    getTimeSinceLastInputMillis();
  }

  public static DesktopLibrary create() {
    try {
      return new Win32DesktopLibrary();
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
    final HWND foreground = user32.GetForegroundWindow();
    final List<WindowInfo> ret = new ArrayList<WindowInfo>();
    user32.EnumWindows(new WinUser.WNDENUMPROC() {
      public boolean callback(HWND hWnd, Pointer pntr) {
        if (!user32.IsWindowVisible(hWnd))
          return true;
        // Add in reverse order.
        ret.add(0, new WindowInfo(getWindowBounds(hWnd), getWindowTitle(hWnd), getWindowPID(hWnd),
            hWnd.equals(foreground)));
        return true;
      }
    }, null);

    // Don't include the desktop window.
    // TODO: Verify that "Program Manager" is the right name in other Windows versions than XP.
    if (!ret.isEmpty() && ret.get(0).getTitle().equals("Program Manager"))
      ret.remove(0);
    return ret;
  }

  private Rectangle getWindowBounds(HWND hWnd) {
    RECT ret = new RECT();
    user32.GetWindowRect(hWnd, ret);
    return new Rectangle(ret.left, ret.top, ret.right - ret.left, ret.bottom - ret.top);
  }

  private String getWindowTitle(HWND hWnd) {
    /* Could use GetWindowTextLength() here, but there would be a race condition
    in any case, so don't bother. */
    char ret[] = new char[STR_BUF_SZ];
    // This seems to work even with Chinese or other wide characters.
    // Note: JNA's own User32 class defines this method as GetWindowText()
    user32.GetWindowTextW(hWnd, ret, ret.length);
    return Native.toString(ret);
  }

  private int getWindowPID(HWND hWnd) {
    IntByReference ret = new IntByReference();
    user32.GetWindowThreadProcessId(hWnd, ret);
    return ret.getValue();
  }

  private int getLastInputInfo() {
    // See http://ochafik.com/blog/?p=98 .
    LastInputInfo ret = new MyUser32.LastInputInfo();
    user32.GetLastInputInfo(ret);
    return ret.dwTime;
  }

  public long getTimeSinceLastInputMillis() {
    final long UINT_WRAP = 4294967296L; // 2 ** 32
    // This value wraps around every 49.7 days (every UINT_WRAP milliseconds).
    long last = getLastInputInfo();
    // This value "never" wraps around.
    long now  = kernel32.GetTickCount64();

    // This should never happen, even with wraparound on last.
    if (now < last)
      return 0;
    /* This will handle wraparound on last as long as the _difference_ between the two times are no
    more than the wraparound time. */
    return (now - last) % UINT_WRAP;
  }

  public static void main(String args[]) throws InterruptedException {
    DesktopLibrary windowEnumerator = create();
    for (WindowInfo wi : windowEnumerator.getWindowList())
      System.out.println(wi);
    while (true) {
      System.out.println(windowEnumerator.getTimeSinceLastInputMillis());
      Thread.sleep(1000);
    }
  }
}
