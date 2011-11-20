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
import no.ebakke.studycaster.configuration.ConcludeConfiguration;
import no.ebakke.studycaster.configuration.OpenFileConfiguration;
import no.ebakke.studycaster.configuration.PageConfiguration;
import no.ebakke.studycaster.configuration.StudyConfiguration;
import no.ebakke.studycaster.configuration.UIStringKey;
import no.ebakke.studycaster.configuration.UIStrings;
import no.ebakke.studycaster.ui.ResourceUtil;

public class MainFrame extends javax.swing.JFrame {
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  private static final long serialVersionUID = 1L;
  private final JDialog positionDialog;
  private StudyConfiguration configuration;
  private Integer pageIndex = null;
  private boolean disposed = false;

  /** Returns a hidden child dialog that can be used as a parent of dialogs that need to be centered
  on the screen while still being descendants of the normally off-center MainFrame dialog. */
  public JDialog getPositionDialog() {
    return positionDialog;
  }

  private void initIcon() {
    try {
      Method setIconImagesMethod = null;
      try {
        setIconImagesMethod = Window.class.getMethod("setIconImages", List.class);
      } catch (NoSuchMethodException e) { }
      if (setIconImagesMethod == null) {
        // Running JRE < 1.6
        LOG.info("Can''t find Window.setIconImages(), probably on JRE 1.5 or earlier");
        // Custom icons on JRE 1.5 or earlier look crummy, so don't use them at all.
        // setIconImage(ResourceUtil.loadImage("icon256.png", false));
      } else {
        // Running JRE >= 1.6
        List<Image> icons = new ArrayList<Image>();
        icons.add(ResourceUtil.loadImage("icon16.png", false));
        icons.add(ResourceUtil.loadImage("icon22.png", false));
        icons.add(ResourceUtil.loadImage("icon24.png", false));
        icons.add(ResourceUtil.loadImage("icon32.png", false));
        icons.add(ResourceUtil.loadImage("icon48.png", false));
        icons.add(ResourceUtil.loadImage("icon64.png", false));
        icons.add(ResourceUtil.loadImage("icon128.png", false));
        icons.add(ResourceUtil.loadImage("icon256.png", false));
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

  public void addConcludeButtonAction(ActionListener l) {
    concludeButton.addActionListener(l);
  }
  
  public void addOpenButtonAction(ActionListener l) {
    openButton.addActionListener(l);
  }

  private static Dimension max(Dimension dim1, Dimension dim2) {
    return new Dimension(Math.max(dim1.width, dim2.width), Math.max(dim1.height, dim2.height));
  }

  @SuppressWarnings("FinalMethod")
  public final void setConfiguration(StudyConfiguration configuration) {
    this.configuration = configuration;
    final boolean visible = isVisible();
    setVisible(false);
    try {
      if (configuration != null) {
        final UIStrings strings = configuration.getUIStrings();
        concludeButton.setText(strings.getString(UIStringKey.MAINFRAME_UPLOAD_BUTTON));
        concludeButton.setMnemonic(strings.getMnemonic(UIStringKey.MAINFRAME_UPLOAD_BUTTON));
        openButton.setText(strings.getString(UIStringKey.MAINFRAME_OPEN_BUTTON));
        openButton.setMnemonic(strings.getMnemonic(UIStringKey.MAINFRAME_OPEN_BUTTON));
        backButton.setText(strings.getString(UIStringKey.MAINFRAME_BACK_BUTTON));
        backButton.setMnemonic(strings.getMnemonic(UIStringKey.MAINFRAME_BACK_BUTTON));
        nextButton.setText(strings.getString(UIStringKey.MAINFRAME_NEXT_BUTTON));
        nextButton.setMnemonic(strings.getMnemonic(UIStringKey.MAINFRAME_NEXT_BUTTON));

        /* Both the action button panel and the frame itself should remain the same size across
        different pages. */
        actionButtonPanel.setPreferredSize(null);
        Dimension actionButtonPanelDim = new Dimension(0, 0);
        for (int i = 0; i < configuration.getPageConfigurations().size(); i++) {
          setPageIndex(i);
          actionButtonPanelDim = max(actionButtonPanelDim, actionButtonPanel.getPreferredSize());
        }
        actionButtonPanel.setPreferredSize(actionButtonPanelDim);

        Dimension frameDim = new Dimension(0, 0);
        for (int i = 0; i < configuration.getPageConfigurations().size(); i++) {
          setPageIndex(i);
          pack();
          frameDim = max(frameDim, getSize());
        }
        setPageIndex(0);
        setSize(frameDim);
      } else {
        // Enforce a minimum window width; might as well use the actionButtonPanel to do this.
        actionButtonPanel.setPreferredSize(new Dimension(200, 0));
        setPageIndex(null);
        pack();
      }
      updateLocation();
    } finally {
      /* Make sure window never remains hidden due to an unexpected error, as the user should always
      be able to quit the application by closing the window. */
      setVisible(visible);
    }
  }

  public Integer getPageIndex() {
    return pageIndex;
  }

  // TODO: Log page changes.
  private void setPageIndex(Integer pageIndex) {
    final String instructions;
    final boolean navigationButtonsVisible;
    final boolean concludeButtonVisible;
    final boolean openButtonVisible;
    this.pageIndex = pageIndex;
    if (this.pageIndex == null) {
      instructions = "Please wait...";
      concludeButtonVisible    = false;
      openButtonVisible        = false;
      navigationButtonsVisible = false;
    } else {
      if (configuration == null)
        throw new IllegalStateException("Can't set page index before pages have been configured");
      PageConfiguration page = configuration.getPageConfigurations().get(pageIndex);
      OpenFileConfiguration openFileConfiguration = page.getOpenFileConfiguration();
      ConcludeConfiguration concludeConfiguration = page.getConcludeConfiguration();

      instructions             = page.getInstructions();
      concludeButtonVisible    = concludeConfiguration != null;
      openButtonVisible        = openFileConfiguration != null;
      navigationButtonsVisible = configuration.getPageConfigurations().size() > 1;
      backButton.setEnabled(pageIndex > 0);
      nextButton.setEnabled(pageIndex < configuration.getPageConfigurations().size() - 1);
    }
    instructionLabel.setText(instructions);
    concludeButton.setEnabled(concludeButtonVisible);
    openButton.setEnabled(openButtonVisible);

    actionButtonPanel.setMinimumSize(new Dimension(800, 600));
    concludeButton.setVisible(concludeButtonVisible);
    openButton.setVisible(openButtonVisible);

    navigationPanel.setVisible(navigationButtonsVisible);

    // Disable buttons during ongoing background tasks.
    if (progressBar.getString().length() != 0 ||
        progressBar.isIndeterminate() ||
        progressBar.getValue() != 0)
    {
      concludeButton.setEnabled(false);
      openButton.setEnabled(false);
      backButton.setEnabled(false);
      nextButton.setEnabled(false);
    }
  }

  public void setProgressBarStatus(final String text, final boolean indeterminate) {
    LOG.log(Level.INFO, "Changing status bar text to \"{0}\"", text);
    progressBar.setString(text);
    progressBar.setIndeterminate(indeterminate);
    if (indeterminate || text.length() == 0)
      setProgressBarValue(0);
    // Enable or disable buttons as appropriate.
    setPageIndex(getPageIndex());
  }

  public void setProgressBarBounds(final int minimum, final int maximum) {
    progressBar.setMinimum(minimum);
    progressBar.setMaximum(maximum);
    progressBar.setIndeterminate(false);
  }

  public void setProgressBarValue(final int progress) {
    progressBar.setValue(progress);
  }

  /** Relocate the window to a suitable position in the lower right-hand corner of the screen. */
  private void updateLocation() {
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
    setConfiguration(null);
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
        concludeButton = new javax.swing.JButton();
        navigationPanel = new javax.swing.JPanel();
        navigationPanelInner = new javax.swing.JPanel();
        backButton = new javax.swing.JButton();
        nextButton = new javax.swing.JButton();
        progressBar = new javax.swing.JProgressBar();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("StudyCaster");
        setAlwaysOnTop(true);
        setName("statusFrame"); // NOI18N
        setResizable(false);
        getContentPane().setLayout(new java.awt.GridBagLayout());

        instructionLabel.setText("Instructions go here.");
        instructionLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 15, 15);
        getContentPane().add(instructionLabel, gridBagConstraints);

        actionButtonPanel.setOpaque(false);
        actionButtonPanel.setLayout(new java.awt.GridBagLayout());

        openButton.setMnemonic('O');
        openButton.setText("Open Sample Document...");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        actionButtonPanel.add(openButton, gridBagConstraints);

        concludeButton.setMnemonic('U');
        concludeButton.setText("Upload and Retrieve Confirmation Code...");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        actionButtonPanel.add(concludeButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 15, 15);
        getContentPane().add(actionButtonPanel, gridBagConstraints);

        navigationPanel.setOpaque(false);
        navigationPanel.setLayout(new java.awt.GridBagLayout());

        navigationPanelInner.setOpaque(false);
        navigationPanelInner.setLayout(new java.awt.GridLayout(1, 2));

        backButton.setMnemonic('B');
        backButton.setText("< Back");
        backButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backButtonActionPerformed(evt);
            }
        });
        navigationPanelInner.add(backButton);

        nextButton.setMnemonic('N');
        nextButton.setText("Next >");
        nextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextButtonActionPerformed(evt);
            }
        });
        navigationPanelInner.add(nextButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 0);
        navigationPanel.add(navigationPanelInner, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
        getContentPane().add(navigationPanel, gridBagConstraints);

        progressBar.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        progressBar.setString("");
        progressBar.setStringPainted(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        getContentPane().add(progressBar, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

  private void nextButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextButtonActionPerformed
    setPageIndex(getPageIndex() + 1);
  }//GEN-LAST:event_nextButtonActionPerformed

  private void backButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backButtonActionPerformed
    setPageIndex(getPageIndex() - 1);
  }//GEN-LAST:event_backButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel actionButtonPanel;
    private javax.swing.JButton backButton;
    private javax.swing.JButton concludeButton;
    private javax.swing.JLabel instructionLabel;
    private javax.swing.JPanel navigationPanel;
    private javax.swing.JPanel navigationPanelInner;
    private javax.swing.JButton nextButton;
    private javax.swing.JButton openButton;
    private javax.swing.JProgressBar progressBar;
    // End of variables declaration//GEN-END:variables
}
