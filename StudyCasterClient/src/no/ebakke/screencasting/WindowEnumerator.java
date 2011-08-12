package no.ebakke.screencasting;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;

/* See http://stackoverflow.com/questions/4478624 . */
public class WindowEnumerator {
  public static void test() {
    User32.INSTANCE.EnumWindows(new WinUser.WNDENUMPROC() {
      public boolean callback(HWND hWnd, Pointer pntr) {
        if (!User32.INSTANCE.IsWindowVisible(hWnd))
          return true;
        char lpString[] = new char[32768];
        User32.INSTANCE.GetWindowText(hWnd, lpString, lpString.length);
        String windowTitle = Native.toString(lpString);
        System.out.println("window: \"" + windowTitle + "\"");
        return true;
      }
    }, null);
  }

  public static void main(String[] args) {
    test();
  }
}
