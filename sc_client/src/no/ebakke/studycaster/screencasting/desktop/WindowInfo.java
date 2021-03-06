package no.ebakke.studycaster.screencasting.desktop;

import java.awt.Rectangle;

/** Immutable. */
public final class WindowInfo {
  private final Rectangle bounds;
  private final String    title;
  private final String    type;
  private final int       pid;
  private final boolean   foreground;

  public WindowInfo(Rectangle bounds, String title, String type, int pid, boolean foreground) {
    if (bounds.width < 0 || bounds.height < 0)
        throw new IllegalArgumentException("Invalid window bounds");
    this.bounds      = new Rectangle(bounds);
    this.title       = title;
    this.type        = type;
    this.pid         = pid;
    this.foreground  = foreground;
  }

  /** Returns the bounds of the window, in other words, where on the screen it is located. */
  public Rectangle getBounds() {
    return new Rectangle(bounds);
  }

  /** Returns the title of the window. */
  public String getTitle() {
    return title;
  }

  /** Returns a platform-dependent string representing the type of the window. For instance, on
  Win32, this would be the "class name" of the window. */
  public String getType() {
    return type;
  }

  /** Returns an identifier for the process that created this window, for the purposes of
  identifying windows belonging to the same process. The nature of the identifier is not further
  specified; it could be the PID from the operating system, or a simple enumeration generated by
  other means in the OS-specific DestkopLibrary implementation. */
  public int getPID() {
    return pid;
  }

  /** Returns whether the window is the foreground window. The foreground window is the window
  currently owning the user focus, and there is typically one foreground window on the screen at
  any given time (there may be exceptions). */
  public boolean isForeground() {
    return foreground;
  }

  @Override
  public String toString() {
    return pid + "\t" + (foreground ? "F" : " ") + "\t" + bounds + "\t[" + type + "] \"" + title + "\"";
  }
}
