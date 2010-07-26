package no.ebakke.studycaster2;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import org.junit.Test;

public class PostStreamTester {
  @Test
  public void testPostStream() throws Exception {
    try {
      ServerContext sc = new ServerContext(new URL("http://www.sieuferd.com/studycaster/server.php"));
      OutputStream os = new PostOutputStream(new StringSequenceGenerator("PostStreamTester_", ".tmp"), sc);

      final int SIZE = 5000000;
      InputStream is = new RandomInputStream(0, SIZE, SIZE);
      int c;
      while ((c = is.read()) != -1)
        os.write(c);
      is.close();
      os.close();
      PostInputStream pis = new PostInputStream(new StringSequenceGenerator("uploads/" + sc.getTicketCC().toString() + "/PostStreamTester_", ".tmp"), sc);
      ExpectRandomOutputStream eros = new ExpectRandomOutputStream(0, SIZE, SIZE);
      while ((c = pis.read()) != -1) {
        eros.write(c);
      }
      eros.close();
      pis.close();
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }
}
