package no.ebakke.studycaster.screencasting;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import no.ebakke.studycaster.screencasting.desktop.DesktopMeta;

/** Immutable. */
public final class ExtendedMeta {
  private static final String MAGIC_STRING_START = "StudyCaster Extended Metadata";
  private static final String MAGIC_STRING_END   = "End";
  private final DesktopMeta desktopMeta;
  private final String      pageName;

  public ExtendedMeta(DesktopMeta desktopMeta, String pageName) {
    this.desktopMeta = desktopMeta;
    this.pageName = pageName;
  }

  public DesktopMeta getDesktopMeta() {
    return desktopMeta;
  }

  public String getPageName() {
    return pageName;
  }

  private void writeToStream(DataOutputStream dout) throws IOException {
    // TODO: Implement.
  }

  private static ExtendedMeta readFromStream(DataInputStream din) throws IOException {
    // TODO: Implement.
    return null;
  }

  public static class ExtendedMetaWriter {
    private final DataOutputStream dout;

    public ExtendedMetaWriter(OutputStream out, String configurationID) throws IOException {
      dout = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(out)));
      dout.writeUTF(MAGIC_STRING_START);
    }

    public void writeOne(ExtendedMeta meta) throws IOException {
      // TODO: Implement.
    }

    public void close() throws IOException {
      dout.writeUTF(MAGIC_STRING_END);
      dout.close();
    }
  }

  public static class ExtendedMetaReader {
    private final DataInputStream din;
    private final String configurationID;

    public ExtendedMetaReader(InputStream in) throws IOException {
      this.din = new DataInputStream(new BufferedInputStream(new GZIPInputStream(in)));
      this.configurationID = null;
    }

    public ExtendedMeta readOne() throws IOException {
      // TODO: Implement.
      return null;
    }

    public String getConfigurationID() {
      return configurationID;
    }

    public void close() throws IOException {
      din.close();
    }
  }
}
