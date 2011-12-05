package no.ebakke.studycaster.nouveau;

import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jnlp.SingleInstanceListener;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import no.ebakke.studycaster.api.ServerContext;
import no.ebakke.studycaster.api.ServerContextUtil;
import no.ebakke.studycaster.api.StudyCasterException;
import no.ebakke.studycaster.configuration.ConcludeConfiguration;
import no.ebakke.studycaster.configuration.OpenFileConfiguration;
import no.ebakke.studycaster.configuration.OpenURIConfiguration;
import no.ebakke.studycaster.configuration.StudyConfiguration;
import no.ebakke.studycaster.configuration.UIStringKey;
import no.ebakke.studycaster.configuration.UploadConfiguration;
import no.ebakke.studycaster.nouveau.MainFrame.UserActionListener;
import no.ebakke.studycaster.ui.UploadDialogPanel;
import no.ebakke.studycaster.util.Util;
import no.ebakke.studycaster.util.Util.CallableExt;

/*
  Manual test cases for this class:
  * Multiple application launches before and after configuration is loaded.
  * Close window before or after configuration is loaded, including right before the configuration
    is loaded such that the closing confirmation dialog still appears.
  * Failsafe shutdown 10 seconds after window is closed.
  * Regular shutdown after upload.
  * Regular shutdown after window closure.
  * Invoked with missing configuration ID property.
  * Configuration file error.
  * Window closed when error dialog is due to appear.
  * 1, 2, or 3 pages in the study configuration.
  * A study configuration with or without two action buttons on a single page in the study
    configuration.
  * Opening an example document in JRE 1.5 and in JRE 1.6 or above.
  * Opening an example document that already exists in the temporary folder, either modified or not,
    and either as the first time for a particular launch or not. When a file already exists in
    modified form, different messages should appear depending on whether it is the first time that
    file has been opened during the current launch or not.
  * Opening an example document of an association that cannot be resolved.
  * Opening an example document requiring renaming of a locked already existing file.
  * Opening an example document that does not already exist in the temporary folder.
  * Opening an example document while it's already opened exclusively (e.g. by Excel or Acrobat
    Reader).
  * Opening an example Excel spreadsheet, and correctly determining whether it has been modified by
    the user or not. (Excel will modify files upon opening them even before the user has done
    anything.)
  * Unexpected error while opening an example document (e.g. file is deleted during rename succeeded
    dialog).
  * Uploading downloaded but unchanged default file.
  * Uploading with non-existent default file.
  * Uploading locked file.
*/

// TODO: Rename to StudyCasterUI. Rename threads to reflect change.
// TODO: Pay attention to button focus.
public final class StudyUI {
  /* Except where noted, methods in this class, including private ones, must be called from the
  event-handling thread (EDT) only. Similarly, all non-final member variables must be accessed from
  the EDT. This avoids declaring members volatile, and makes it easier to reason about concurrency.
  */
  private static final Logger LOG = Logger.getLogger("no.ebakke.studycaster");
  private static final String CONFIGID_PROP_NAME = "studycaster.config.id";
  private final MainFrame mainFrame;
  private final EnvironmentHooks hooks;
  private final WindowListener windowClosingListener;
  private ServerContext serverContext;
  private StudyConfiguration configuration;
  private Thread initializerThread, failsafeCloseThread, backendCloseThread;

