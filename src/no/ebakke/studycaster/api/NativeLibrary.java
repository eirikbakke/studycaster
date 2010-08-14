package no.ebakke.studycaster.api;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public final class NativeLibrary {
  private NativeLibrary() { }
  private static boolean initialized = false;

  // TODO: Change this interface to move more functionarily out of the native library (retrieve a window list with titles and coordinates instead).
  private static native void getPermittedRecordingArea_internal(String whiteList[], String blackList[], int result[]);

  public static Rectangle getPermittedRecordingArea(List<String> whiteList, List<String> blackList) {
    if (!initialized)
      throw new IllegalStateException("Native library not yet initialized");
    int result[] = new int[4];
    getPermittedRecordingArea_internal(whiteList.toArray(new String[0]), blackList.toArray(new String[0]), result);
    return new Rectangle(result[0], result[1], result[2], result[3]);
  }

  public static void initialize() throws StudyCasterException {
    if (initialized)
      return;
    try {
      System.loadLibrary("libSCNative");
      initialized = true;
      NativeLibrary.getPermittedRecordingArea(new ArrayList<String>(), new ArrayList<String>());
    } catch (Exception e) {
      throw new StudyCasterException("Can't initialize window position detector library", e);
    } catch (UnsatisfiedLinkError e) {
      throw new StudyCasterException("Can't initialize window position detector library", e);
    }
  }
}
