package no.ebakke.studycaster2;

import java.awt.Rectangle;

public final class NativeLibrary {
  public NativeLibrary() { }

  public static native void getWindowArea_internal(String titleMustInclude, int result[]);

  public static Rectangle getWindowArea(String titleMustInclude) {
    int result[] = new int[4];
    getWindowArea_internal(titleMustInclude, result);
    if (result[0] == 0 && result[1] == 0 && result[2] == 0 && result[3] == 0) {
      return null;
    } else {
      return new Rectangle(result[0], result[1], result[2], result[3]);
    }
  }
}
