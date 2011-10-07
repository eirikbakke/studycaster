package no.ebakke.studycaster.servlets;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

/* Define this exception to simplify control flow in Servlet request handlers. */
public class BadRequestException extends Exception {
	private static final long serialVersionUID = 1L;
  private final int     httpStatusCode;
  private final boolean disableRetry;

	public BadRequestException(String message) {
		this(message, HttpServletResponse.SC_BAD_REQUEST);
	}

  public BadRequestException(String message, int httpStatusCode) {
    this(message, httpStatusCode, false);
  }

  public BadRequestException(String message, int httpStatusCode, boolean disableRetry) {
    super(message);
    this.httpStatusCode = httpStatusCode;
    this.disableRetry   = disableRetry;
  }

  public void sendError(HttpServletResponse resp) throws IOException {
    resp.setHeader("X-StudyCaster-DisableRetry", disableRetry ? "true" : "false");
    resp.setHeader("X-StudyCaster-ErrorMessage", getMessage());
    resp.sendError(httpStatusCode, getMessage());
  }
}
