package no.ebakke.studycaster.ui;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import no.ebakke.studycaster.backend.ServerContext;
import no.ebakke.studycaster.configuration.ConcludeConfiguration;
import no.ebakke.studycaster.configuration.OpenFileConfiguration;
import no.ebakke.studycaster.configuration.OpenURIConfiguration;
import no.ebakke.studycaster.configuration.PageConfiguration;
import no.ebakke.studycaster.configuration.StudyConfiguration;
import no.ebakke.studycaster.configuration.UIStringKey;
import no.ebakke.studycaster.configuration.UIStrings;

public class MainFrame extends javax.swing.JFrame {
  /** Public methods on this interface will be called on the EDT. */
  public static interface UserActionListener {
    public void openFileAction(OpenFileConfiguration openFileConfiguration);
    public void openURIAction(OpenURIConfiguration openURIConfiguration);
    public void concludeAction(ConcludeConfiguration concludeConfiguration);
    public void pageChanged();
  }

  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  private static final long serialVersionUID = 1L;
  private final UserActionListener userActionListener;
  private StudyConfiguration configuration;
  private Integer pageIndex;
  private boolean taskInProgress;
  private UploadDialogPanel uploadDialogPanel;
  private ConfirmationCodeDialogPanel confirmationCodeDialogPanel;
  private JButton previouslyClicked;

  public MainFrame(UserActionListener userActionListener) {
    this.userActionListener = userActionListener;
    initComponents();
    initIcon();
    configure(null, null);
  }

  public UploadDialogPanel getUploadDialogPanel() {
    if (configuration == null)
      throw new IllegalStateException("Not configured");
    return uploadDialogPanel;
  }

  public ConfirmationCodeDialogPanel getConfirmationCodeDialogPanel() {
    if (configuration == null)
      throw new IllegalStateException("Not configured");
    return confirmationCodeDialogPanel;
  }