  private StudyUI(EnvironmentHooks hooks) {
    this.hooks = hooks;
    mainFrame = new MainFrame(new PrivateUserActionListener());
    windowClosingListener = new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        final boolean doClose;
        if (configuration != null) {
          LOG.info("User tried to close main StudyCaster window");
          doClose = JOptionPane.showConfirmDialog(mainFrame.getPositionDialog(),
              getUIString(UIStringKey.DIALOG_CLOSE_MESSAGE),
              getUIString(UIStringKey.DIALOG_CLOSE_TITLE),
              JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
          if (doClose) {
            LOG.info("User confirmed closing of main StudyCaster window");
          } else {
            LOG.info("User canceled closing of main StudyCaster window");
          }
        } else {
          doClose = true;
          LOG.info("User closed main StudyCaster window before configuration was loaded; exiting");
        }
        if (doClose)
          closeUIandBackend();
      }
    };
    mainFrame.addWindowListener(windowClosingListener);
  }

  private String getUIString(UIStringKey key) {
    return configuration.getUIStrings().getString(key);
  }

  private String getUIString(UIStringKey key, Object parameters[]) {
    return configuration.getUIStrings().getString(key, parameters);
  }

  // TODO: Rename, refactor, or combine closeUI() and closeBackend().
  // Before calling this function, consider whether the user may already have closed the window.
  private void closeUIandBackend() {
    closeUI();
    closeBackend(null);
  }

  private void closeUI() {
    if (failsafeCloseThread != null)
      throw new IllegalStateException("UI already closed");
    mainFrame.removeWindowListener(windowClosingListener);
    mainFrame.setVisible(false);
    failsafeCloseThread = new Thread(new Runnable() {
      public void run() {
        try {
          Thread.sleep(7000);
          LOG.warning("Forcing exit in three seconds (this may be last log message)");
          Thread.sleep(3000);
          LOG.warning("Forcing exit ten seconds after window closure");
        } catch (InterruptedException e) {
          LOG.warning("Failsafe exit thread interrupted; exiting immediately");
        }
        System.exit(1);
      }
    }, "StudyUI-failsafeClose");
    // Don't keep the VM running just because of the failsafe thread.
    failsafeCloseThread.setDaemon(true);
    failsafeCloseThread.start();
    /* Attempt to avoid race conditions that might make the window displayable again after the
    call to dispose() by putting the call at the end of the event queue. */
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        mainFrame.dispose();
      }
    });
  }

  private void closeBackend(final Runnable onEndEDT) {
    if (backendCloseThread != null)
      throw new IllegalStateException("Backend already closed");
    // Only access members from EDT.
    final Thread initializerThreadT = initializerThread;
    backendCloseThread = new Thread(new Runnable() {
      public void run() {
        if (initializerThreadT != null) {
          Util.ensureInterruptible(new Util.Interruptible() {
            public void run() throws InterruptedException {
              initializerThreadT.join();
            }
          });
        }
        EnvironmentHooks.shutdown();
        // In the case of an error, serverContext may still not be defined.
        if (serverContext != null)
          serverContext.close();
        if (onEndEDT != null)
          SwingUtilities.invokeLater(onEndEDT);
      }
    }, "StudyUI-backendClose");
    backendCloseThread.start();
  }

  private void reportGenericError(Exception e, boolean fatal) {
    final String TYPE = "Generic error (" + (fatal ? "fatal" : "non-fatal") + ")";
    if (failsafeCloseThread != null) {
      LOG.log(Level.SEVERE, TYPE + ", suppressing error dialog due to earlier close action", e);
    } else {
      LOG.log(Level.SEVERE, TYPE + ", showing error dialog", e);
      mainFrame.stopTask();
      JOptionPane.showMessageDialog(mainFrame.getPositionDialog(),
          "There was an unexpected error:\n" + e.getMessage(), "Error",
          JOptionPane.ERROR_MESSAGE);
      if (fatal)
        closeUIandBackend();
    }
  }

  /** Convenience method for showing a parameterized modal message dialog. The MainFrame taskbar
  will be reset prior to showing the dialog. This method may be called from any thread, including
  the EDT. The method blocks until the dialog is closed. */
  private void showMessageDialog(final UIStringKey messageKey,
      final Object messageParameters[], final UIStringKey titleKey, final int messageType)
  {
    if (failsafeCloseThread != null) {
      LOG.log(Level.INFO, "Suppressing dialog with message {0} due to earlier close action",
          messageKey.toString());
      return;
    }
    final String message = (messageParameters != null) ?
        getUIString(messageKey, messageParameters) : getUIString(messageKey);
    LOG.log(Level.INFO, "Showing dialog with message {0}", messageKey.toString());
    if (EventQueue.isDispatchThread()) {
      mainFrame.stopTask();
      JOptionPane.showMessageDialog(mainFrame.getPositionDialog(),
          message, getUIString(titleKey), messageType);
    } else {
      try {
        Util.checkedSwingInvokeAndWait(new CallableExt<Void,RuntimeException>() {
          public Void call() {
            mainFrame.stopTask();
            JOptionPane.showMessageDialog(mainFrame.getPositionDialog(),
                message, getUIString(titleKey), messageType);
            return null;
          }
        });
      } catch (InterruptedException e) {
        LOG.warning("showMessageDialog() interrupted");
        Thread.currentThread().interrupt();
      }
    }
  }

  private void initUI(StudyCasterException storedException) {
    try {
      if (storedException != null)
        throw storedException;
      if (failsafeCloseThread != null) {
        LOG.info("Window closed before UI was configured");
        /* Explicitly return here as the operations below may make the window displayable again,
        neutralizing an earlier dispose(). */
        return;
      }
      mainFrame.stopTask();
      mainFrame.configure(configuration, serverContext);

      // Do this after properly setting up the main window, in case there are enqueued messages.
      SingleInstanceHandler sih = hooks.getSingleInstanceHandler();
      if (sih != null) {
        sih.setListener(new SingleInstanceListener() {
          public void newActivation(String[] strings) {
            showMessageDialog(UIStringKey.DIALOG_ALREADY_RUNNING_MESSAGE, null,
                UIStringKey.DIALOG_ALREADY_RUNNING_TITLE, JOptionPane.INFORMATION_MESSAGE);
          }
        });
      }
    } catch (StudyCasterException e) {
      // Note: Due to the exception, configuration and serverContext may not be defined.
      reportGenericError(e, true);
    }
  }

  /** This method may only be called once during the lifetime of a StudyUI object. */
  public void runStudy(final String args[]) {
    if (initializerThread != null)
      throw new IllegalStateException("Already started");
    initializerThread = new Thread(new Runnable() {
      private ServerContext      serverContextT;
      private StudyConfiguration configurationT;

      public void run() {
        StudyCasterException exception = null;
        try {
          serverContextT = new ServerContext();
          hooks.getLogFormatter().setServerMillisAhead(serverContextT.getServerMillisAhead());
          try {
            hooks.getConsoleStream().connect(serverContextT.uploadFile("console.txt"));
            final String configurationID = System.getProperty(CONFIGID_PROP_NAME);
            if (configurationID == null)
              throw new StudyCasterException("Unspecified configuration ID");
            configurationT = StudyConfiguration.parseConfiguration(
              serverContextT.downloadFile("studyconfig.xml"), configurationID);
            LOG.log(Level.INFO, "Loaded configuration with name \"{0}\"", configurationT.getName());
          } catch (IOException e) {
            throw new StudyCasterException("Unexpected I/O error", e);
          }
        } catch (StudyCasterException e) {
          exception = e;
        }
        final StudyCasterException exceptionF = exception;
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            // Only access members from EDT.
            serverContext = serverContextT;
            configuration = configurationT;
            initUI(exceptionF);
          }
        });
      }
    }, "StudyUI-runStudy");

    /* To avoid a race condition with closeBackend(), initializerThread must be initialized before
    the UI can be displayed, and started after the UI has been displayed. */
    /* TODO: If configuration files ever get parsed on the server side, consider including the
    initial few UI strings as parameters in the JNLP file. */
    mainFrame.startTask(null, true);
    mainFrame.setVisible(true);
    initializerThread.start();
  }

  public static void main(final String args[]) {
    final EnvironmentHooks hooks = EnvironmentHooks.create();

    // Must be called before any UI components are rendered.
    try {
      UIUtil.setSystemLookAndFeel();
    } catch (StudyCasterException e) {
      LOG.log(Level.INFO, "Couldn''t set system L&F", e);
    }

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        new StudyUI(hooks).runStudy(args);
      }
    });
  }

  private class PrivateUserActionListener implements UserActionListener {
    /** No real concurrency should happen, but these maps are accessed from different threads. */
    private final Map<String,byte[]> hashesBeforeOpen = new ConcurrentHashMap<String,byte[]>();
    private final Map<String,byte[]> hashesAfterOpen  = new ConcurrentHashMap<String,byte[]>();
    private final File openFileDirectory;

    PrivateUserActionListener() {
      File rawOpenFileDirectory = new File(System.getProperty("java.io.tmpdir"));
      try {
        /* I/O operations should generally not go on the EDT, but this is a tiny one, and we do it
        in the constructor only once, before the main frame is shown. */
        rawOpenFileDirectory = rawOpenFileDirectory.getCanonicalFile();
      } catch (IOException e) {
        // Canonicalization of the path is only for cosmetic reasons, so take no action.
      }
      openFileDirectory = rawOpenFileDirectory;
    }

    private File getOpenFileDirectory() {
      return openFileDirectory;
    }

    private File getOpenFilePath(OpenFileConfiguration openFileConfiguration) {
      return new File(getOpenFileDirectory(), openFileConfiguration.getClientName());
    }

    private boolean fileIsModified(OpenFileConfiguration openFileConfiguration, File otherFile)
        throws IOException
    {
      final String key = openFileConfiguration.getClientName();
      byte[] otherHash = Util.computeSHA1(otherFile);
      byte[] hashAfterOpen = hashesAfterOpen.get(key);
      if (hashAfterOpen != null && Arrays.equals(otherHash, hashAfterOpen))
        return false;
      if (!hashesBeforeOpen.containsKey(key))
        downloadFile(openFileConfiguration).delete();
      byte[] hashBeforeOpen = hashesBeforeOpen.get(key);
      return !Arrays.equals(otherHash, hashBeforeOpen);
    }

    private File downloadFile(OpenFileConfiguration openFileConfiguration) throws IOException {
      final String key = openFileConfiguration.getClientName();
      File ret = File.createTempFile("sc_", ".tmp", getOpenFileDirectory());
      try {
        ServerContextUtil.downloadFile(serverContext, openFileConfiguration.getServerName(), ret);
        if (!hashesBeforeOpen.containsKey(key))
          hashesBeforeOpen.put(key, Util.computeSHA1(ret));
      } catch (IOException e) {
        ret.delete();
        throw e;
      }
      return ret;
    }

    /** Should not be called on the EDT. */
    private void openFileActionHelper(OpenFileConfiguration openFileConfiguration)
        throws StudyCasterException
    {
      final String key = openFileConfiguration.getClientName();
      final boolean openedInThisSession = hashesBeforeOpen.containsKey(key);
      File downloadedFile = null;
      try {
        LOG.log(Level.INFO, "Open action for file {0}", key);
        final File clientFile = getOpenFilePath(openFileConfiguration);
        if (!Util.fileAvailableExclusive(clientFile)) {
          showMessageDialog(UIStringKey.DIALOG_OPEN_FILE_ALREADY_MESSAGE,
              new Object[] { Util.getPathString(clientFile) }, UIStringKey.DIALOG_OPEN_FILE_TITLE,
              JOptionPane.INFORMATION_MESSAGE);
          return;
        }
        final boolean modified, useDownloaded;
        if (!clientFile.exists()) {
          useDownloaded = true;
          modified      = false;
        } else {
          /* Small optimization: call downloadFile() before fileModified() to avoid downloading the
          file both via fileModified() and the other call to downloadFile() below. */
          downloadedFile = downloadFile(openFileConfiguration);
          if (!fileIsModified(openFileConfiguration, clientFile)) {
            LOG.info("File already exists in unmodified form");
            // The existing file is identical to the downloaded one; so use the existing one.
            useDownloaded = false;
            modified      = false;
          } else {
            LOG.info("File already exists in modified form, showing option dialog");
            // The existing file is different from the downloaded one; ask the user what to do.
            int res = Util.checkedSwingInvokeAndWait(new Util.CallableExt<Integer,RuntimeException>() {
              public Integer call() {
                final String downloadOption = getUIString(UIStringKey.DIALOG_OPEN_FILE_NEW_BUTTON);
                final String existingOption = getUIString(UIStringKey.DIALOG_OPEN_FILE_KEEP_BUTTON);
                int ret = JOptionPane.showOptionDialog(mainFrame.getPositionDialog(),
                    getUIString(
                      openedInThisSession ? UIStringKey.DIALOG_OPEN_FILE_MODIFIED_MESSAGE
                                          : UIStringKey.DIALOG_OPEN_FILE_EXISTING_MESSAGE,
                      new Object[] { Util.getPathString(clientFile) }),
                    getUIString(UIStringKey.DIALOG_OPEN_FILE_TITLE),
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, new Object[] {downloadOption, existingOption}, existingOption);
                return ret;
              }
            });
            if (res == JOptionPane.CLOSED_OPTION) {
              LOG.info("User closed option window");
              return;
            }
            useDownloaded = (res == JOptionPane.YES_OPTION);
            modified = !useDownloaded;
            if (useDownloaded) {
              // Move the old file out of place without deleting it.
              String path = clientFile.getPath();
              int dot = path.lastIndexOf('.');
              String basename = (dot < 0) ? path : path.substring(0, dot);
              String extension = (dot < 0) ? "" : path.substring(dot);
              File newName;
              int index = 1;
              do {
                newName = new File(basename + " (" + index + ")" + extension);
                index++;
              } while (newName.exists());
              if (clientFile.renameTo(newName)) {
                showMessageDialog(UIStringKey.DIALOG_OPEN_FILE_RENAMED_MESSAGE,
                    new Object[] {Util.getPathString(clientFile), Util.getPathString(newName) },
                    UIStringKey.DIALOG_OPEN_FILE_TITLE, JOptionPane.INFORMATION_MESSAGE);
              } else {
                showMessageDialog(UIStringKey.DIALOG_OPEN_FILE_RENAME_FAILED_MESSAGE,
                    new Object[] { Util.getPathString(clientFile) },
                    UIStringKey.DIALOG_OPEN_FILE_TITLE, JOptionPane.WARNING_MESSAGE);
                // Leave it up to the user to try again.
                return;
              }
            }
          }
        }
        if (useDownloaded) {
          if (downloadedFile == null)
            downloadedFile = downloadFile(openFileConfiguration);
          /* An error here is unexpected, as any existing file should already have been moved out of
          the way at this point. */
          if (!downloadedFile.renameTo(clientFile))
            throw new IOException("Failed to rename");
        }
        if (!Util.desktopOpenFile(clientFile)) {
          showMessageDialog(UIStringKey.DIALOG_OPEN_FILE_ASSOCIATION_FAILED_MESSAGE,
              new Object[] { Util.getPathString(clientFile), openFileConfiguration.getErrorMessage() },
              UIStringKey.DIALOG_OPEN_FILE_TITLE, JOptionPane.WARNING_MESSAGE);
          return;
        }
        /* Certain applications, such as Microsoft Excel, will modify a file immediately upon
        opening it, possibly in a new way every time. For the purposes of advising the user about
        the status of opened files, consider a file to be unmodified if it matches either the
        originally downloaded file or the file in the state it was a short moment after it was
        opened in an unmodified state (presumably before the user would have had time to change
        it.) */
        if (modified) {
          // Once the file is considered modified, don't use hashAfterOpen for matching anymore.
          hashesAfterOpen.remove(key);
        } else {
          try {
            Thread.sleep(500);
          } catch (InterruptedException e) {
            throw new StudyCasterException(e);
          }
          hashesAfterOpen.put(key, Util.computeSHA1(clientFile));
        }
      } catch (IOException e) {
        throw new StudyCasterException(e);
      } catch (InterruptedException e) {
        throw new StudyCasterException(e);
      } finally {
        // This file won't exist in all cases, even if downloadedFile != null; that's OK.
        if (downloadedFile != null)
          downloadedFile.delete();
      }
    }

    private void processAction(final CallableExt<Void,StudyCasterException> callable) {
      new Thread(new Runnable() {
        public void run() {
          try {
            callable.call();
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                mainFrame.stopTask();
              }
            });
          } catch (final StudyCasterException e) {
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                reportGenericError(e, false);
              }
            });
          }
        }
      }).start();
    }

    public void openURIAction(final OpenURIConfiguration openURIConfiguration) {
      mainFrame.startTask(UIStringKey.PROGRESS_OPEN_URI, true);
      processAction(new CallableExt<Void,StudyCasterException>() {
        public Void call() throws StudyCasterException {
          Util.desktopOpenURI(openURIConfiguration.getURI());
          return null;
        }
      });
    }

    public void openFileAction(final OpenFileConfiguration openFileConfiguration) {
      mainFrame.startTask(UIStringKey.PROGRESS_OPEN_FILE, true);
      processAction(new CallableExt<Void,StudyCasterException>() {
        public Void call() throws StudyCasterException {
          openFileActionHelper(openFileConfiguration);
          return null;
        }
      });
    }

    /** Returns true if the operation should be retried. Should not be called on the EDT.
    openFileConfiguration may be null. */
    private void uploadFileHelper(final OpenFileConfiguration openFileConfiguration,
        final UploadDialogPanel udp)
        throws StudyCasterException, IOException, InterruptedException
    {
      boolean first = true;
      do {
        final boolean firstF = first;
        first = false;
        String selectedFilePath =
            Util.checkedSwingInvokeAndWait(new Util.CallableExt<String,RuntimeException>()
        {
          public String call() {
            if (firstF && openFileConfiguration != null)
              udp.setFilePath(getOpenFilePath(openFileConfiguration).getAbsolutePath());
            mainFrame.stopTask();
            int res = JOptionPane.showOptionDialog(mainFrame.getPositionDialog(), udp,
                getUIString(UIStringKey.DIALOG_CONCLUDE_TITLE), JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, null, null);
            if (res != JOptionPane.OK_OPTION)
              return null;
            mainFrame.startTask(UIStringKey.PROGRESS_UPLOAD_FILE, true);
            return udp.getFilePath();
          }
        });
        if (selectedFilePath == null)
          break;
        if (selectedFilePath.trim().length() == 0) {
          showMessageDialog(UIStringKey.DIALOG_CONCLUDE_EMPTY_PATH_MESSAGE, null,
              UIStringKey.DIALOG_CONCLUDE_TITLE, JOptionPane.INFORMATION_MESSAGE);
          continue;
        }
        final File selectedFile = new File(selectedFilePath);
        if (!selectedFile.exists()) {
          showMessageDialog(UIStringKey.DIALOG_CONCLUDE_FILE_NOT_EXISTS_MESSAGE,
              new Object[] { selectedFile.getAbsolutePath() },
              UIStringKey.DIALOG_CONCLUDE_TITLE, JOptionPane.WARNING_MESSAGE);
          continue;
        }
        if (!Util.fileAvailableExclusive(selectedFile)) {
          showMessageDialog(UIStringKey.DIALOG_CONCLUDE_FILE_OPEN_MESSAGE,
              new Object[] { selectedFile.getAbsolutePath() },
              UIStringKey.DIALOG_CONCLUDE_TITLE, JOptionPane.INFORMATION_MESSAGE);
          continue;
        }
        if (!fileIsModified(openFileConfiguration, selectedFile)) {
          showMessageDialog(UIStringKey.DIALOG_CONCLUDE_FILE_NOT_MODIFIED_MESSAGE,
              new Object[] { selectedFile.getAbsolutePath() },
              UIStringKey.DIALOG_CONCLUDE_TITLE, JOptionPane.INFORMATION_MESSAGE);
          continue;
        }
        // TODO: Upload now.
        Thread.sleep(1500);
        break;
      } while (true);
    }

    public void concludeAction(final ConcludeConfiguration concludeConfiguration) {
      if (concludeConfiguration.getUploadConfiguration() != null) {
        /* To avoid a race condition, briefly disable buttons while waiting for the upload dialog to
        appear. */
        mainFrame.startTask(null, false);
        final UploadDialogPanel udp = mainFrame.getUploadDialogPanel();
        final UploadConfiguration uploadConfiguration = concludeConfiguration.getUploadConfiguration();
        udp.setFileFilter(uploadConfiguration.getFileFilter());
        processAction(new CallableExt<Void,StudyCasterException>() {
          public Void call() throws StudyCasterException {
            OpenFileConfiguration originalFile = uploadConfiguration.getDefaultFile();
            if (originalFile != null && !getOpenFilePath(originalFile).exists()) {
              showMessageDialog(UIStringKey.DIALOG_CONCLUDE_FILE_NOT_OPENED_MESSAGE, null,
                  UIStringKey.DIALOG_CONCLUDE_TITLE, JOptionPane.INFORMATION_MESSAGE);
              return null;
            }
            try {
              uploadFileHelper(originalFile, udp);
            } catch (InterruptedException e) {
              throw new StudyCasterException(e);
            } catch (IOException e) {
              throw new StudyCasterException(e);
            }
            return null;
          }
        });
      } else {
        int res = JOptionPane.showConfirmDialog(mainFrame.getPositionDialog(),
            getUIString(UIStringKey.DIALOG_CONCLUDE_QUESTION),
            getUIString(UIStringKey.DIALOG_CONCLUDE_TITLE), JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        if (res != JOptionPane.OK_OPTION)
          return;
      }
      // TODO: Conclude study here.
    }
  }
}
