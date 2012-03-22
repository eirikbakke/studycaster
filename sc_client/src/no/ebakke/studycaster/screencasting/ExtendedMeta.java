package no.ebakke.studycaster.screencasting;

import java.awt.Rectangle;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import no.ebakke.studycaster.screencasting.desktop.DesktopMeta;
import no.ebakke.studycaster.screencasting.desktop.WindowInfo;

/** Immutable. */
public final class ExtendedMeta {
  public  static final String FILE_EXTENSION = "em2";
  private static final String MAGIC_STRING_START = "StudyCaster Extended Metadata V2";
  private static final String MAGIC_STRING_END   = "End";
  private final DesktopMeta desktopMeta;
  private final String      pageName;

  public ExtendedMeta(DesktopMeta desktopMeta, String pageName) {
    this.desktopMeta = desktopMeta;
    this.pageName    = pageName;
  }

  public DesktopMeta getDesktopMeta() {
    return desktopMeta;
  }

  public String getPageName() {
    return pageName;
  }

  private static void writeWindowInfoToStream(DataOutputStream dout, WindowInfo windowInfo)
      throws IOException
  {
    dout.writeInt(windowInfo.getPID());
    // TODO: Anonymize.
    dout.writeUTF(windowInfo.getTitle());
    dout.writeUTF(windowInfo.getType());
    dout.writeBoolean(windowInfo.isForeground());
    final Rectangle bounds = windowInfo.getBounds();
    dout.writeInt(bounds.x);
    dout.writeInt(bounds.y);
    dout.writeInt(bounds.width);
    dout.writeInt(bounds.height);
  }

  private static WindowInfo readWindowInfoFromStream(DataInputStream din) throws IOException {
    final int pid = din.readInt();
    final String title = din.readUTF();
    final String type = din.readUTF();
    final boolean isForeground = din.readBoolean();
    final int boundsX = din.readInt();
    final int boundsY = din.readInt();
    final int boundsW = din.readInt();
    final int boundsH = din.readInt();
    if (boundsW < 0 || boundsH < 0)
      throw new IOException("Read invalid window bounds");
    return new WindowInfo(new Rectangle(boundsX, boundsY, boundsW, boundsH),
        title, type, pid, isForeground);
  }

  private void writeToStream(DataOutputStream dout) throws IOException {
    dout.writeLong(desktopMeta.getTimeNanos());
    dout.writeLong(desktopMeta.getLastUserInputNanos());
    final WindowInfo focusWindow = desktopMeta.getFocusWindow();
    if (focusWindow != null) {
      dout.writeBoolean(true);
      writeWindowInfoToStream(dout, focusWindow);
    } else {
      dout.writeBoolean(false);
    }
    final List<WindowInfo> windowList = desktopMeta.getWindowList();
    dout.writeInt(windowList.size());
    for (WindowInfo windowInfo : windowList)
      writeWindowInfoToStream(dout, windowInfo);
    dout.writeUTF(pageName);
  }

  private static ExtendedMeta readFromStream(DataInputStream din) throws IOException {
    final long timeNanos          = din.readLong();
    final long lastUserInputNanos = din.readLong();
    final WindowInfo focusWindow;
    if (din.readBoolean()) {
      focusWindow = readWindowInfoFromStream(din);
    } else {
      focusWindow = null;
    }
    final int windowListSize = din.readInt();
    if (windowListSize < 0)
      throw new IOException("Read invalid window list size");
    final List<WindowInfo> windowList = new ArrayList<WindowInfo>();
    for (int i = 0; i < windowListSize; i++)
      windowList.add(readWindowInfoFromStream(din));
    final String pageName = din.readUTF();
    return new ExtendedMeta(
        new DesktopMeta(timeNanos, windowList, focusWindow, lastUserInputNanos), pageName);
  }

  /** Thread-safe. */
  public static class ExtendedMetaWriter {
    private final DataOutputStream dout;

    public ExtendedMetaWriter(OutputStream out, String configurationID) throws IOException {
      dout = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(out)));
      dout.writeUTF(MAGIC_STRING_START);
      dout.writeUTF(configurationID);
    }

    public synchronized void writeOne(ExtendedMeta meta) throws IOException {
      dout.writeBoolean(false);
      meta.writeToStream(dout);
    }

    public synchronized void close() throws IOException {
      dout.writeBoolean(true);
      dout.writeUTF(MAGIC_STRING_END);
      dout.close();
    }
  }

  /** Thread-safe. */
  public static class ExtendedMetaReader {
    private final DataInputStream din;
    private final String configurationID;
    private boolean endEncountered = false;

    public ExtendedMetaReader(InputStream in) throws IOException {
      this.din = new DataInputStream(new BufferedInputStream(new GZIPInputStream(in)));
      if (!din.readUTF().equals(MAGIC_STRING_START))
        throw new IOException("Invalid start marker");
      this.configurationID = din.readUTF();
    }

    /** Returns null when the end-of-file marker is reached. */
    public synchronized ExtendedMeta readOne() throws IOException {
      if (endEncountered)
        return null;
      endEncountered = din.readBoolean();
      if (endEncountered) {
        if (!din.readUTF().equals(MAGIC_STRING_END))
          throw new IOException("Invalid end marker");
        return null;
      }
      return ExtendedMeta.readFromStream(din);
    }

    public String getConfigurationID() {
      return configurationID;
    }

    public void close() throws IOException {
      din.close();
    }
  }
}
