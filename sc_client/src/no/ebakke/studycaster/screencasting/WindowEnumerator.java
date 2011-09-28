package no.ebakke.studycaster.screencasting;

import java.awt.Rectangle;
import java.util.List;

public interface WindowEnumerator {
  /** Return information about visible windows ordered rear-to-front. */
  public List<WindowInfo> getWindowList();

  public static final class WindowInfo {
    private Rectangle location;
    private String    title;
    private int       pid;

    public WindowInfo(Rectangle location, String title, int pid) {
      this.location = location;
      this.title    = title;
      this.pid      = pid;
    }

    public Rectangle getLocation() {
      return location;
    }

    public String getTitle() {
      return title;
    }

    public int getPID() {
      return pid;
    }

    @Override
    public String toString() {
      return pid + "\t" + location + "\t" + "\"" + title + "\"";
    }
  }
}
