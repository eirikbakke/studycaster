package no.ebakke.studycaster.applications;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import no.ebakke.studycaster.screencasting.RecordingConverter;

public class Converter {
  // SCTOP=z:/Unfinished/ResearchRepo/archive/100713_StudyCaster/
  // find -name *.ebc -exec java -cp "$SCTOP/build/classes;$SCTOP/lib/xuggler/xuggle-xuggler.jar" no.ebakke.studycaster.applications.Converter {} 8 \;

  public static void main(String args[]) {
    if (args.length != 2) {
      System.err.println("usage: no.ebakke.studycaster.applications.Converter <input ebc file> <speedup>");
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
    String outputFileName = inputFileName.substring(0, inputFileName.length() - 4) + "_" + speedUpFactor + ".mkv";
    FileInputStream fis;
    try {
      fis = new FileInputStream(inputFileName);
    } catch (FileNotFoundException e) {
      System.err.println("File not found: " + inputFileName);
      return;
    }
    try {
      RecordingConverter.convert(new FileInputStream(inputFileName), outputFileName, speedUpFactor);
    } catch (IOException e) {
      System.err.println("Got an error: " + e.getMessage());
      e.printStackTrace();
      return;
    }
  }
}
