package no.ebakke.studycaster.ui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;
import no.ebakke.studycaster.api.StudyCaster;
import no.ebakke.studycaster.util.Util;

public class UploadDialog extends javax.swing.JDialog {
    private static final long serialVersionUID = -5929608062465652108L;
    private JFileChooser fileChooser;
    private File selectedFile;

    public File getSelectedFile() {
      return selectedFile;
    }

    private void setFile(File file) {
      String text = (file == null) ? "" : Util.getPathString(file);
      fileNameTextBox.setText(text);
      fileNameTextBox.setSelectionStart(0);
      fileNameTextBox.setSelectionEnd(text.length());
    }

    public UploadDialog(Frame parent, FileFilter filter, File defaultFile) {
        // TODO: Avoid code duplication between this class and ConfirmationCodeDialog.
        // TODO: Do I need to remove all these listeners?
        super(parent);
        initComponents();
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        getRootPane().getActionMap().put("close", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
              dispose();
            }
          });
        getRootPane().setDefaultButton(okButton);
        okButton.requestFocusInWindow();
        setFile(defaultFile);

        fileChooser = new JFileChooser();
        fileChooser.setFileFilter(filter);
        fileChooser.setDialogTitle("Select File to Upload");

        browseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              StudyCaster.log.info("User pressed upload dialog's browse button");
              fileChooser.setSelectedFile(new File(fileNameTextBox.getText()));
              int res = fileChooser.showOpenDialog(UploadDialog.this);
              if (res == JFileChooser.APPROVE_OPTION)
                setFile(fileChooser.getSelectedFile());
            }
          });
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              StudyCaster.log.info("User pressed upload dialog's OK button");
              selectedFile = new File(fileNameTextBox.getText());
              if (!selectedFile.exists()) {
                StudyCaster.log.info("User chose a non-existent file");
                JOptionPane.showMessageDialog(UploadDialog.this,
                        "<html>" + selectedFile.getName() + "<br>File not found.<br>" +
                        "Please try again.</html>",
                        "File Upload",
                        JOptionPane.WARNING_MESSAGE);
                selectedFile = null;
              } else {
                dispose();
              }
            }
          });
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              StudyCaster.log.info("User pressed upload dialog's cancel button");
              dispose();
            }
          });
        addWindowListener(new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent e) {
            StudyCaster.log.info("Upload dialog closing");
            super.windowClosing(e);
          }
        });

        Dimension sdim = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension wdim = getSize();
        setLocation((sdim.width - wdim.width) / 2, (sdim.height - wdim.height) / 2);
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

    fileNameLabel = new javax.swing.JLabel();
    fileNameTextBox = new javax.swing.JTextField();
    browseButton = new javax.swing.JButton();
    infoLabel = new javax.swing.JLabel();
    okCancelPanel = new javax.swing.JPanel();
    cancelButton = new javax.swing.JButton();
    okButton = new javax.swing.JButton();

    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    setTitle("Upload and Retrieve Confirmation Code");
    setModal(true);
    setResizable(false);
    getContentPane().setLayout(new java.awt.GridBagLayout());

    fileNameLabel.setDisplayedMnemonic('F');
    fileNameLabel.setLabelFor(fileNameTextBox);
    fileNameLabel.setText("File to upload:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
    getContentPane().add(fileNameLabel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 10);
    getContentPane().add(fileNameTextBox, gridBagConstraints);

    browseButton.setMnemonic('B');
    browseButton.setText("Browse...");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(0, 10, 10, 10);
    getContentPane().add(browseButton, gridBagConstraints);

    infoLabel.setText("If you saved the file to a different location, select the new location here. Otherwise, just click OK.");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 10, 10, 10);
    getContentPane().add(infoLabel, gridBagConstraints);

    okCancelPanel.setLayout(new java.awt.GridBagLayout());

    cancelButton.setMnemonic('C');
    cancelButton.setText("Cancel");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.ipadx = 15;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
    okCancelPanel.add(cancelButton, gridBagConstraints);

    okButton.setMnemonic('O');
    okButton.setText("OK");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
    gridBagConstraints.ipadx = 30;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    okCancelPanel.add(okButton, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(15, 10, 10, 10);
    getContentPane().add(okCancelPanel, gridBagConstraints);

    pack();
  }// </editor-fold>//GEN-END:initComponents

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton browseButton;
  private javax.swing.JButton cancelButton;
  private javax.swing.JLabel fileNameLabel;
  private javax.swing.JTextField fileNameTextBox;
  private javax.swing.JLabel infoLabel;
  private javax.swing.JButton okButton;
  private javax.swing.JPanel okCancelPanel;
  // End of variables declaration//GEN-END:variables

}
