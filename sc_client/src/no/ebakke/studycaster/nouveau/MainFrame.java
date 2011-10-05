package no.ebakke.studycaster.nouveau;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import no.ebakke.studycaster.configuration.UIStringKey;
import no.ebakke.studycaster.configuration.UIStrings;

public class MainFrame extends javax.swing.JFrame {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  private static final long serialVersionUID = 1L;
  private final JDialog positionDialog;
  private Dimension constantSize;

  /** Returns a hidden child dialog that can be used as a parent of dialogs that need to be centered
  on the screen while still being ancestors of the normally off-center MainFrame dialog. */
  public JDialog getPositionDialog() {
    return positionDialog;
  }

  private static Image getIconImage(String postFix) {
    return Toolkit.getDefaultToolkit().getImage(MainFrame.class.getClassLoader().getResource(
        "no/ebakke/studycaster/resources/icon" + postFix + ".png"));
  }

  private void initIcon() {
    try {
      Method setIconImagesMethod = null;
      try {
        setIconImagesMethod = Window.class.getMethod("setIconImages", List.class);
      } catch (NoSuchMethodException e) { }
      if (setIconImagesMethod == null) {
        // Running JRE < 1.6
        LOG.info("Can''t find Window.setIconImages(), using Frame.setIconImage() " +
            "instead (probably on JRE 1.5 or earlier)");
        setIconImage(getIconImage("256"));
      } else {
        // Running JRE >= 1.6
        List<Image> icons = new ArrayList<Image>();
        icons.add(getIconImage("16"));
        icons.add(getIconImage("22"));
        icons.add(getIconImage("24"));
        icons.add(getIconImage("32"));
        icons.add(getIconImage("48"));
        icons.add(getIconImage("64"));
        icons.add(getIconImage("128"));
        icons.add(getIconImage("256"));
        try {
          setIconImagesMethod.invoke(this, icons);
        } catch (IllegalAccessException e) {
          LOG.log(Level.WARNING, "Unexpected error while invoking Window.setIconImages()", e);
        } catch (InvocationTargetException e) {
          LOG.log(Level.WARNING, "Got unexpected exception from Window.setIconImages()", e);
        }
      }
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Failed to configure window icon", e);
    }
  }

  public void addUploadButtonAction(ActionListener l) {
    uploadButton.addActionListener(l);
  }
  
  public void addOpenButtonAction(ActionListener l) {
    openButton.addActionListener(l);
  }

  public void setButtonCaptions(UIStrings strings) {
    uploadButton.setText(strings.getString(UIStringKey.MAINFRAME_UPLOAD_BUTTON));
    uploadButton.setMnemonic(strings.getMnemonic(UIStringKey.MAINFRAME_UPLOAD_BUTTON));
    openButton.setText(strings.getString(UIStringKey.MAINFRAME_OPEN_BUTTON));
    openButton.setMnemonic(strings.getMnemonic(UIStringKey.MAINFRAME_OPEN_BUTTON));
    backButton.setText(strings.getString(UIStringKey.MAINFRAME_BACK_BUTTON));
    backButton.setMnemonic(strings.getMnemonic(UIStringKey.MAINFRAME_BACK_BUTTON));
    nextButton.setText(strings.getString(UIStringKey.MAINFRAME_NEXT_BUTTON));
    nextButton.setMnemonic(strings.getMnemonic(UIStringKey.MAINFRAME_NEXT_BUTTON));
  }

  public void setButtonsVisible(
      boolean uploadButtonVisible, boolean openButtonVisible, boolean navigationButtonsVisible)
  {
    uploadButton.setVisible(uploadButtonVisible);
    openButton.setVisible(openButtonVisible);
    backButton.setVisible(navigationButtonsVisible);
    nextButton.setVisible(navigationButtonsVisible);
  }

  public void setInstructions(String instructions) {
    instructionLabel.setText(instructions);
  }

