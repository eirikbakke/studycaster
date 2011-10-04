package no.ebakke.studycaster.api;

public class StudyCasterException extends Exception {
  private static final long serialVersionUID = 1L;

  public StudyCasterException(Throwable cause) {
    super(cause);
  }

  public StudyCasterException(String msg) {
    super(msg);
  }

  public StudyCasterException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
