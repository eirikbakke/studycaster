package no.ebakke.studycaster.servlets;

import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import no.ebakke.studycaster.backend.Backend;
import no.ebakke.studycaster.backend.BackendConfiguration;
import no.ebakke.studycaster.backend.BackendUtil;

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
    if (req.getParameter("logout") != null) {
      BackendUtil.setAdminLoggedIn(req, false);
      // Do a redirect to get rid of the ?logout at the end of the URL.
      resp.sendRedirect(ServletUtil.getApplicationBase(req));
      return;
    }

    try {
      String serverURL = ServletUtil.getApplicationBase(req);

      String password = req.getParameter("pwd");
      req.setAttribute("isAdminLoggedIn", BackendUtil.isAdminLoggedIn(req, password));
      if (password != null) {
        // Do a redirect to avoid "resubmit form" warning when refreshing.
        resp.sendRedirect(ServletUtil.getApplicationBase(req));
        return;
      }
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

      String pageType = req.getParameter("page");
      req.setAttribute("pageType", pageType);
      if (pageType == null) {
        req.setAttribute("serverURLproperty" , BackendConfiguration.JDBC_URL_PROPERTY);
        req.setAttribute("storageDirProperty", BackendConfiguration.STORAGE_DIR_PROPERTY);
        req.setAttribute("backendStatus", Backend.INSTANCE.getStatusMessage());
        req.setAttribute("geoInfo", BackendUtil.getGeoInfo(req));
      } else {
      }

      // TODO: Consider if there's a better way to do this.
      String scriptCode = ServletUtil.renderServletToString(
          "/WEB-INF/jwsButton.jspx", req, resp);
      req.setAttribute("scriptCode", scriptCode);
      RequestDispatcher rd =
          getServletContext().getRequestDispatcher("/WEB-INF/adminPage.jspx");
      rd.forward(req, resp);

    } catch (BadRequestException e) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
    }
  }
}
