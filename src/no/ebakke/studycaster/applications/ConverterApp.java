package no.ebakke.studycaster.applications;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import no.ebakke.studycaster.screencasting.RecordingConverter;
import no.ebakke.studycaster.api.ServerContext;
import no.ebakke.studycaster.util.Util;

public class ConverterApp {
  public static void main(String args[]) throws Exception {
    ServerContext sc = new ServerContext();
    OutputStream fos = new FileOutputStream("z:/rectest/downloaded.ebc");
    Util.hookupStreams(sc.downloadFile("uploads/1e2019787323/screencast.ebc"), fos);
    fos.close();

    //convert(new FileInputStream("z:/rectest/overbuffer.ebc"), "z:/rectest/overbuffer.mkv");
    RecordingConverter.convert(new FileInputStream("z:/rectest/downloaded.ebc"), "z:/rectest/downconv.mkv");
    //convert(new FileInputStream("z:/rectest/localout.ebc"), "z:/rectest/localconv.mkv");
  }
}
