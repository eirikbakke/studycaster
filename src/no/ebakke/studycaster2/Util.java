package no.ebakke.studycaster2;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

public final class Util {
  private Util() { }

  public static void logEnvironmentInfo() {
    String propkeys[] = new String[]
      {"java.vendor", "java.version", "java.class.version", "os.name", "os.arch", "os.version", "user.language", "user.region", "user.timezone"};
    StringBuffer props = new StringBuffer();
    boolean first = true;
    for (String key : propkeys) {
      props.append((first ? "" : ", ") + key + "=" + System.getProperty(key));
      first = false;
    }
    StudyCaster2.log.info("Environment: " + props);

    for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
      StudyCaster2.log.info("Found a screen " + gd.getDisplayMode().getWidth() + "x" + gd.getDisplayMode().getHeight()
          + ((gd.equals(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice())) ? " (default)" : ""));
    }
  }
}
