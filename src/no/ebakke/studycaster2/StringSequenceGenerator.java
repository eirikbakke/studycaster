package no.ebakke.studycaster2;

public class StringSequenceGenerator {
  private String prefix, postfix;
  private int index = 0;

  public StringSequenceGenerator(String prefix, String postfix) {
    this.prefix  = prefix;
    this.postfix = postfix;
  }

  public synchronized String nextString() {
    if (index > 99999999)
      throw new IllegalStateException("Counted too far.");
    return prefix + String.format("%08d", index++) + postfix;
  }

  public static void main(String args[]) {
    StringSequenceGenerator sfng = new StringSequenceGenerator("TestPrefix_", ".upl");
    for (int i = 0; i < 10; i++)
      System.out.println(sfng.nextString());
  }
}
