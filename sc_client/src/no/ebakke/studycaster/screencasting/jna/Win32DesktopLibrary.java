package no.ebakke.studycaster.screencasting.jna;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
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

/** Microsoft Windows implementation of the DesktopLibrary. */
public final class Win32DesktopLibrary implements DesktopLibrary {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  private static boolean    SIMULATE_WRAPAROUND = false;
  private static final long UINT_WRAP = 4294967296L; // 2 ** 32
  private static final int  STR_BUF_SZ = 32768;
  private final MyUser32    user32;
  private final MyKernel32  kernel32;
  private final long        kernelTimeOffset;

  private Win32DesktopLibrary() {
    user32   = (MyUser32)   Native.loadLibrary("user32"  , MyUser32.class  );
    kernel32 = (MyKernel32) Native.loadLibrary("kernel32", MyKernel32.class);
    /* Err on the side of overestimating the offset (to avoid negative times since last input), so
    get the Java time first. */
    final long javaTime   = System.nanoTime();
    final long kernelTime = getTickCount() * 1000000L;
    kernelTimeOffset = kernelTime - javaTime;
    // Fail-fast by exercising all library calls.
    getWindowList();
    getLastInputTimeNanos();
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

    /* Don't include the desktop window. "Program Manager" is the correct name on at least
    Windows XP and Windows 7. */
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

  private long getTickCount() {
    return kernel32.GetTickCount64() + (SIMULATE_WRAPAROUND ? UINT_WRAP : 0);
  }

  public long getLastInputTimeNanos() {
    // This value wraps around every 49.7 days (every UINT_WRAP milliseconds).
    long last = getLastInputInfo();
    /* This value "never" wraps around. The only purpose of using GetTickCount here is to use its
    value to handle the wraparound of GetLastInputInfo. */
    long now  = getTickCount();

    /* This will handle wraparound of GetLastInputInfo as long as the _difference_ between the two
    times are no more than the wraparound time. The less-than case should never happen even with
    wraparound, but we handle it anyway. */
    long sinceLast = (now < last) ? 0 : ((now - last) % UINT_WRAP);
    long adjustedLast = now - sinceLast;
    /* The kernel and Java timers may not always be perfectly in sync, so explicitly constrain the
    return value so that it does not exceed System.nanoTime(). */
    return Math.min(adjustedLast * 1000000L - kernelTimeOffset, System.nanoTime());
  }

  public static void main(String args[]) throws InterruptedException {
    DesktopLibrary windowEnumerator = create();
    for (WindowInfo wi : windowEnumerator.getWindowList())
      System.out.println(wi);
    while (true) {
      long last = windowEnumerator.getLastInputTimeNanos();
      long nano = System.nanoTime();
      long since = nano - last;
      System.out.println(last / 1000000L + "\t" + since / 1000000L);
      Thread.sleep(1000);
    }
  }
}
