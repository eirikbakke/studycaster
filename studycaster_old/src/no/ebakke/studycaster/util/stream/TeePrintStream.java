package no.ebakke.studycaster.util.stream;

import java.io.OutputStream;
import java.io.PrintStream;

public class TeePrintStream extends PrintStream {
  private PrintStream otherStream;

  public TeePrintStream(OutputStream out, OutputStream otherStream) {
    super(out);
    this.otherStream = new PrintStream(otherStream);
  }

  @Override
  public void write(int b) {
    super.write(b);
    otherStream.write(b);
  }

  @Override
  public void write(byte[] buf, int off, int len) {
    super.write(buf, off, len);
    otherStream.write(buf, off, len);
  }

  @Override
  public void flush() {
    super.flush();
    otherStream.flush();
  }

  @Override
  public void close() {
    super.close();
    otherStream.close();
  }

  @Override
  public boolean checkError() {
    return super.checkError() || otherStream.checkError();
  }
}
