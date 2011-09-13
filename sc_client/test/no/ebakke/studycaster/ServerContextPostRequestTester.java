package no.ebakke.studycaster;
import no.ebakke.studycaster.api.ServerContext;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.Test;

public class ServerContextPostRequestTester {
  @Test
  public void testEverything() throws Exception {
    ServerContext sc = new ServerContext();
    String remoteName = "ServerContextPostRequestTester.tmp";
    
    OutputStream uploadOS = sc.uploadFile(remoteName);
    RandomInputStream ris = new RandomInputStream(43, 50000, 50000);
    TestUtil.hookupStreams(ris, uploadOS);
    uploadOS.close();

    InputStream returnedFile = sc.downloadFile("uploads/" + sc.getTicketCC().toString() + "/" + remoteName);
    OutputStream os = new ExpectRandomOutputStream(43, 50000, 50000);

    TestUtil.hookupStreams(returnedFile, os);
    os.close();
  }
}