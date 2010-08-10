package no.ebakke.studycaster.util.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class ConsoleTee {
  private OutputStream out;
  private PrintStream oldStdOut, oldStdErr;
  private boolean disconnected = false;
  private TeePrintStream teeStdOut, teeStdErr;

  public ConsoleTee(OutputStream out) {
    this.out = out;
    oldStdOut = System.out;
    oldStdErr = System.err;
    teeStdOut = new TeePrintStream(out, System.out);
    teeStdErr = new TeePrintStream(out, System.err);
    System.setOut(teeStdOut);
    System.setErr(teeStdErr);
  }

  public void close() throws IOException {
    if (disconnected)
      return;
    disconnected = true;
    System.setOut(oldStdOut);
    System.setErr(oldStdErr);
    out.close();
  }
}
