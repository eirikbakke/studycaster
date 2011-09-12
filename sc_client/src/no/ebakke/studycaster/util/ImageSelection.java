package no.ebakke.studycaster.util;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import javax.swing.TransferHandler;

/* Simplified version of class with same name at
http://java.sun.com/developer/technicalArticles/releases/data/ */
public class ImageSelection extends TransferHandler implements Transferable {
  private static final long serialVersionUID = -5954556582641675851L;
  private static final DataFlavor flavors[] = {DataFlavor.imageFlavor};
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
    return flavors;
  }

  public boolean isDataFlavorSupported(DataFlavor flavor) {
    return flavor.equals(flavors[0]);
  }
}