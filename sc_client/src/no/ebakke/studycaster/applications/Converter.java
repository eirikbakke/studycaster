package no.ebakke.studycaster.applications;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import no.ebakke.studycaster.screencasting.RecordingConverter;

/*
# Example usage from Cygwin terminal:
SCC_SCTOP=z:/studycaster2
SCC_JOPTS=""
SCC_CP="$SCC_SCTOP/lib/sc_icons/icons.jar;$SCC_SCTOP/lib/liberation-fonts/liberation-fonts.jar;$SCC_SCTOP/sc_client/build/classes;$SCC_SCTOP/lib/xuggler/xuggle-xuggler.jar"
find -name *.ebc -exec java $SCC_JOPTS -cp $SCC_CP no.ebakke.studycaster.applications.Converter {} 8 \;
*/

public final class Converter {
  private Converter() {}

  public static void main(String args[]) {
    if (args.length != 2) {
      System.err.println(
          "usage: no.ebakke.studycaster.applications.Converter <input ebc file> <speedup>");
      return;
    }
    String inputFileName = args[0];
    int speedUpFactor;
    try {
      speedUpFactor = Integer.parseInt(args[1]);
    } catch (NumberFormatException e) {
      System.err.println("Bad number format for speedup argument");
      return;
    }
    if (!inputFileName.endsWith(".ebc")) {
      System.err.println("Input file extension should be '.ebc'");
      return;
    }
    String outputFileName = inputFileName.substring(0, inputFileName.length() - 4) + "_" +
        speedUpFactor + "x." + RecordingConverter.FILE_EXTENSION;
    File outFile = new File(outputFileName);
    File tmpFile = new File(outFile.getParentFile(), "~" + outFile.getName());
    if (outFile.exists()) {
      System.err.println("Skipping existing file " + outputFileName);
      return;
    }
    FileInputStream fis;
    try {
      fis = new FileInputStream(inputFileName);
    } catch (FileNotFoundException e) {
      System.err.println("File not found: " + inputFileName);
      return;
    }
    try {
      System.err.format("Converting to %s at %dx speedup: \n", outFile.toString(), speedUpFactor);
      RecordingConverter.convert(fis, tmpFile.toString(), speedUpFactor);
      tmpFile.renameTo(outFile);
    } catch (IOException e) {
      System.err.println("Got an error: " + e.getMessage());
      e.printStackTrace(System.err);
      return;
    }
  }
}
