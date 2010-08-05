package no.ebakke.studycaster.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import no.ebakke.studycaster.StudyCaster;

public class Util {
  private static int BUF_SIZE = 8192;

  // TODO: Consider whether this should rather be in the StudyCaster class.
  public static boolean fileAvailableExclusive(File f) {
    try {
      FileOutputStream exclusive = new FileOutputStream(f, true);
      exclusive.close();
    } catch (FileNotFoundException e) {
      return false;
    } catch (IOException e) {
      return false;
    }
    return true;
  }

  public static String getPathTo(File f) {
    try {
      return f.getCanonicalPath();
    } catch (IOException e) {
      StudyCaster.log.log(Level.WARNING, "Couldn't get canonical path.", e);
      return f.getAbsolutePath();
    }
  }
}
