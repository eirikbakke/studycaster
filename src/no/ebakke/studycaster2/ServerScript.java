package no.ebakke.studycaster2;
import java.io.IOException;
import java.io.InputStream;

public interface ServerScript {
  public void        uploadFile(String fileName, InputStream is) throws IOException;
  public InputStream downloadFile(String remoteName)             throws IOException;
}
