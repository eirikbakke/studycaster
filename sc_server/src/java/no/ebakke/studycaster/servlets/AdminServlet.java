package no.ebakke.studycaster.servlets;

import java.io.IOException;
import java.util.Date;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import no.ebakke.studycaster.backend.BackendConfiguration;
import no.ebakke.studycaster.backend.BackendUtil;
import no.ebakke.studycaster.reporting.Reports;

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
    resp.setCharacterEncoding("UTF-8");

    if (req.getParameter("logout") != null) {
      BackendUtil.setAdminLoggedIn(req, false);
      // Do a redirect to get rid of the ?logout at the end of the URL.
      resp.sendRedirect(ServletUtil.getApplicationBase(req));
      return;
    }

    try {
      final String serverURL = ServletUtil.getApplicationBase(req);
      final String password = req.getParameter("pwd");
      String pageType = req.getParameter("page");
      if (!BackendUtil.isAdminLoggedIn(req, password))
        pageType = "loggedOut";
      req.setAttribute("pageType", pageType);
      if (password != null) {
        // Do a redirect to avoid "resubmit form" warning when refreshing.
        resp.sendRedirect(ServletUtil.getApplicationBase(req));
        return;
      }
      /* Don't allow strings with characters that would need escaping, since deployJava.js doesn't
      support them. */
      req.setAttribute("serverURL", ServletUtil.ensureSafeString(serverURL));
      req.setAttribute("urlJNLP", ServletUtil.ensureSafeString(
          serverURL + JNLPServlet.JNLP_PATH + "?ci=" +
          JNLPServlet.DEFAULT_CONFIGURATION_ID + "&ver="));
      // TODO: Synchronize with JNLP file.
      req.setAttribute("minJavaVer", ServletUtil.ensureSafeString("1.5"));
      req.setAttribute("currentTime", new Date());

      if (pageType == null) {
        req.setAttribute("serverURLproperty" , BackendConfiguration.JDBC_URL_PROPERTY);
        req.setAttribute("storageDirProperty", BackendConfiguration.STORAGE_DIR_PROPERTY);
        req.setAttribute("backendStatus", LifeCycle.getBackend(req).getStatusMessage());
        req.setAttribute("geoInfo", BackendUtil.getGeoInfo(req));
      } else if (pageType.equals("subjectReport")){
        req.setAttribute("subjectReport",
            Reports.getSubjectReport(LifeCycle.getSessionFactory(req)));
        long highlightMinutes = 60;
        String highlightS = req.getParameter("highlight");
        if (highlightS != null) {
          try {
            highlightMinutes = Integer.parseInt(highlightS);
          } catch (NumberFormatException e) { }
        }
        req.setAttribute("highlightMinutes", highlightMinutes);
      }

      // TODO: Consider if there's a better way to do this.
      String scriptCode = ServletUtil.renderServletToString("/WEB-INF/jwsButton.jspx", req, resp);
      req.setAttribute("scriptCode", scriptCode);
      getServletContext().getRequestDispatcher("/WEB-INF/adminPage.jspx").forward(req, resp);

    } catch (BadRequestException e) {
      e.sendError(resp);
    }
  }
}
