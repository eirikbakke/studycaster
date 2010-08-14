package no.ebakke.studycaster.applications;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import no.ebakke.studycaster.screencasting.RecordingConverter;
import no.ebakke.studycaster.api.ServerContext;
import no.ebakke.studycaster.util.Util;

public class Converter {
  public static void main(String args[]) throws Exception {
    int speedupFactor = 1;
    String confCode = "61bf51d47e93";
    boolean download = true;

    if (download) {
      String fileName = "z:/rectest/downloaded.ebc";
      ServerContext sc = new ServerContext();
      OutputStream fos = new FileOutputStream(fileName);
      Util.hookupStreams(sc.downloadFile("uploads/" + confCode + "/screencast.ebc"), fos);
      fos.close();
      RecordingConverter.convert(new FileInputStream(fileName), "z:/rectest/downconv.mkv", speedupFactor);
    } else {
      RecordingConverter.convert(new FileInputStream("z:/recruiting/uploads/" + confCode + "/screencast.ebc"),
            "z:/recruiting/uploads/" + confCode + "/screencast.mkv", speedupFactor);
    }
  }
}
