package no.ebakke.studycaster2;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import org.junit.Test;

public class PostStreamTester {
  @Test
  public void TestPostStream() throws Exception {
    ServerContext sc = new ServerContext(new URL("http://www.sieuferd.com/studycaster/server.php"));
    OutputStream os = new PostOutputStream(new StringSequenceGenerator("PostStreamTester_", ".tmp"), sc);

    final int SIZE = 49999;
    InputStream is = new RandomInputStream(0, SIZE, SIZE);
    int c;
    while ((c = is.read()) != -1) {
      os.write(c);
    }
    is.close();
    os.close();
  }
}
