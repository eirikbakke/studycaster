package no.ebakke.studycaster.api;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

// TODO: Make this a couple of static utility functions instead.

/* This class needs to be serializable in order to be retained in the session
store across server restarts. */
public class Ticket implements Serializable {
  private static final long serialVersionUID = 1L;
  private static Random random = new Random();
  private byte[] value;

  public Ticket(int length) {
    value = new byte[length];
    random.nextBytes(value);
  }

  public Ticket(int length, String hashMe) {
    try {
      MessageDigest sha1 = MessageDigest.getInstance("SHA1");
      value = Arrays.copyOf(sha1.digest(hashMe.getBytes("UTF-8")), length);
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  /*
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
  */

  @Override
  public String toString() {
    StringBuilder ret = new StringBuilder();
    for (int i = 0; i < value.length; i++) {
      ret.append(Character.forDigit((value[i] & 0xF0) >> 4, 16));
      ret.append(Character.forDigit((value[i] & 0x0F)     , 16));
    }
    return ret.toString();
  }
}
