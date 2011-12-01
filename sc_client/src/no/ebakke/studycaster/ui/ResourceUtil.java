package no.ebakke.studycaster.ui;

import java.awt.Canvas;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URL;

public final class ResourceUtil {
  private static final String RESOURCE_DIR = "no/ebakke/studycaster/resources/";

  private ResourceUtil() { }

  private static InputStream getResource(final String fileName) throws IOException {
    InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(
        RESOURCE_DIR + fileName);
    if (is == null) {
      throw new FileNotFoundException(
          "Could not locate resource \"" + fileName + "\"");
    }
    return is;
  }

  public static Font createFont(final String fileName, final float size) throws IOException {
    try {
      return Font.createFont(Font.TRUETYPE_FONT, getResource(fileName)).deriveFont(size);
    } catch (FontFormatException e) {
      throw new IOException("Could not load font; " + e.getMessage());
    }
  }

  public static Image loadImage(final String fileName, boolean wait) throws IOException {
    /* Do this to properly catch an error which may otherwise cause waitForAll() below to hang with
    an "Uncaught error fetching image" output. */
    //getResource(fileName).close();
    final URL pointerImageURL = Thread.currentThread().getContextClassLoader().getResource(
        RESOURCE_DIR + fileName);
    final Image ret = Toolkit.getDefaultToolkit().getImage(pointerImageURL);
    if (wait) {
      final MediaTracker mt = new MediaTracker(new Canvas());
      mt.addImage(ret, 0);
      try {
        mt.waitForAll();
      } catch (InterruptedException e) {
        throw new InterruptedIOException();
      }
    }
    return ret;
  }
}
