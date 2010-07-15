package no.ebakke.studycaster;

import java.util.Random;

public class Ticket {
  private static final int TICKET_BYTES = 8;
  private byte[] value;

  public Ticket() {
    value = new byte[TICKET_BYTES];
    new Random().nextBytes(value);
  }

  public Ticket(String s) throws NumberFormatException {
    if (s.length() != TICKET_BYTES * 2)
      throw new NumberFormatException();
    value = new byte[TICKET_BYTES];
    for (int i = 0; i < TICKET_BYTES; i++) {
      value[i] = (byte) (
                 ((Character.digit(s.charAt(i * 2 + 0), 16)) << 4) |
                 ((Character.digit(s.charAt(i * 2 + 1), 16)) << 0)
                 );
    }
  }

  @Override
  public String toString() {
    StringBuffer ret = new StringBuffer();
    for (int i = 0; i < TICKET_BYTES; i++) {
      ret.append(Character.forDigit((value[i] & 0xF0) >> 4, 16));
      ret.append(Character.forDigit((value[i] & 0x0F) >> 0, 16));
    }
    return ret.toString();
  }
}
