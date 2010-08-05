package no.ebakke.studycaster2;

import java.io.OutputStream;
import java.net.URI;

public class Main {
  public static void main(String args[]) throws Exception {
    NonBlockingOutputStream nbos = new NonBlockingOutputStream(1024 * 128);
    ConsoleTee conTee = new ConsoleTee(nbos);
    ServerTimeLogFormatter logFormatter = new ServerTimeLogFormatter();
    logFormatter.install();
    ServerContext sc = new ServerContext(new URI("http://www.sieuferd.com/studycaster/server.php"));
    logFormatter.setServerSecondsAhead(sc.getServerSecondsAhead());

    sc.downloadFile("exflat.xls").close();
    sc.downloadFile("exflat.xls").close();
    sc.downloadFile("exflat.xls").close();

    OutputStream out = sc.uploadFile("console.txt");
    nbos.connect(out);

    StudyCaster2.log.info("Log 1");
    StudyCaster2.log.info("Log 3");
    Thread.sleep(500);
    StudyCaster2.log.info("Log 4");
    conTee.disconnect();
    nbos.close();
    out.close();
    
    /*
    Logger.getLogger("").log(Level.WARNING, "testmessage", new Exception("Test exception", new FileNotFoundException()));
    System.out.println("Doesn't make it.");
    System.out.println("Doesn't make it (stderr).");
    */
  }
}
