package no.ebakke.studycaster.api;
import java.util.Random;

public class Ticket {
  private byte[] value;

  public Ticket(int length) {
    value = new byte[length];
    new Random().nextBytes(value);
  }

  public Ticket(String s) throws StudyCasterException {
    if (s.length() % 2 != 0)
      throw new StudyCasterException("Not a hex string (odd-length string).");
    int length = s.length() / 2;
    value = new byte[length];
    for (int i = 0; i < length; i++) {
      int dig1 = Character.digit(s.charAt(i * 2 + 0), 16);
      int dig0 = Character.digit(s.charAt(i * 2 + 1), 16);
      if (dig1 < 0 || dig0 < 0)
        throw new StudyCasterException("Not a hex string (bad character).");
      value[i] = (byte) ((dig1 << 4) | (dig0 << 0));
    }
  }

  public Ticket(String s, int length) throws StudyCasterException {
    this(s);
    byte[] oldValue = value;
    value = new byte[length];
    System.arraycopy(oldValue, 0, value, 0, Math.min(oldValue.length, length));
  }

  @Override
  public String toString() {
    StringBuffer ret = new StringBuffer();
    for (int i = 0; i < value.length; i++) {
      ret.append(Character.forDigit((value[i] & 0xF0) >> 4, 16));
      ret.append(Character.forDigit((value[i] & 0x0F) >> 0, 16));
    }
    return ret.toString();
  }
}
