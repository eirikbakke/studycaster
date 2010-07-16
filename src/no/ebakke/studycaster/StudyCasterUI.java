package no.ebakke.studycaster;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class StudyCasterUI {
  public enum UserAction {
    NO_ACTION,
    CLOSE,
    UPLOAD
  }

  private StatusFrame sf;
  private final Object actionCondition = new Object();
  private UserAction actionTaken = UserAction.NO_ACTION;

  public StudyCasterUI(final String instructions) {
    final Object initedCondition = new Object();
    StatusFrame.setSystemLookAndFeel();
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        synchronized (initedCondition) {
          sf = new StatusFrame(instructions);
          sf.setVisible(true);
          sf.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent evt) {
              synchronized (actionCondition) {
                actionTaken = UserAction.CLOSE;
                actionCondition.notifyAll();
              }
            }
          });
          sf.getUploadButton().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              synchronized (actionCondition) {
                actionTaken = UserAction.UPLOAD;
                actionCondition.notifyAll();
              }
            }
          });
          initedCondition.notify();
        }
      }
    });
    synchronized (initedCondition) {
      while (sf == null) {
        try {
          initedCondition.wait();
        } catch (InterruptedException e) { }
      }
    }
  }

  public void disposeUI() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        sf.dispose();
      }
    });
  }

  public void showMessageDialog(final String title, final String message, final int messageType, boolean block) {
    // TODO: Clean up or refactor the synchonization mess in this class.
    final Object doneCondition = new Object();
    final boolean doneYet[] = new boolean[1];
    doneYet[0] = false;

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        JDialog positionDialog = new JDialog(sf, true);
        Dimension sdim = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension wdim = positionDialog.getSize();
        positionDialog.setLocation(sdim.width - wdim.width - 100, sdim.height - wdim.height - 150);
        JOptionPane.showMessageDialog(positionDialog, message, title, messageType);
        positionDialog.dispose();
        synchronized (doneCondition) {
          doneYet[0] = true;
          doneCondition.notifyAll();
        }
      }
    });
    if (block) {
      synchronized (doneCondition) {
        while (!doneYet[0]) {
          try {
            doneCondition.wait();
          } catch (InterruptedException e) { }
        }
      }
    }
  }

  public ProgressBarUI getProgressBarUI() {
    return sf.getProgressBarUI();
  }

  public void setUploadEnabled(final boolean enabled) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        sf.setUploadEnabled(enabled);
      }
    });
  }

  public UserAction waitForUserAction() {
    UserAction ret;
    synchronized (actionCondition) {
      while ((ret = actionTaken) == UserAction.NO_ACTION) {
        try {
          actionCondition.wait();
        } catch (InterruptedException e) { }
      }
      actionTaken = UserAction.NO_ACTION;
    }
    return ret;
  }
}
