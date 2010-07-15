package no.ebakke.studycaster;

public class StudyCasterException extends Exception {
  private static final long serialVersionUID = -5449643201941127164L;

  public StudyCasterException(Exception cause) {
    super(cause);
  }

  public StudyCasterException(String msg) {
    super(msg);
  }

  public StudyCasterException(String msg, Exception cause) {
    super(msg, cause);
  }
}
