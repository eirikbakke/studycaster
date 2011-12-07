package no.ebakke.studycaster.screencasting.jna;

import java.awt.Rectangle;
import java.util.List;

public interface DesktopLibrary {
  /** Return a list of visible windows on the screen, ordered rear-to-front. Windows that are
  partially or completely obscured by other windows may be included despite not being literally
  visible. Do not include the bottom desktop "window", if any. */
  public List<WindowInfo> getWindowList();

  /** Return the number of milliseconds since the last desktop-level input event (i.e. mouse or
  keyboard) was received from the user. */
  public long getTimeSinceLastInputMillis();

  public static final class WindowInfo {
    private Rectangle bounds;
    private String    title;
    private int       pid;

    public WindowInfo(Rectangle bounds, String title, int pid) {
      this.bounds = bounds;
      this.title  = title;
      this.pid    = pid;
    }

    public Rectangle getBounds() {
      return bounds;
    }

    public String getTitle() {
      return title;
    }

    public int getPID() {
      return pid;
    }

    @Override
    public String toString() {
      return pid + "\t" + bounds + "\t" + "\"" + title + "\"";
    }
  }
}
