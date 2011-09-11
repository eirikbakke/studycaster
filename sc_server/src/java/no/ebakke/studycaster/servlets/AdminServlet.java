package no.ebakke.studycaster.servlets;

import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import no.ebakke.studycaster.domain.DomainUtil;

// TODO: Does this root mapping work on all containers?
@WebServlet(name = "AdminServlet", urlPatterns = {"/index.html"})
public class AdminServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException
  {
    resp.setHeader("Cache-Control", "no-cache");
    resp.setHeader("Pragma"       , "no-cache");
    try {
      String serverURL = ServletUtil.getApplicationBase(req);

      req.setAttribute("serverURL", serverURL);
      req.setAttribute("urlDeployScript", serverURL + "/deployJava.min.js");
      /* Don't allow strings with characters that would need escaping, since
      deployJava.js doesn't support them. */
      req.setAttribute("urlButtonImage", ServletUtil.ensureSafeString(
          serverURL + "/webstart_button.png"));
      req.setAttribute("urlJNLP", ServletUtil.ensureSafeString(
          serverURL + JNLPServlet.JNLP_PATH));
      // TODO: Synchronize with JNLP file.
      req.setAttribute("minJavaVer", ServletUtil.ensureSafeString("1.5"));
      req.setAttribute("jdbcURLproperty", DomainUtil.JDBC_URL_PROPERTY);
      req.setAttribute("dbStatus", DomainUtil.getStatus());

      // TODO: Consider if there's a better way to do this.
      String scriptCode = ServletUtil.renderServletToString(
          "/WEB-INF/jwsButton.jspx", req, resp);
      req.setAttribute("scriptCode", scriptCode);
      RequestDispatcher rd =
          getServletContext().getRequestDispatcher("/WEB-INF/adminPage.jspx");
      rd.forward(req, resp);

      /*for (Request r : DomainUtil.getRequests())
        System.out.println(r);*/
    } catch (BadRequestException e) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
    }
  }
}
