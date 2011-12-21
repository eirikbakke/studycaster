package no.ebakke.studycaster.screencasting.desktop;

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
import no.ebakke.studycaster.screencasting.desktop.MyUser32.LastInputInfo;

/** Microsoft Windows implementation of the DesktopLibrary. */
public final class Win32DesktopLibrary implements DesktopLibrary {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  private static boolean    SIMULATE_WRAPAROUND = false;
  // 2^32ms * 1000000ns/ms = 49.7 days
  private static final long WRAP_NANOS = 4294967296L * 1000000L;
  private static final int  STR_BUF_SZ = 32768;
  private final long        DEBUG_OFFSET;
  private final MyUser32    user32;
  private final MyKernel32  kernel32;
  private Long              lastInputKernelTimeNanos;
  private long              lastInputJavaTimeNanos = 0;

  private Win32DesktopLibrary() {
    user32   = (MyUser32)   Native.loadLibrary("user32"  , MyUser32.class  );
    kernel32 = (MyKernel32) Native.loadLibrary("kernel32", MyKernel32.class);
    if (SIMULATE_WRAPAROUND) {
      LOG.info("Simulating Win32 tick wraparound in 10.3 seconds");
      DEBUG_OFFSET = WRAP_NANOS - kernel32.GetTickCount() * 1000000L - 10300 * 1000000L;
    } else {
      DEBUG_OFFSET = 0;
    }
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

  private long toSigned(int uint) {
    return ((long) uint) & 0x00000000FFFFFFFFL;
  }

  /** Wraps around at WRAP_NANOS. */
  private long getLastInputKernelTimeNanos() {
    // See http://ochafik.com/blog/?p=98 .
    LastInputInfo ret = new MyUser32.LastInputInfo();
    user32.GetLastInputInfo(ret);
    return (toSigned(ret.dwTime) * 1000000L + DEBUG_OFFSET) % WRAP_NANOS;
  }

  /** Wraps around at WRAP_NANOS. */
  private long getKernelTimeNanos() {
    return (toSigned(kernel32.GetTickCount()) * 1000000L + DEBUG_OFFSET) % WRAP_NANOS;
  }

  private long getJavaTimeNanos() {
    return System.nanoTime();
  }

  public synchronized long getLastInputTimeNanos() {
    final long lastKernel = getLastInputKernelTimeNanos();
    /* Only return a new value if GetLastInputInfo is returning a new value as well (clock jitter
    could otherwise make the result slightly off from the previous time). */
    if (lastInputKernelTimeNanos == null || lastInputKernelTimeNanos != lastKernel) {
      lastInputKernelTimeNanos = lastKernel;

      final long nowJava    = getJavaTimeNanos();
      final long nowKernel  = getKernelTimeNanos();
      /* This will handle wraparound of GetLastInputInfo as long as the _difference_ between the
      two times are no more than the wraparound time. */
      final long sinceLast    = ((nowKernel + WRAP_NANOS) - lastKernel) % WRAP_NANOS;
      final long adjustedLast = nowKernel - sinceLast;

      /* The kernel and Java timers may not always be perfectly in sync, so explicitly constrain
      the return value so that it does not exceed System.nanoTime(). Also constrain the result to
      always increase monotonically between invocations of this method. */
      lastInputJavaTimeNanos =
          Math.max(Math.min(adjustedLast - (nowKernel - nowJava), nowJava), lastInputJavaTimeNanos);
    }
    return lastInputJavaTimeNanos;
  }

  public static void main(String args[]) throws InterruptedException {
    DesktopLibrary desktopLibrary = create();
    for (WindowInfo wi : desktopLibrary.getWindowList())
      System.out.println(wi);
    long timeBefore = System.nanoTime();
    final long ITERATIONS = 1000;
    for (int i = 0; i < ITERATIONS; i++) {
      if (i % 100 == 0)
        System.out.println(i);
      desktopLibrary.getWindowList();
      desktopLibrary.getLastInputTimeNanos();
    }
    long timeAfter = System.nanoTime();
    // Got about 24 milliseconds per call on my laptop.
    System.out.println((timeAfter - timeBefore) / ITERATIONS + "ns per call");
    for (int i = 0; i < 20; i++) {
      long lastJava  = desktopLibrary.getLastInputTimeNanos();
      long nowKernel = ((Win32DesktopLibrary) desktopLibrary).getKernelTimeNanos();
      long nowJava   = ((Win32DesktopLibrary) desktopLibrary).getJavaTimeNanos();
      long since = nowJava - lastJava;
      System.out.println(
          nowKernel / 1000000L + "\t" + lastJava / 1000000L + "\t" + since / 1000000L);
      Thread.sleep(1000);
    }
  }
}