package no.ebakke.studycaster.screencasting.desktop;

import java.util.List;

/** Must be thread-safe. */
public interface DesktopLibrary {
  /** Return a list of visible top-level windows on the screen, ordered rear-to-front. Windows that
  are completely obscured by other windows may still be included despite not being literally
  visible. Do not include the bottom desktop "window", if any. */
  public List<WindowInfo> getTopLevelWindows();

  /** Get the window currently in focus. This is not necessarily a top-level window; it could be,
  for instance, a text field in a dialog box. */
  public WindowInfo getFocusWindow();

  /** Returns the time, in nanoseconds, of the last desktop-level input event, that is, mouse or
  keyboard input from the user. The time base is such that
  (System.nanoTime() - getLastInputTimeNanos()) will yield the time since the last input event. */
  public long getLastInputTimeNanos();
}
