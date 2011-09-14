package no.ebakke.studycaster.servlets;

import com.maxmind.geoip.LookupService;
import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import no.ebakke.studycaster.backend.Backend;
import no.ebakke.studycaster.backend.BackendConfiguration;
import no.ebakke.studycaster.backend.BackendUtil;

// TODO: Does this root mapping work on all containers?
@WebServlet(name = "AdminServlet", urlPatterns = {"/index.html"})
public class AdminServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  // TODO: Put these methods somewhere else.
  public static boolean isAdminLoggedIn(HttpServletRequest req, String password)
  {
    // TODO: Remove obvious security hole here.
    if (!Backend.INSTANCE.wasDBproperlyInitialized())
      return true;
    if (password != null)
      setAdminLoggedIn(req, BackendUtil.passwordMatches(password));
    HttpSession session = req.getSession(false);
    if (session == null)
      return false;
    Object attr = session.getAttribute("adminLoggedIn");
    if ((attr instanceof Boolean) && ((Boolean) attr))
      return true;
    return false;
  }

  public static void setAdminLoggedIn(HttpServletRequest req, boolean loggedIn)
  {
    // TODO: Should I rather figure out how to erase the session?
    req.getSession(true).setAttribute("adminLoggedIn", loggedIn);
  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException
  {
    resp.setHeader("Cache-Control", "no-cache");
    resp.setHeader("Pragma"       , "no-cache");
    if (req.getParameter("logout") != null)
      setAdminLoggedIn(req, false);

    try {
      String serverURL = ServletUtil.getApplicationBase(req);

      req.setAttribute("isAdminLoggedIn", isAdminLoggedIn(req,
          req.getParameter("pwd")));
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
      req.setAttribute("serverURLproperty" , BackendConfiguration.JDBC_URL_PROPERTY);
      req.setAttribute("storageDirProperty", BackendConfiguration.STORAGE_DIR_PROPERTY);
      req.setAttribute("backendStatus", Backend.INSTANCE.getStatusMessage());
      req.setAttribute("geoInfo", BackendUtil.getGeoInfo(req));

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
