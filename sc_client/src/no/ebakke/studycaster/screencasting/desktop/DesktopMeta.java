package no.ebakke.studycaster.screencasting.desktop;

import java.util.ArrayList;
import java.util.List;

/** Immutable. */
public final class DesktopMeta {
  final long timeNanos;
  final List<WindowInfo> windowList;
  final WindowInfo focusWindow;
  final long lastUserInputNanos;

  public DesktopMeta(long timeNanos, List<WindowInfo> windowList, WindowInfo focusWindow,
      long lastUserInputNanos)
  {
    this.timeNanos = timeNanos;
    this.windowList = new ArrayList<WindowInfo>(windowList);
    this.focusWindow = focusWindow;
    this.lastUserInputNanos = lastUserInputNanos;
  }

  public long getTimeNanos() {
    return timeNanos;
  }

  public List<WindowInfo> getWindowList() {
    return new ArrayList<WindowInfo>(windowList);
  }

  /** May be null. */
  public WindowInfo getFocusWindow() {
    return focusWindow;
  }

  public long getLastUserInputNanos() {
    return lastUserInputNanos;
  }
}
