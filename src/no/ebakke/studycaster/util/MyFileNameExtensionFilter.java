package no.ebakke.studycaster.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.filechooser.FileFilter;

/* Modeled after the FileNameExtensionFilter class in Java 1.6 (we're targeting for 1.5). */
public class MyFileNameExtensionFilter extends FileFilter {
  private List<String> extensions;
  private String description;

  public MyFileNameExtensionFilter(List<String> extensions, String description) {
    this.extensions = new ArrayList<String>();
    for (String e : extensions)
      this.extensions.add(e.toLowerCase(Locale.ENGLISH));
    this.description = description;
  }

  @Override
  public boolean accept(File f) {
    String fn = f.getName();
    int at = fn.lastIndexOf(".");
    if (at < 0 || at + 1 >= fn.length())
      return false;
    String acte = fn.substring(at + 1).toLowerCase(Locale.ENGLISH);
    for (String e : extensions) {
      if (acte.equals(e))
        return true;
    }
    return false;
  }

  @Override
  public String getDescription() {
    return description;
  }
}
