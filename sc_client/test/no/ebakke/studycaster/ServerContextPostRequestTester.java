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
    RandomHookup hookup = new RandomHookup(0);
    
    OutputStream uploadOS = sc.uploadFile(remoteName);
    RandomInputStream ris = new RandomInputStream(43, 5000000, 5000000);
    hookup.hookupStreams(ris, uploadOS);
    uploadOS.close();

    InputStream returnedFile = sc.downloadFile(
        "uploads/" + sc.getLaunchTicket().toString() + "/" + remoteName);
    OutputStream os = new ExpectRandomOutputStream(43, 5000000, 5000000);

    hookup.hookupStreams(returnedFile, os);
    os.close();
  }
}