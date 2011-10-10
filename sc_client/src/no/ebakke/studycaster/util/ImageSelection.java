package no.ebakke.studycaster.util;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import javax.swing.TransferHandler;

/* Simplified version of class with same name at
http://java.sun.com/developer/technicalArticles/releases/data/ */
public class ImageSelection extends TransferHandler implements Transferable {
  private static final long serialVersionUID = 1L;
  private static final DataFlavor FLAVOR = DataFlavor.imageFlavor;
  private Image image;

  public static void copyImageToClipBoard(Image image) {
    try {
      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageSelection(image), null);
    } catch (IllegalStateException e) {
      // TODO: Handle this better.
      System.err.println("Can't copy to clipboard");
    }
  }

  public ImageSelection(Image image) {
    this.image = image;
  }

  public Object getTransferData(DataFlavor flavor) {
    if (isDataFlavorSupported(flavor))
      return image;
    return null;
  }

  public DataFlavor[] getTransferDataFlavors() {
    return new DataFlavor[] {FLAVOR};
  }

  public boolean isDataFlavorSupported(DataFlavor flavor) {
    return flavor.equals(FLAVOR);
  }
}
