package no.ebakke.studycaster.nouveau;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JDialog;
import no.ebakke.studycaster.configuration.OpenFileConfiguration;
import no.ebakke.studycaster.configuration.UIStrings;

public class ActionState {
  private JDialog   positionDialog;
  private UIStrings strings;
  private Map<String,LocalFile> localFiles = new LinkedHashMap<String,LocalFile>();

  public ActionState(JDialog positionDialog, UIStrings strings) {
    this.positionDialog = positionDialog;
    this.strings = strings;
  }

  public void openFile(OpenFileConfiguration config) {
    if (!localFiles.containsKey(config.getLocalName())) {
      
    }
  }

  private static class LocalFile {
    private File   path;
    private byte[] hashCode;
  }
}
