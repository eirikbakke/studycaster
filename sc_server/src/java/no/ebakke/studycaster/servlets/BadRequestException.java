package no.ebakke.studycaster.servlets;

/* Define this exception to simplify control flow in Servlet request handlers. */
public class BadRequestException extends Exception {
	private static final long serialVersionUID = 1L;

	public BadRequestException(String message) {
		super(message);
    // TODO: Get rid of this.
    System.err.println("Creating a BadRequestException: " + message);
	}
}
