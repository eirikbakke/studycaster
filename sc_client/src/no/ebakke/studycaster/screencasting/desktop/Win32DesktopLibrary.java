package no.ebakke.studycaster.screencasting.desktop;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.IntByReference;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.ebakke.studycaster.screencasting.desktop.MyUser32.LastInputInfo;

/* TODO: Move complexity out of the platform-specific classes, for instance by handling time base
         conversion in a platform-independent helper class, and by guaranteeing methods in this
         class will all be from the same thread. */
/** Microsoft Windows implementation of the DesktopLibrary. */
public final class Win32DesktopLibrary implements DesktopLibrary {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  private static final boolean SIMULATE_WRAPAROUND = false;
  // 2^32ms * 1000000ns/ms = 49.7 days
  private static final long WRAP_NANOS = 4294967296L * 1000000L;
  // GetClassName got confused when using 32768 or more, so go one step lower.
  private static final int  STR_BUF_SZ = 16384;
  private final long        DEBUG_OFFSET;
  private final MyUser32    user32;
  private final MyKernel32  kernel32;
  private Long              lastInputKernelTimeNanos;
  private long              lastInputJavaTimeNanos = 0;
  private final Map<DWORD,DWORD> attachedInputThreads = new ConcurrentHashMap<DWORD,DWORD>();

  private void detachExistingThreadInput(DWORD idAttach) {
    if (idAttach == null)
      throw new NullPointerException();
    final DWORD idAttachToExisting = attachedInputThreads.remove(idAttach);
    if (idAttachToExisting == null)
      return;
    // Ignore failed detaches; normal for instance when a process exits.
    user32.AttachThreadInput(idAttach, idAttachToExisting, false);
  }

  private void detachExistingThreadInput() {
    detachExistingThreadInput(kernel32.GetCurrentThreadId());
  }

  private boolean attachThreadInput(DWORD idAttachTo) {
    if (idAttachTo == null)
      throw new NullPointerException();
    final DWORD idAttach = kernel32.GetCurrentThreadId();
    if (idAttach == null)
      throw new NullPointerException();
    /* TODO: Avoid attaching immediately when a window gains focus, in case the user is trying to
             double-click it (e.g. to close or maximize it). */
    /* Avoid a high frequency of unnecessary attach/detach cycles, as this is known to interfere
    with double-clicking. */
    final DWORD idAttachToExisting = attachedInputThreads.get(idAttach);
    if (idAttachToExisting != null && idAttachTo.equals(idAttachToExisting))
      return true;
    detachExistingThreadInput();
    if (!user32.AttachThreadInput(idAttach, idAttachTo, true))
      return false;
    attachedInputThreads.put(idAttach, idAttachTo);
    return true;
  }

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
    getFocusWindow();
    getTopLevelWindows();
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

  private WindowInfo createWindowInfo(HWND hWnd, HWND foreground) {
    return new WindowInfo(getWindowBounds(hWnd), getWindowTitle(hWnd), getClassName(hWnd),
        getWindowPID(hWnd), hWnd.equals(foreground));
  }

  /** May return null. */
  public WindowInfo getFocusWindow() {
    final HWND foreground = user32.GetForegroundWindow();
    if (foreground == null)
      return null;
    if (!attachThreadInput(user32.GetWindowThreadProcessId(foreground, null)))
      return null;
    HWND focusWindow = user32.GetFocus();
    if (focusWindow == null)
      return null;
    return createWindowInfo(focusWindow, foreground);
  }

  /* See http://stackoverflow.com/questions/4478624 . */
  public List<WindowInfo> getTopLevelWindows() {
    final HWND foreground = user32.GetForegroundWindow();
    final List<WindowInfo> ret = new ArrayList<WindowInfo>();
    user32.EnumWindows(new WinUser.WNDENUMPROC() {
      public boolean callback(HWND hWnd, Pointer pntr) {
        if (!user32.IsWindowVisible(hWnd))
          return true;
        // Add in reverse order.
        ret.add(0, createWindowInfo(hWnd, foreground));
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
    // TODO: Consider using W32APIOptions.DEFAULT_OPTIONS instead of naming the W suffix here.
    user32.GetWindowTextW(hWnd, ret, ret.length);
    return Native.toString(ret);
  }

  private String getClassName(HWND hWnd) {
    /* "The maximum length for lpszClassName is 256", and GetClassNameW had trouble with buffers of
    size 32768 and above, so set a small buffer here to be safe. See
    http://msdn.microsoft.com/en-us/library/windows/desktop/ms633577(v=vs.85).aspx . */
    char ret[] = new char[1024];
    user32.GetClassNameW(hWnd, ret, ret.length);
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
    // Note: GetTickCount64 does not exist in Windows XP.
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

  public void close() {
    for (DWORD idAttach : attachedInputThreads.keySet())
      detachExistingThreadInput(idAttach);
  }

  public static void main(String args[]) throws InterruptedException {
    DesktopLibrary desktopLibrary = create();
    boolean testFocus = false;
    do {
      System.out.println("Focus: " + desktopLibrary.getFocusWindow());
    } while (testFocus);
    for (WindowInfo wi : desktopLibrary.getTopLevelWindows())
      System.out.println(wi);
    long timeBefore = System.nanoTime();
    final long ITERATIONS = 1000;
    for (int i = 0; i < ITERATIONS; i++) {
      if (i % 100 == 0)
        System.out.println(i);
      desktopLibrary.getTopLevelWindows();
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
    desktopLibrary.close();
  }
}
