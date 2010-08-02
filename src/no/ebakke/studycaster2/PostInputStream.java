package no.ebakke.studycaster2;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class PostInputStream extends InputStream {
  private StringSequenceGenerator fileNames;
  private ServerScript script;
  private final Object lock = new Object();
  private boolean closed = false, eof = false;
  private InputStream currentFile;

  public PostInputStream(StringSequenceGenerator fileNames, ServerScript script) {
    this.fileNames = fileNames;
    this.script    = script;
  }

  @Override
  public int read() throws IOException {
    byte ret[] = new byte[1];
    int got = read(ret);
    if (got != 1) {
      assert got == -1;
      return -1;
    }
    return ret[0] & 0xFF;
  }

  private void closeCurrentFile() throws IOException {
    if (currentFile != null) {
      currentFile.close();
      currentFile = null;
    }
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    synchronized (lock) {
      if (closed)
        throw new IOException("Stream closed");
      if (eof)
        return -1;
      int got;
      while (currentFile == null || (got = currentFile.read(b, off, len)) < 1) {
        closeCurrentFile();
        String next = fileNames.nextString();
        try {
          currentFile = script.downloadFile(next);
        } catch (FileNotFoundException e) {
          eof = true;
          return -1;
        }
      }
      return got;
    }
  }

  @Override
  public void close() throws IOException {
    synchronized (lock) {
      if (closed)
          throw new IOException("Already closed");
      closed = true;
      closeCurrentFile();
    }
  }
}
