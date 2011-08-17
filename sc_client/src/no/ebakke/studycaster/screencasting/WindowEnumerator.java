package no.ebakke.studycaster.screencasting;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;

/* See http://stackoverflow.com/questions/4478624 . */
public class WindowEnumerator {
  public static String test() {
    final StringBuffer ret = new StringBuffer();
    
    User32.INSTANCE.EnumWindows(new WinUser.WNDENUMPROC() {
      public boolean callback(HWND hWnd, Pointer pntr) {
        if (!User32.INSTANCE.IsWindowVisible(hWnd))
          return true;
        /* Could use GetWindowTextLength() here, but there could be a race
        condition, so don't bother. */
        char lpString[] = new char[32768];
        /* I considered using the underlying GetWindowTextW() instead, but JNA
        doesn't define it; or maybe it is actually the function being called
        somehow. In any case, this seems to work even with Chinese or other
        wide characters. */
        User32.INSTANCE.GetWindowText(hWnd, lpString, lpString.length);
        String windowTitle = Native.toString(lpString);
        ret.append("window: \"" + windowTitle + "\"\n");
        return true;
      }
    }, null);
    return ret.toString();
  }
}
