package no.ebakke.studycaster.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;

public final class ServerContextUtil {
  private ServerContextUtil() {
  }

  public static void downloadFile(ServerContext serverContext, String remoteName, File toFile)
      throws IOException
  {
    OutputStream os = new FileOutputStream(toFile);
    try {
      InputStream is = serverContext.downloadFile(remoteName);
      try {
        IOUtils.copy(is, os);
      } finally {
        is.close();
      }
    } finally {
      os.close();
    }
  }

  public static void uploadFile(ServerContext serverContext, File f, String remoteName)
      throws IOException
  {
    OutputStream os = serverContext.uploadFile(remoteName);
    try {
      InputStream is = new FileInputStream(f);
      try {
        IOUtils.copy(is, os);
      } finally {
        is.close();
      }
    } finally {
      os.close();
    }
  }
}
