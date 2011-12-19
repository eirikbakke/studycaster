package no.ebakke.studycaster.screencasting.desktop;

import java.util.ArrayList;
import java.util.List;

/** Immutable. */
public final class DesktopMeta {
  final long timeNanos;
  final List<WindowInfo> windowList;
  final long lastUserInputNanos;

  public DesktopMeta(long timeNanos, List<WindowInfo> windowList, long lastUserInputNanos) {
    this.timeNanos = timeNanos;
    this.windowList = new ArrayList<WindowInfo>(windowList);
    this.lastUserInputNanos = lastUserInputNanos;
  }

  public long getTimeNanos() {
    return timeNanos;
  }

  public List<WindowInfo> getWindowList() {
    return new ArrayList<WindowInfo>(windowList);
  }

  public long getLastUserInputNanos() {
    return lastUserInputNanos;
  }
}
