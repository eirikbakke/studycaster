package no.ebakke.studycaster.applications;

import java.io.*;
import no.ebakke.studycaster.screencasting.ExtendedMeta;
import no.ebakke.studycaster.screencasting.RecordingConverter;

/*
# Example usage from Cygwin terminal:
SCC_SCTOP=z:/studycaster2
SCC_JOPTS=""
SCC_CP="$SCC_SCTOP/lib/sc-resources/resources.jar;$SCC_SCTOP/lib/xuggler/xuggle-xuggler.jar;$SCC_SCTOP/sc_client/build/classes"
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
    final int speedUpFactor;
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
    final String fileNameBase = inputFileName.substring(0, inputFileName.length() - 4);
    File outFile = new File(fileNameBase + "_" +
        speedUpFactor + "x." + RecordingConverter.FILE_EXTENSION);
    File tmpFile = new File(outFile.getParentFile(), "~" + outFile.getName());
    File metaFile = new File(fileNameBase + "." + ExtendedMeta.FILE_EXTENSION);
    try {
      // Optimistic optimalization to avoid creating the temporary file most of the time.
      if (outFile.exists()) {
        System.err.println("Skipping already converted file " + outFile);
        return;
      }
      if (!tmpFile.createNewFile()) {
        System.err.println("Skipping incomplete file " + tmpFile);
        return;
      }
      if (outFile.exists()) {
        System.err.println("Skipping already converted file " + outFile);
        tmpFile.delete();
        return;
      }
      InputStream input;
      try {
        input = new FileInputStream(inputFileName);
      } catch (FileNotFoundException e) {
        System.err.println("File not found: " + inputFileName);
        return;
      }
      InputStream extendedMetaStream = null;
      try {
        extendedMetaStream = new FileInputStream(metaFile);
        System.err.println("Found extended metadata file " + metaFile.toString());
      } catch (FileNotFoundException e) {
        System.err.println("No extended metadata file found");
      }
      System.err.format("Converting to %s at %dx speedup: \n", outFile, speedUpFactor);
      RecordingConverter.convert(input, tmpFile.toString(), extendedMetaStream, speedUpFactor);
      tmpFile.renameTo(outFile);
    } catch (IOException e) {
      System.err.println("Got an error: " + e.getMessage());
      e.printStackTrace(System.err);
    }
  }
}
