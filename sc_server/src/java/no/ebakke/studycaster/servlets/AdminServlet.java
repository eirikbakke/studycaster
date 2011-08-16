package no.ebakke.studycaster.servlets;

import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// TODO: Does this root mapping work on all containers?
@WebServlet(name = "AdminServlet", urlPatterns = {"/index.html"})
public class AdminServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException
  {
    resp.setHeader("Cache-Control", "no-cache");
    resp.setHeader("Pragma"       , "no-cache");
    String codebaseURL = ServletUtil.getApplicationBase(req);

    req.setAttribute("codebaseURL", codebaseURL);
    req.setAttribute("urlDeployScript", codebaseURL + "/deployJava.min.js");
    req.setAttribute("urlButtonImage", ServletUtil.quoteAndEscapeJS(
        codebaseURL + "/webstart_button.png"));
    // TODO: Keep the JNLP file name in a single location.
    req.setAttribute("urlJNLP", ServletUtil.quoteAndEscapeJS(
        codebaseURL + "/sc_client.jnlp"));
    // TODO: Synchronize with JNLP file.
    req.setAttribute("minJavaVer", ServletUtil.quoteAndEscapeJS("1.5"));

    // TODO: Consider if there's a better way to do this.
    String scriptCode =
        ServletUtil.renderServletToString("/WEB-INF/jwsscript.jspx", req, resp);
    req.setAttribute("scriptCode", scriptCode);
    RequestDispatcher rd =
        getServletContext().getRequestDispatcher("/WEB-INF/admin.jspx");
    rd.forward(req, resp);
  }
}
