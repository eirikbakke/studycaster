package no.ebakke.studycaster.screencasting.desktop;

import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.win32.StdCallLibrary;

public interface MyKernel32 extends StdCallLibrary {
  public int GetTickCount();
  public DWORD GetCurrentThreadId();
}