  public void setProgressBarStatus(final String text, final boolean indeterminate) {
    LOG.log(Level.INFO, "Changing status bar text to \"{0}\"", text);
    progressBar.setString(text);
    progressBar.setIndeterminate(indeterminate);
    if (indeterminate)
      setProgressBarValue(0);
  }

  public void setProgressBarBounds(final int minimum, final int maximum) {
    progressBar.setMinimum(minimum);
    progressBar.setMaximum(maximum);
    progressBar.setIndeterminate(false);
  }

  public void setProgressBarValue(final int progress) {
    progressBar.setValue(progress);
  }

  /*
  public final void updateLayout() {
    pack();
    setSize(constantSize);
  }

  public final void measureSize(boolean enlargeOnly) {
    pack();
    if (enlargeOnly) {
      Dimension oldSize = constantSize;
      Dimension newSize = getSize();
      setSize(Math.max(oldSize.width, newSize.width), Math.max(oldSize.height, newSize.height));
    }
  }*/

  /** Relocate the window to a suitable position in the lower right-hand corner of the screen. */
  public final void updateLocation() {
    /* The following incantations were carefully derived through experimentation to work well with
    a variety of screen resultions and window sizes. */
    Dimension sdim = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension wdim = getSize();
    final int XMARGIN_MIN = 40;
    final int YMARGIN_MIN = 110;
    int xmargin = (int) (sdim.width  * 0.14) - wdim.width  / 2;
    int ymargin = (int) (sdim.height * 0.22) - wdim.height / 2;
    xmargin = Math.max(XMARGIN_MIN, xmargin);
    ymargin = Math.max(YMARGIN_MIN, ymargin);
    setLocation(sdim.width - wdim.width - xmargin, sdim.height - wdim.height - ymargin);
  }

  public MainFrame() {
    positionDialog = new JDialog(this);
    initComponents();
    initIcon();
    setButtonsVisible(false, false, false);
    // TODO: Remove.
    updateLocation();
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

        instructionLabel = new javax.swing.JLabel();
        actionButtonPanel = new javax.swing.JPanel();
        openButton = new javax.swing.JButton();
        uploadButton = new javax.swing.JButton();
        navigationPanel = new javax.swing.JPanel();
        backButton = new javax.swing.JButton();
        nextButton = new javax.swing.JButton();
        progressBar = new javax.swing.JProgressBar();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("StudyCaster");
        setAlwaysOnTop(true);
        setName("statusFrame"); // NOI18N
        setResizable(false);
        getContentPane().setLayout(new java.awt.GridBagLayout());

        instructionLabel.setText("Please wait...");
        instructionLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 15, 15);
        getContentPane().add(instructionLabel, gridBagConstraints);

        actionButtonPanel.setLayout(new java.awt.GridLayout(2, 1));

        openButton.setMnemonic('O');
        openButton.setText("Open Sample Document...");
        openButton.setEnabled(false);
        actionButtonPanel.add(openButton);

        uploadButton.setMnemonic('U');
        uploadButton.setText("Upload and Retrieve Confirmation Code...");
        uploadButton.setEnabled(false);
        actionButtonPanel.add(uploadButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
        getContentPane().add(actionButtonPanel, gridBagConstraints);

        navigationPanel.setLayout(new java.awt.GridLayout(1, 0));

        backButton.setMnemonic('B');
        backButton.setText("< Back");
        backButton.setEnabled(false);
        navigationPanel.add(backButton);

        nextButton.setMnemonic('N');
        nextButton.setText("Next >");
        nextButton.setEnabled(false);
        navigationPanel.add(nextButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 15, 15);
        getContentPane().add(navigationPanel, gridBagConstraints);

        progressBar.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        progressBar.setString("");
        progressBar.setStringPainted(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        getContentPane().add(progressBar, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel actionButtonPanel;
    private javax.swing.JButton backButton;
    private javax.swing.JLabel instructionLabel;
    private javax.swing.JPanel navigationPanel;
    private javax.swing.JButton nextButton;
    private javax.swing.JButton openButton;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JButton uploadButton;
    // End of variables declaration//GEN-END:variables
}
