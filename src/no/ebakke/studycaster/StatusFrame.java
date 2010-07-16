package no.ebakke.studycaster;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.swing.JButton;
import javax.swing.UIManager;

public class StatusFrame extends javax.swing.JFrame {
  private static final long serialVersionUID = -49886778462745844L;
  private ProgressBarUI pbui;
  
  public ProgressBarUI getProgressBarUI() {
    return pbui;
  }

  public void setUploadEnabled(boolean enabled) {
    uploadButton.setEnabled(enabled);
  }

  public JButton getUploadButton() {
    return uploadButton;
  }

  public static void setSystemLookAndFeel() {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      StudyCaster.log.log(Level.WARNING, "Can't set default Look and Feel", e);
    }
  }

  public StatusFrame(String instructions) {
    initComponents();
    pbui = new ProgressBarUI(progressBar);
    progressBar.setIndeterminate(true);
    instructionLabel.setText(instructions);
    pack();
    Dimension sdim = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension wdim = getSize();
    setLocation(sdim.width - wdim.width - 100, sdim.height - wdim.height - 150);

    try {
      List<Image> icons = new ArrayList<Image>();
      icons.add(Toolkit.getDefaultToolkit().getImage(StatusFrame.class.getClassLoader().getResource("no/ebakke/studycaster/resources/icon16.png")));
      icons.add(Toolkit.getDefaultToolkit().getImage(StatusFrame.class.getClassLoader().getResource("no/ebakke/studycaster/resources/icon22.png")));
      icons.add(Toolkit.getDefaultToolkit().getImage(StatusFrame.class.getClassLoader().getResource("no/ebakke/studycaster/resources/icon32.png")));
      setIconImages(icons);
    } catch (Exception e) {
      StudyCaster.log.log(Level.WARNING, "Can't set icon images.", e);
    }
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

    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    setTitle("User Study Console");
    setAlwaysOnTop(true);
    setResizable(false);
    getContentPane().setLayout(new java.awt.GridBagLayout());

    progressBar.setBorder(javax.swing.BorderFactory.createEtchedBorder());
    progressBar.setString("");
    progressBar.setStringPainted(true);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
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
    gridBagConstraints.insets = new java.awt.Insets(15, 15, 15, 15);
    getContentPane().add(uploadButton, gridBagConstraints);

    pack();
  }// </editor-fold>//GEN-END:initComponents

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JLabel instructionLabel;
  private javax.swing.JProgressBar progressBar;
  private javax.swing.JButton uploadButton;
  // End of variables declaration//GEN-END:variables
}
