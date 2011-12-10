package no.ebakke.studycaster.screencasting.jna;

import com.sun.jna.win32.StdCallLibrary;

public interface MyKernel32 extends StdCallLibrary {
  public int GetTickCount();
}
