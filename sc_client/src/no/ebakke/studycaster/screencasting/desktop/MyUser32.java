package no.ebakke.studycaster.screencasting.desktop;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;

public interface MyUser32 extends StdCallLibrary {
  @SuppressWarnings("PublicField")
  public static class LastInputInfo extends Structure {
    public int cbSize = 8;
    public int dwTime;
  }
  public boolean GetLastInputInfo(LastInputInfo result);
  public boolean EnumWindows(WNDENUMPROC lpEnumFunc, Pointer data);
  public boolean IsWindowVisible(HWND hWnd);
  public int GetClassNameW(HWND hWnd, char[] lpClassName, int nMaxCount);
  public int GetWindowTextW(HWND hWnd, char[] lpString, int nMaxCount);
  public DWORD GetWindowThreadProcessId(HWND hWnd, IntByReference lpdwProcessId);
  public boolean AttachThreadInput(DWORD idAttach, DWORD idAttachTo, boolean fAttach);
  public boolean GetWindowRect(HWND hWnd, RECT rect);
  public HWND GetForegroundWindow();
  public HWND GetFocus();
}
