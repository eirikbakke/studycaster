package no.ebakke.studycaster.servlets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public final class ServletUtil {
  private ServletUtil() { }

  // See http://stackoverflow.com/questions/675730 .
  public static String getApplicationBase(HttpServletRequest req) {
    String scheme = req.getScheme().toLowerCase();
    int port = req.getServerPort();
    String portS = ("http".equals(scheme)  && port != 80 ||
                    "https".equals(scheme) && port != 443) ? (":" + port) : "";
    return scheme + "://" + req.getServerName() + portS + req.getContextPath();
  }

  public static String renderServletToString(
      String servlet, HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException
  {
    final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    final ServletOutputStream stream = new ServletOutputStream() {
      @Override
      public void write(int b) throws IOException {
        write(new byte[] {(byte) b}, 0, 1);
      }

      @Override
      public void write(byte[] b, int off, int len) throws IOException {
        byteStream.write(b, off, len);
      }
    };
    final PrintWriter writer = new PrintWriter(stream);
    ServletResponse responseWrapped = new HttpServletResponseWrapper(response) {
      @Override
      public PrintWriter getWriter() throws IOException {
        return writer;
      }

      @Override
      public ServletOutputStream getOutputStream() throws IOException {
        return stream;
      }
    };
    request.getServletContext().getRequestDispatcher(servlet).include(
      new HttpServletRequestWrapper(request), responseWrapped);
    writer.close();

    return byteStream.toString(response.getCharacterEncoding());
  }

	/** Retrieve a string parameter from the request, throwing a
  BadRequestException if it was not supplied by the client. */
	public static String getStringParamChecked(
      HttpServletRequest req, String paramName) throws BadRequestException
	{
		String ret = req.getParameter(paramName);
		if (ret == null)
			throw new BadRequestException("Missing parameter \"" + paramName + "\"");
		return ret;
	}
}
