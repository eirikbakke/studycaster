package no.ebakke.studycaster2;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import org.junit.Test;

public class ServerContextPostRequestTester {
  @Test
  public void testEverything() throws Exception {
    ServerContext sc = new ServerContext(new URL("http://www.sieuferd.com/studycaster/server.php"));
    File tf = File.createTempFile("ServerContextTest_", ".tmp");
    String remoteName = tf.getName();
    tf.delete();
    System.out.println(remoteName);
    sc.uploadFile(remoteName, new RandomInputStream(43, 50000, 50000));
    InputStream returnedFile = sc.downloadFile("uploads/" + sc.getTicketCC().toString() + "/" + remoteName);
    OutputStream os = new ExpectRandomOutputStream(43, 50000, 50000);
    int c;
    while ((c = returnedFile.read()) != -1)
      os.write(c);
    returnedFile.close();
    os.close();
  }

}