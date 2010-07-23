package no.ebakke.studycaster2;

public class Main {
  public static void main(String args[]) throws Exception {
    //ServerContext sc = new ServerContext(new URL("http://www.sieuferd.com/studycaster/server.php"));
    //URL libLoc = Main.class.getClassLoader().getResource("no/ebakke/studycaster2/libNativeLib.dll");
    //String fileName = new File(libLoc.getFile()).getAbsolutePath();
    //String fileName = "Z:/Unfinished/ResearchRepo/archive/100713_StudyCaster/build/classes/no/ebakke/studycaster2/libNativeLib.dll";
    String fileName = "Z:/Unfinished/ResearchRepo/archive/100713_StudyCaster/NativeLib/libNativeLib.dll";
    //System.out.println(fileName);
    //System.load(fileName);
    //System.out.println("OOOO " + System.getProperty("java.library.path"));
    System.loadLibrary("libSCNative");

    while (true) {
      Thread.sleep(1000);
      System.out.println(NativeLibrary.getWindowArea("Notepad"));
    }
    //System.out.println("String from native library: " + NativeLibrary.getTestString());
  }
}