  private void initIcon() {
    try {
      Method setIconImagesMethod = null;
      try {
        setIconImagesMethod = Window.class.getMethod("setIconImages", List.class);
      } catch (NoSuchMethodException e) { }
      if (setIconImagesMethod == null) {
        // Running JRE < 1.6
        LOG.info("Can't find Window.setIconImages(), probably on JRE 1.5 or earlier");
        /* Custom icons on JRE 1.5 or earlier look crummy due to inevitable low-quality scaling, so
        don't bother in this case. */
        // setIconImage(UIUtil.loadImage("icon256.png", false));
      } else {
        // Running JRE >= 1.6
        List<Image> icons = new ArrayList<Image>();
        icons.add(UIUtil.loadImage("icon16.png", false));
        icons.add(UIUtil.loadImage("icon22.png", false));
        icons.add(UIUtil.loadImage("icon24.png", false));
        icons.add(UIUtil.loadImage("icon32.png", false));
        icons.add(UIUtil.loadImage("icon48.png", false));
        icons.add(UIUtil.loadImage("icon64.png", false));
        icons.add(UIUtil.loadImage("icon128.png", false));
        icons.add(UIUtil.loadImage("icon256.png", false));
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

  private static Dimension max(Dimension dim1, Dimension dim2) {
    return new Dimension(Math.max(dim1.width, dim2.width), Math.max(dim1.height, dim2.height));
  }

  @SuppressWarnings("FinalMethod")
  public final void configure(StudyConfiguration configuration, ServerContext sc) {
    this.configuration = configuration;
    final boolean visible = isVisible();
    setVisible(false);
    try {
      if (configuration != null) {
        final UIStrings strings = configuration.getUIStrings();
        strings.get(UIStringKey.MAINFRAME_BACK_BUTTON).setOnButton(backButton);
        strings.get(UIStringKey.MAINFRAME_NEXT_BUTTON).setOnButton(nextButton);

        uploadDialogPanel           = new UploadDialogPanel(strings);
        confirmationCodeDialogPanel = new ConfirmationCodeDialogPanel(strings);

        /* Both the action button panel and the frame itself should remain the same size across
        different pages. */
        actionButtonPanel.setPreferredSize(null);
        Dimension actionButtonPanelDim = new Dimension(0, 0);
        for (int i = 0; i < configuration.getPageConfigurations().size(); i++) {
          setPageIndex(i, false);
          actionButtonPanelDim = max(actionButtonPanelDim, actionButtonPanel.getPreferredSize());
        }
        actionButtonPanel.setPreferredSize(actionButtonPanelDim);

        Dimension frameDim = new Dimension(0, 0);
        for (int i = 0; i < configuration.getPageConfigurations().size(); i++) {
          setPageIndex(i, false);
          pack();
          frameDim = max(frameDim, getSize());
        }
        setPageIndex(0, true);
        setSize(frameDim);
      } else {
        // Enforce a minimum window width; might as well use the actionButtonPanel to do this.
        actionButtonPanel.setPreferredSize(new Dimension(200, 0));
        setPageIndex(null, false);
        pack();
      }
      updateLocation();
    } finally {
      /* Make sure window never remains hidden due to an unexpected error, as the user should always
      be able to quit the application by closing the window. */
      setVisible(visible);
    }
  }

  private Integer getPageIndex() {
    return pageIndex;
  }

  public PageConfiguration getPageConfiguration() {
    return configuration.getPageConfigurations().get(getPageIndex());
  }

  private void setPageIndex(final Integer pageIndex, final boolean triggerEvent) {
    final String instructions;
    final boolean navigationButtonsVisible;
    final boolean openFileButtonVisible, openURIButtonVisible, concludeButtonVisible;
    final boolean changed = this.pageIndex != pageIndex;
    this.pageIndex = pageIndex;
    if (this.pageIndex == null) {
      instructionLabel.setText("Please wait...");
      openFileButtonVisible    = false;
      openURIButtonVisible     = false;
      concludeButtonVisible    = false;
      navigationButtonsVisible = false;
    } else {
      if (configuration == null)
        throw new IllegalStateException("Can't set page index before pages have been configured");
      PageConfiguration page = getPageConfiguration();
      OpenFileConfiguration openFileConfiguration = page.getOpenFileConfiguration();
      OpenURIConfiguration  openURIConfiguration  = page.getOpenURIConfiguration();
      ConcludeConfiguration concludeConfiguration = page.getConcludeConfiguration();

      openFileButtonVisible    = openFileConfiguration != null;
      openURIButtonVisible     = openURIConfiguration  != null;
      concludeButtonVisible    = concludeConfiguration != null;
      navigationButtonsVisible = configuration.getPageConfigurations().size() > 1;
      backButton.setEnabled(pageIndex > 0);
      nextButton.setEnabled(pageIndex < configuration.getPageConfigurations().size() - 1);
      if (openFileConfiguration != null)
        openFileConfiguration.getButtonText().setOnButton(openFileButton);
      if (openURIConfiguration != null)
        openURIConfiguration.getButtonText().setOnButton(openURIButton);
      if (concludeConfiguration != null)
        concludeConfiguration.getButtonText().setOnButton(concludeButton);
      page.getInstructions().setOnLabel(instructionLabel);
    }

    openFileButton.setEnabled(openFileButtonVisible);
    openFileButton.setVisible(openFileButtonVisible);
    openURIButton.setEnabled(openURIButtonVisible);
    openURIButton.setVisible(openURIButtonVisible);
    concludeButton.setEnabled(concludeButtonVisible);
    concludeButton.setVisible(concludeButtonVisible);
    navigationPanel.setVisible(navigationButtonsVisible);

    // Disable buttons during ongoing background tasks.
    if (taskInProgress) {
      openFileButton.setEnabled(false);
      openURIButton.setEnabled(false);
      concludeButton.setEnabled(false);
      backButton.setEnabled(false);
      nextButton.setEnabled(false);
    }

    if (changed) {
      previouslyClicked = null;
      advanceFocus();
    } else if (previouslyClicked != null){
      previouslyClicked.requestFocusInWindow();
    }
    if (triggerEvent) {
      LOG.log(Level.INFO, "Switching to page {0}/{1}",
          new Object[] { pageIndex, getPageConfiguration().getName() });
      userActionListener.pageChanged();
    }
  }

  /** Focus on the next button in logical order after any previously clicked one, or the first
  button if no button was previously clicked on this page. */
  private void advanceFocus() {
    boolean previousFound = (previouslyClicked == null);
    for (JButton button : new JButton[] {
      openFileButton, openURIButton, concludeButton, nextButton
    }) {
      if (previousFound && button.isEnabled()) {
        button.requestFocusInWindow();
        break;
      }
      if (button == previouslyClicked)
        previousFound = true;
    }
  }

  /** messageKey may be null. */
  public void startTask(UIStringKey messageKey, boolean indeterminate) {
    if (messageKey == null) {
      LOG.log(Level.INFO, "Starting status bar task with empty description");
    } else {
      LOG.log(Level.INFO, "Starting status bar task with message key {0}", messageKey);
    }
    taskInProgress = true;
    progressBar.setString(
        messageKey == null ? "" : configuration.getUIStrings().getString(messageKey));
    progressBar.setValue(0);
    setProgressBarBounds(0, 100);
    progressBar.setIndeterminate(indeterminate);
    // Update button enabled status.
    setPageIndex(getPageIndex(), false);
  }

  public void stopTask(boolean advanceFocus) {
    if (taskInProgress)
      LOG.log(Level.INFO, "Clearing status bar");
    taskInProgress = false;
    progressBar.setString("");
    progressBar.setIndeterminate(false);
    progressBar.setValue(0);
    // Update button enabled status.
    setPageIndex(getPageIndex(), false);
    if (advanceFocus)
      advanceFocus();
  }

  public void setProgressBarBounds(final int minimum, final int maximum) {
    if (!taskInProgress)
      throw new IllegalStateException("Task not started");
    progressBar.setMinimum(minimum);
    progressBar.setMaximum(maximum);
  }

  public void setProgressBarValue(final int progress) {
    if (!taskInProgress)
      throw new IllegalStateException("Task not started");
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
        openFileButton = new javax.swing.JButton();
        openURIButton = new javax.swing.JButton();
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

        instructionLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
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

        openFileButton.setMnemonic('D');
        openFileButton.setText("Open Sample Document...");
        openFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openFileButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        actionButtonPanel.add(openFileButton, gridBagConstraints);

        openURIButton.setMnemonic('W');
        openURIButton.setText("Open Sample Web Page...");
        openURIButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openURIButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        actionButtonPanel.add(openURIButton, gridBagConstraints);

        concludeButton.setMnemonic('C');
        concludeButton.setText("Upload and Retrieve Confirmation Code...");
        concludeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                concludeButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
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
    setPageIndex(getPageIndex() + 1, true);
  }//GEN-LAST:event_nextButtonActionPerformed

  private void backButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backButtonActionPerformed
    setPageIndex(getPageIndex() - 1, true);
  }//GEN-LAST:event_backButtonActionPerformed

  private void openFileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openFileButtonActionPerformed
    LOG.info("User pressed open file button");
    previouslyClicked = openFileButton;
    userActionListener.openFileAction(getPageConfiguration().getOpenFileConfiguration());
  }//GEN-LAST:event_openFileButtonActionPerformed

  private void concludeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_concludeButtonActionPerformed
    LOG.info("User pressed conclude button");
    previouslyClicked = concludeButton;
    userActionListener.concludeAction(getPageConfiguration().getConcludeConfiguration());
  }//GEN-LAST:event_concludeButtonActionPerformed

  private void openURIButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openURIButtonActionPerformed
    LOG.info("User pressed open URI button");
    previouslyClicked = openURIButton;
    userActionListener.openURIAction(getPageConfiguration().getOpenURIConfiguration());
  }//GEN-LAST:event_openURIButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel actionButtonPanel;
    private javax.swing.JButton backButton;
    private javax.swing.JButton concludeButton;
    private javax.swing.JLabel instructionLabel;
    private javax.swing.JPanel navigationPanel;
    private javax.swing.JPanel navigationPanelInner;
    private javax.swing.JButton nextButton;
    private javax.swing.JButton openFileButton;
    private javax.swing.JButton openURIButton;
    private javax.swing.JProgressBar progressBar;
    // End of variables declaration//GEN-END:variables
}
