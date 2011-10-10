package no.ebakke.studycaster.screencasting;

final class CodecConstants {
  public static final String MAGIC_STRING  = "StudyCaster Screencast";
  public static final byte   MARKER_FRAME  = 1;
  public static final byte   MARKER_META   = 2;
  public static final byte   INDEX_NO_DIFF = (byte) -1;
  public static final byte   INDEX_REPEAT  = (byte) -2;

  private CodecConstants() { }
}
