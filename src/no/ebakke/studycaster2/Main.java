package no.ebakke.studycaster2;

import java.io.OutputStream;
import java.net.URL;

public class Main {
  public static void main(String args[]) throws Exception {
    NonBlockingOutputStream nbos = new NonBlockingOutputStream(1024 * 128);
    ConsoleTee conTee = new ConsoleTee(nbos);
    ServerContext sc = new ServerContext(new URL("http://www.sieuferd.com/studycaster/server.php"));

    OutputStream out = new PostOutputStream(new StringSequenceGenerator("console_", ".tmp"), sc);
    nbos.connect(out);
    /*
    System.setErr(new TeePrintStream(uploadSink, System.err));
    System.setOut(new TeePrintStream(uploadSink, System.out));
    */
    /*
    System.out.println("Doesn't make it.");
    System.out.println("Doesn't make it (stderr).");
    */
    System.out.println("Does make it.");
    Thread.sleep(5000);
    System.err.println("Does make it (to stderr).");
    System.out.println("Does make it, too.");
    conTee.disconnect();
    System.out.println("Doesn't make it, nah.");
    System.out.println("Doesn't make it, nah (stderr).");

    /*
    PipedOutputStream uploadSink = new PipedOutputStream();
    PipedInputStream  uploadSource = new PipedInputStream(uploadSink, 1024 * 1024 * 4);
    StreamMuxer uploadMux = new StreamMuxer(uploadSink);

    //System.setErr(new TeePrintStream(System.err, out));
    //System.setOut(new TeePrintStream(System.out, out));

    System.setErr(new TeePrintStream(System.err, uploadMux.createOutputStream("stderr")));
    System.setOut(new TeePrintStream(System.out, uploadMux.createOutputStream("stdout")));
    */

    /*
    PrintStream testPrintStream = new PrintStream(uploadQueue);
    System.out.println("Got here 1!");
    System.out.println("Available: " + uploadSource.available());
    testPrintStream.print("One message");
    System.out.println("Got here 2!");
    System.out.println("Available: " + uploadSource.available());
    testPrintStream.print("Two messages");
    System.out.println("Got here 3!");
    System.out.println("Available: " + uploadSource.available());
    testPrintStream.flush();
    System.out.println("Got here 4!");
    System.out.println("Available: " + uploadSource.available());
    uploadQueue.flush();
    System.out.println("Got here 5!");
    System.out.println("Available: " + uploadSource.available());
    testPrintStream.close();
    System.out.println("Got here 6!");
    System.out.println("Available: " + uploadSource.available());
    uploadQueue.close();
    System.out.println("Got here 7!");
    System.out.println("Available: " + uploadSource.available());
    */



    //ServerContext sc = new ServerContext(new URL("http://www.sieuferd.com/studycaster/server.php"));
    //URL libLoc = Main.class.getClassLoader().getResource("no/ebakke/studycaster2/libNativeLib.dll");
    //String fileName = new File(libLoc.getFile()).getAbsolutePath();
    //String fileName = "Z:/Unfinished/ResearchRepo/archive/100713_StudyCaster/build/classes/no/ebakke/studycaster2/libNativeLib.dll";
    String fileName = "Z:/Unfinished/ResearchRepo/archive/100713_StudyCaster/NativeLib/libNativeLib.dll";
    //System.out.println(fileName);
    //System.load(fileName);
    //System.out.println("OOOO " + System.getProperty("java.library.path"));
    System.loadLibrary("libSCNative");
/*
    while (true) {
      Thread.sleep(1000);
      System.out.println(NativeLibrary.getPermittedRecordingArea(Arrays.asList(new String[] {"Notepad", "Calc"}), true));
    }
*/
    //System.out.println("String from native library: " + NativeLibrary.getTestString());
  }
}
