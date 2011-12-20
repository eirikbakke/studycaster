package no.ebakke.studycaster.screencasting;

import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import no.ebakke.studycaster.screencasting.desktop.DesktopMeta;
import no.ebakke.studycaster.screencasting.desktop.WindowInfo;

/** Immutable. */
public final class ExtendedMeta {
  private static final String MAGIC_STRING_START = "StudyCaster Extended Metadata";
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

  private void writeToStream(DataOutputStream dout) throws IOException {
    dout.writeLong(desktopMeta.getTimeNanos());
    dout.writeLong(desktopMeta.getLastUserInputNanos());
    final List<WindowInfo> windowList = desktopMeta.getWindowList();
    dout.writeInt(windowList.size());
    for (WindowInfo windowInfo : windowList) {
      dout.writeInt(windowInfo.getPID());
      // TODO: Anonymize.
      dout.writeUTF(windowInfo.getTitle());
      dout.writeBoolean(windowInfo.isForeground());
      final Rectangle bounds = windowInfo.getBounds();
      dout.writeInt(bounds.x);
      dout.writeInt(bounds.y);
      dout.writeInt(bounds.width);
      dout.writeInt(bounds.height);
    }
    dout.writeUTF(pageName);
  }

  private static ExtendedMeta readFromStream(DataInputStream din) throws IOException {
    long timeNanos          = din.readLong();
    long lastUserInputNanos = din.readLong();
    int windowListSize = din.readInt();
    if (windowListSize < 0)
      throw new IOException("Read invalid window list size");
    List<WindowInfo> windowList = new ArrayList<WindowInfo>();
    for (int i = 0; i < windowListSize; i++) {
      int pid = din.readInt();
      String title = din.readUTF();
      boolean isForeground = din.readBoolean();
      int boundsX = din.readInt();
      int boundsY = din.readInt();
      int boundsW = din.readInt();
      int boundsH = din.readInt();
      if (boundsW < 0 || boundsH < 0)
        throw new IOException("Read invalid window bounds");
      windowList.add(new WindowInfo(new Rectangle(boundsX, boundsY, boundsW, boundsH),
          title, pid, isForeground));
    }
    String pageName = din.readUTF();
    return new ExtendedMeta(new DesktopMeta(timeNanos, windowList, lastUserInputNanos), pageName);
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
