package no.ebakke.studycaster2;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class PostOutputStream extends OutputStream {
  // Used for testing: 650, 499999.
  private static int MAX_UPLOAD_MILLIS = 20000;
  private static int MAX_UPLOAD_BYTES  = 5000000;
  private StringSequenceGenerator fileNames;
  private ServerScript script;
  private OutputStream currentFile;
  private long currentFileStartTime;
  private int currentFileBytes;
  private boolean closed;
  private Exception storedException;
  private Thread currentFileUploadThread;
  private Thread currentFileTimeOutThread;
  private final Object lock = new Object();

  public PostOutputStream(StringSequenceGenerator fileNames, ServerScript script) {
    this.script = script;
    this.fileNames = fileNames;
  }

  @Override
  public void write(int b) throws IOException {
    write(new byte[] { (byte) b }, 0, 1);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    synchronized (lock) {
      if (closed)
        throw new IllegalStateException("Stream closed.");
      if (storedException != null)
        throw new IOException("Problem with upload", storedException);

      if (currentFile == null) {
        final PipedInputStream is = new PipedInputStream();
        currentFile = new PipedOutputStream(is);
        currentFileStartTime = System.currentTimeMillis();
        currentFileBytes = 0;

        final String fileName = fileNames.nextString();
        currentFileUploadThread = new Thread(new Runnable() {
          public void run() {
            try {
              script.uploadFile(fileName, is);
            } catch (IOException e) {
              storedException = e;
            }
          }
        }, "Upload thread (" + fileName + ")");
        currentFileUploadThread.start();
        currentFileTimeOutThread = new Thread(new Runnable() {
          public void run() {
            long remainingTime;
            while (!Thread.interrupted()) {
              synchronized (lock) {
                if (currentFile == null)
                  break;

                remainingTime = MAX_UPLOAD_MILLIS + currentFileStartTime - System.currentTimeMillis();
                if (remainingTime <= 0) {
                  try {
                    closeCurrentFile();
                  } catch (IOException e) {
                    storedException = e;
                  }
                  break;
                } else {
                  try {
                    // Note: No notify() anywhere; we're doing it just for the timeout and locking semantics.
                    lock.wait(remainingTime);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }
              }
            }
          }
        }, "Upload timeout thread (" + fileName + ")");
        currentFileTimeOutThread.start();
      }
      int writeNow = Math.min(MAX_UPLOAD_BYTES - currentFileBytes, len);
      assert(writeNow > 0);
      currentFile.write(b, off, writeNow);
      currentFileBytes += writeNow;

      assert(currentFileBytes <= MAX_UPLOAD_BYTES);
      if (currentFileBytes == MAX_UPLOAD_BYTES)
        closeCurrentFile();

      if (writeNow < len)
        write(b, off + writeNow, len - writeNow);
    }
  }

  @Override
  public void flush() throws IOException {
    synchronized (lock) {
      if (closed)
        throw new IllegalStateException("Stream closed.");
      if (storedException != null)
        throw new IOException("Problem with upload", storedException);
      if (currentFile != null)
        currentFile.flush();
    }
  }

  @Override
  public void close() throws IOException {
    synchronized (lock) {
      if (!closed) {
        closed = true;
        closeCurrentFile();
      }
    }
  }

  private void closeCurrentFile() throws IOException {
    if (currentFile != null) {
      currentFile.close();
      currentFile = null;
      try {
        currentFileUploadThread.join();
      } catch (InterruptedException e) {
        throw new IOException("Interrupted while uploading", e);
      }
      if (storedException != null)
        throw new IOException("Problem with upload", storedException);
    }
    currentFileTimeOutThread.interrupt();
  }
}
