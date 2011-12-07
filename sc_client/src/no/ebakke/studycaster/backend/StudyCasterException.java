package no.ebakke.studycaster.backend;

// TODO: See if it's possible to avoid the use of this custom exception.
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
