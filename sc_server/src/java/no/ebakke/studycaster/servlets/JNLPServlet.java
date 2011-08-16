package no.ebakke.studycaster.servlets;

import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "JNLPServlet", urlPatterns = {"/sc_client.jnlp"})
public class JNLPServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp)
          throws ServletException, IOException
  {
    resp.setHeader("Cache-Control", "no-cache");
    resp.setHeader("Pragma"       , "no-cache");
    resp.setCharacterEncoding("UTF-8");

    if (req.getParameter("debug") != null) {
      resp.setContentType("text/html");
    } else {
      resp.setContentType("application/x-java-jnlp-file");
      resp.setHeader("Content-Disposition",
          "attachment; filename=\"sc_client.jnlp\"");
    }
    /* TODO: Consider whether the codebase URL should be a configuration
    parameter rather than being derived dynamically. */
    req.setAttribute("codebaseURL", ServletUtil.getApplicationBase(req));
    RequestDispatcher rd =
        getServletContext().getRequestDispatcher("/WEB-INF/client.jspx");
    rd.forward(req, resp);
  }
}
