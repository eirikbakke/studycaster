package no.ebakke.studycaster2;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StreamMuxer {
  private final DataOutputStream out;
  private final Set<String> streamNames = new LinkedHashSet<String>();
  private int nextStreamID = 1;

  public StreamMuxer(OutputStream out) {
    this.out = new DataOutputStream(out);
  }

  public void flush() throws IOException {
    out.flush();
  }

  public void close() throws IOException {
    out.close();
  }

  public OutputStream createOutputStream(String streamName) throws IOException {
    synchronized (out) {
      if (nextStreamID > 255)
        throw new IllegalStateException("Too many substreams.");
      if (!streamNames.add(streamName))
        throw new IllegalStateException("Substream " + streamName + " already exists.");
      out.writeByte(0);
      out.writeUTF(streamName);
      return new BufferedOutputStream(new MuxedOutputStream(nextStreamID++));
    }
  }

  private class MuxedOutputStream extends OutputStream {
    private int streamID;

    public MuxedOutputStream(int streamID) {
      this.streamID = streamID;
    }

    @Override
    public void write(int b) throws IOException {
      //write(new byte[] { (byte) b }, 0, 1);
      throw new AssertionError("Did not expect BufferedOutputStream to write byte-wise.");
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      synchronized (out) {
        out.writeByte((byte) streamID);
        out.writeInt(len);
        out.write(b, off, len);
        //System.out.println("Wrote bytes: " + len + " one " + b[0] + " two " + b[1]);
      }
    }

    @Override
    public void flush() throws IOException {
      out.flush();
    }

    @Override
    public void close() throws IOException {
      flush();
    }
  }

  public static void demux(InputStream is, Map<String, OutputStream> outputStreams) throws IOException {
    List<OutputStream> mappedStreams = new ArrayList<OutputStream>();
    DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
    int nextStreamID = 1;
    byte buf[] = new byte[0];

    while (true) {
      int streamID;
      try {
        streamID = dis.readByte() & 0xFF;
      } catch (EOFException e) {
        break;
      }
      if (streamID == 0) {
        if (nextStreamID > 255)
          throw new IOException("Corrupt stream, too many substreams.");
        mappedStreams.add(outputStreams.get(dis.readUTF()));
        nextStreamID++;
      } else {
        if (streamID-1 >= mappedStreams.size())
          throw new IOException("Corrupt stream; undeclared substream number " + streamID);
        int len = dis.readInt();
        if (len < 0)
          throw new IOException("Corrupt stream, got negative length.");
        if (len > buf.length)
          buf = new byte[Math.max(len, (int) (buf.length * 1.25))];
        int pos = 0;
        while (pos < len) {
          int got = dis.read(buf, pos, len - pos);
          if (got < 0)
            throw new EOFException();
          pos += got;
        }
        OutputStream os = mappedStreams.get(streamID-1);
        if (os != null)
          os.write(buf, 0, len);
      }
    }
  }
}
