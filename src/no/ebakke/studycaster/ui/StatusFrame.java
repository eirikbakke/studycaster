package no.ebakke.studycaster.ui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import no.ebakke.studycaster.api.StudyCaster;

public class StatusFrame extends javax.swing.JFrame {
  private static final long serialVersionUID = -49886778462745844L;
  private ProgressBarUI pbui;
  private JDialog positionDialog;
  
  public ProgressBarUI getProgressBarUI() {
    return pbui;
  }

  public void setUploadEnabled(boolean enabled) {
    uploadButton.setEnabled(enabled);
  }

  public JButton getUploadButton() {
    return uploadButton;
  }

  public JDialog getPositionDialog() {
    return positionDialog;
  }

  public static void setSystemLookAndFeel() {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      StudyCaster.log.log(Level.WARNING, "Can't set default Look and Feel", e);
    }
  }

  private static void initIcon(Frame frame) {
    try {
      Method setIconImagesMethod = null;
      try {
        setIconImagesMethod = Window.class.getMethod("setIconImages", List.class);
      } catch (NoSuchMethodException e) { }
      if (setIconImagesMethod == null) {
        // Running JRE < 1.6
        StudyCaster.log.info("Can't find Window.setIconImages(), using Frame.setIconImage() instead (probably on JRE 1.5 or earlier)");
        frame.setIconImage(Toolkit.getDefaultToolkit().getImage(StatusFrame.class.getClassLoader().getResource("no/ebakke/studycaster/resources/icon256.png")));
      } else {
        // Running JRE >= 1.6
        List<Image> icons = new ArrayList<Image>();
        icons.add(Toolkit.getDefaultToolkit().getImage(StatusFrame.class.getClassLoader().getResource("no/ebakke/studycaster/resources/icon16.png")));
        icons.add(Toolkit.getDefaultToolkit().getImage(StatusFrame.class.getClassLoader().getResource("no/ebakke/studycaster/resources/icon22.png")));
        icons.add(Toolkit.getDefaultToolkit().getImage(StatusFrame.class.getClassLoader().getResource("no/ebakke/studycaster/resources/icon24.png")));
        icons.add(Toolkit.getDefaultToolkit().getImage(StatusFrame.class.getClassLoader().getResource("no/ebakke/studycaster/resources/icon32.png")));
        icons.add(Toolkit.getDefaultToolkit().getImage(StatusFrame.class.getClassLoader().getResource("no/ebakke/studycaster/resources/icon48.png")));
        icons.add(Toolkit.getDefaultToolkit().getImage(StatusFrame.class.getClassLoader().getResource("no/ebakke/studycaster/resources/icon64.png")));
        icons.add(Toolkit.getDefaultToolkit().getImage(StatusFrame.class.getClassLoader().getResource("no/ebakke/studycaster/resources/icon128.png")));
        icons.add(Toolkit.getDefaultToolkit().getImage(StatusFrame.class.getClassLoader().getResource("no/ebakke/studycaster/resources/icon256.png")));
        try {
          setIconImagesMethod.invoke(frame, icons);
        } catch (IllegalAccessException e) {
          StudyCaster.log.log(Level.WARNING, "Unexpected error while invoking Window.setIconImages()", e);
        } catch (InvocationTargetException e) {
          StudyCaster.log.log(Level.WARNING, "Got unexpected exception from Window.setIconImages()", e);
        }
      }
    } catch (Exception e) {
      StudyCaster.log.log(Level.WARNING, "Failed to configure window icon", e);
    }
  }

  public StatusFrame(String instructions) {
    positionDialog = new JDialog(this);
    initComponents();
    pbui = new ProgressBarUI(progressBar);
    //progressBar.setIndeterminate(true);
    instructionLabel.setText(instructions);
    pack();
    Dimension sdim = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension wdim = getSize();
    final int XMARGIN_MIN = 40;
    final int YMARGIN_MIN = 110;
    int xmargin = (int) (sdim.width  * 0.14) - wdim.width  / 2;
    int ymargin = (int) (sdim.height * 0.22) - wdim.height / 2;
    xmargin = Math.max(XMARGIN_MIN, xmargin);
    ymargin = Math.max(YMARGIN_MIN, ymargin);
    setLocation(sdim.width - wdim.width - xmargin, sdim.height - wdim.height - ymargin);

    initIcon(this);

    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        StudyCaster.log.info("User tried to close main StudyCaster window");
        int decision = JOptionPane.showConfirmDialog(getPositionDialog(),
                "If you exit the User Study Console without uploading first, your changes will be lost.", "Exit Without Uploading?",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (decision == JOptionPane.OK_OPTION) {
          StudyCaster.log.info("User confirmed closing of main StudyCaster window");
          dispose();
        } else {
          StudyCaster.log.info("User canceled closing of main StudyCaster window");
        }
      }
    });
  }

  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    progressBar = new javax.swing.JProgressBar();
    instructionLabel = new javax.swing.JLabel();
    uploadButton = new javax.swing.JButton();
    underButtonLabel = new javax.swing.JLabel();

    setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
    setTitle("User Study Console");
    setAlwaysOnTop(true);
    setResizable(false);
    getContentPane().setLayout(new java.awt.GridBagLayout());

    progressBar.setBorder(javax.swing.BorderFactory.createEtchedBorder());
    progressBar.setString("");
    progressBar.setStringPainted(true);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    getContentPane().add(progressBar, gridBagConstraints);

    instructionLabel.setText("Please wait...");
    instructionLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(15, 15, 15, 15);
    getContentPane().add(instructionLabel, gridBagConstraints);

    uploadButton.setMnemonic('U');
    uploadButton.setText("Upload and Retrieve Confirmation Code...");
    uploadButton.setEnabled(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
    gridBagConstraints.insets = new java.awt.Insets(15, 15, 5, 15);
    getContentPane().add(uploadButton, gridBagConstraints);

    underButtonLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    underButtonLabel.setText("(You'll have a chance to select a different file if you need to.)");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.insets = new java.awt.Insets(0, 15, 15, 15);
    getContentPane().add(underButtonLabel, gridBagConstraints);

    pack();
  }// </editor-fold>//GEN-END:initComponents

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JLabel instructionLabel;
  private javax.swing.JProgressBar progressBar;
  private javax.swing.JLabel underButtonLabel;
  private javax.swing.JButton uploadButton;
  // End of variables declaration//GEN-END:variables
}
