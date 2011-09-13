package no.ebakke.studycaster.screencasting;

import java.awt.Rectangle;
import java.util.List;

public interface WindowEnumerator {
  /** Return information about visible windows ordered rear-to-front. */
  public List<WindowInfo> getWindowList();

  public final class WindowInfo {
    public Rectangle location;
    public String    title;
    public int       pid;

    public WindowInfo(Rectangle location, String title, int pid) {
      this.location = location;
      this.title    = title;
      this.pid      = pid;
    }

    @Override
    public String toString() {
      return pid + "\t" + location + "\t" + "\"" + title + "\"";
    }
  }
}
