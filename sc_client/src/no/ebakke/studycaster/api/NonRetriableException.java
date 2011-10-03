package no.ebakke.studycaster.api;

import java.io.IOException;

class NonRetriableException extends IOException {
  private static final long serialVersionUID = 1L;

  public NonRetriableException(String s) {
    super(s);
  }
}
