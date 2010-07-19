/*
 * Created on 24-Jan-05
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package no.ebakke.orgstonesoupscreen;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;
import java.io.IOException;
import java.io.InputStream;

public class ScreenPlayer implements Runnable {
  private ScreenPlayerListener listener;
  private MemoryImageSource mis = null;
  private long startTime;
  private boolean running;
  private boolean paused;
  private boolean fastForward;
  private boolean realtime = false;
  private RecordingStream recStr;

  public ScreenPlayer(InputStream iStream, ScreenPlayerListener listener) {
    this.listener = listener;
    recStr = new RecordingStream(iStream);
  }

  public void play() {
    fastForward = false;
    paused = false;

    if (running == false) {
      new Thread(this, "Screen Player").start();
    }
  }

  public void pause() {
    if (realtime == false) {
      paused = true;
    }
  }

  public void stop() {
    paused = false;
    running = false;
  }

  public void fastforward() {
    fastForward = true;
    paused = false;
  }

  public synchronized void run() {
    startTime = System.currentTimeMillis();
    long lastFrameTime = 0;

    running = true;
    while (running == true) {
      while (paused == true && realtime == false) {
        try {
          Thread.sleep(50);
        } catch (Exception e) {
        }
        startTime += 50;
      }

      try {
        readFrame();
        listener.newFrame();
      } catch (IOException ioe) {
        //ioe.printStackTrace();
        listener.showNewImage(null);
        break;
      }

      if (fastForward == true) {
        startTime -= (recStr.getFrameTime() - lastFrameTime);
      } else {
        while ((System.currentTimeMillis() - startTime < recStr.getFrameTime() && realtime == false) && running) {
          try {
            Thread.sleep(100);
          } catch (Exception e) {
          }
        }

        //System.out.println( "FrameTime:"+frameTime+">"+(System.currentTimeMillis()-startTime));
      }

      lastFrameTime = recStr.getFrameTime();
    }
    running = false;

    listener.playerStopped();
  }

  private void readFrame() throws IOException {
    if (recStr.readFrameData()) {
      if (recStr.isFinished())
        running = false;
      return;
    }
    if (mis == null) {
      mis = new MemoryImageSource(recStr.getArea().width, recStr.getArea().height,
              recStr.getFrameData(), 0, recStr.getArea().width);
      mis.setAnimated(true);
      listener.showNewImage(Toolkit.getDefaultToolkit().createImage(mis));
      return;
    } else {
      mis.newPixels(recStr.getFrameData(), ColorModel.getRGBdefault(), 0, recStr.getArea().width);
      return;
    }
  }

  public boolean isRealtime() {
    return realtime;
  }

  public void setRealtime(boolean realtime) {
    this.realtime = realtime;
  }
}
