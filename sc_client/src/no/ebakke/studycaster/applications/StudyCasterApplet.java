package no.ebakke.studycaster.applications;

import java.applet.Applet;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import no.ebakke.studycaster.backend.EnvironmentHooks;
import no.ebakke.studycaster.backend.ServerContext;
import no.ebakke.studycaster.backend.StudyCasterException;
import no.ebakke.studycaster.ui.UIUtil;

public class StudyCasterApplet extends Applet {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  // Wrap in Swing HTML tags to ensure proper line breaking.
  private final String STATUS_RUNNING = "<html>StudyCaster is running in a separate window.</html>";
  private final String STATUS_CLOSED  = "<html>The StudyCaster window was closed. Click here to reopen.</html>";
  private volatile EnvironmentHooks hooks;
  // Access from EHT only.
  private StudyCaster studyCaster;

  @Override
  public void init() {
    hooks = EnvironmentHooks.create();
    LOG.info("Applet init (after hooks created)");

    {
      // TODO: Consider making this less hacky.
      String serverURIProp = getParameter(ServerContext.SERVERURI_PROP_NAME);
      if (serverURIProp != null)
        System.setProperty(ServerContext.SERVERURI_PROP_NAME, serverURIProp);
      String configIDProp = getParameter(StudyCaster.CONFIGID_PROP_NAME);
      if (configIDProp != null)
        System.setProperty(StudyCaster.CONFIGID_PROP_NAME, configIDProp);
    }

    try {
      UIUtil.swingBlock(new UIUtil.CallableExt<Void,RuntimeException>() {
        public Void call() {
          // Must be called before any UI components are rendered.
          try {
            UIUtil.setSystemLookAndFeel();
          } catch (StudyCasterException e) {
            LOG.log(Level.INFO, "Couldn't set system L&F", e);
          }
          initComponents();
          try {
            appletLabel.setIcon(new ImageIcon(UIUtil.loadImage("icon32.png", true)));
          } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to load applet label icon", e);
          }
          validate();
          addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
              if (studyCaster == null) {
                LOG.info("Starting StudyCaster after mouse click in applet");
                startStudyCaster();
              } else {
                LOG.info("Restoring/focusing StudyCaster window after mouse click in applet");
                studyCaster.restoreLocationAndRequestFocus();
              }
            }
          });
          return null;
        }
      });
    } catch (InterruptedException e) {
      LOG.log(Level.SEVERE, "Interrupted while initializing study from applet", e);
    }
  }

  @Override
  public void setSize(int width, int height) {
    super.setSize(width,height);
    validate();
  }

  @Override
  public void destroy() {
    LOG.info("Applet destroy");
    super.destroy();
  }

  /* TODO: Make StudyCaster lifecycle interface more suitable for use either with applets or
           applications. */

  private void startStudyCaster() {
    try {
      UIUtil.swingBlock(new UIUtil.CallableExt<Void,RuntimeException>() {
        public Void call() {
          if (studyCaster != null)
            return null;
          appletLabel.setText(STATUS_RUNNING);
          studyCaster = new StudyCaster(hooks, false, new Runnable() {
            public void run() {
              hooks = EnvironmentHooks.create();
              LOG.info("Reconnected EnvironmentHooks after StudyCaster shutdown");
              appletLabel.setText(STATUS_CLOSED);
              studyCaster = null;
            }
          });
          studyCaster.runStudy();
          return null;
        }
      });
    } catch (InterruptedException e) {
      LOG.log(Level.SEVERE, "Interrupted while starting study from applet", e);
    }
  }

  @Override
  public void start() {
    LOG.info("Applet start");
    startStudyCaster();
  }

  @Override
  public void stop() {
    LOG.info("Applet stop");
    try {
      UIUtil.swingBlock(new UIUtil.CallableExt<Void,RuntimeException>() {
        public Void call() {
          if (studyCaster != null && !studyCaster.isClosed())
            studyCaster.close();
          studyCaster = null;
          return null;
        }
      });
    } catch (InterruptedException e) {
      LOG.log(Level.SEVERE, "Interrupted while stopping study from applet", e);
    }
  }

  /**
   * This method is called from within the init() method to initialize the form. WARNING: Do NOT
   * modify this code. The content of this method is always regenerated by the Form Editor.
   */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        appletLabel = new javax.swing.JLabel();

        setLayout(new java.awt.BorderLayout());

        appletLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        appletLabel.setText("Status here.");
        appletLabel.setFocusable(false);
        add(appletLabel, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel appletLabel;
    // End of variables declaration//GEN-END:variables
}
