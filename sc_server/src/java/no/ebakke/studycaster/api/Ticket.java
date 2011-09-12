package no.ebakke.studycaster.api;

import java.util.Random;

/* TODO: Get rid of this class. A more complete version is already in the same
package on the client. */
public class Ticket {
  private byte[] value;

  public Ticket(int length) {
    value = new byte[length];
    new Random().nextBytes(value);
  }

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
