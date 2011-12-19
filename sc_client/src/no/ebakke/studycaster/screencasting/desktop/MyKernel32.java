package no.ebakke.studycaster.screencasting.desktop;

import com.sun.jna.win32.StdCallLibrary;

public interface MyKernel32 extends StdCallLibrary {
  public int GetTickCount();
}
