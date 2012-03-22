package no.ebakke.studycaster.screencasting.desktop;

import java.util.List;
import no.ebakke.studycaster.backend.TimeSource;

/** Thread-safe. */
public class DesktopMetaFactory {
  private final DesktopLibrary desktopLibrary;
  private final TimeSource     timeSource;

  public DesktopMetaFactory(DesktopLibrary desktopLibrary, TimeSource timeSource) {
    this.desktopLibrary = desktopLibrary;
    this.timeSource     = timeSource;
  }

  public DesktopMeta createMeta() {
    List<WindowInfo> windowList = desktopLibrary.getTopLevelWindows();
    /* Fetch this value before calling currentTimeNanos() so the latter is never smaller. For best
    accuracy, still do it after the potentially time-consuming getWindowList() operation. */
    long lastUserInputNanos =
        timeSource.convertNanoTimeToRealTimeNanos(desktopLibrary.getLastInputTimeNanos());
    return new DesktopMeta(timeSource.currentTimeNanos(), windowList,
        desktopLibrary.getFocusWindow(), lastUserInputNanos);
  }

  public DesktopLibrary getDesktopLibrary() {
    return desktopLibrary;
  }
}
