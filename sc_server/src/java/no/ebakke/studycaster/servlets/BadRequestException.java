package no.ebakke.studycaster.servlets;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

/* Define this exception to simplify control flow in Servlet request handlers. */
public class BadRequestException extends Exception {
	private static final long serialVersionUID = 1L;
  private int httpStatusCode = HttpServletResponse.SC_BAD_REQUEST;

	public BadRequestException(String message) {
		super(message);
	}

  public BadRequestException(String message, int httpStatusCode) {
    super(message);
    this.httpStatusCode = httpStatusCode;
  }

  public void sendError(HttpServletResponse resp) throws IOException {
    resp.sendError(httpStatusCode, getMessage());
  }
}
