package no.ebakke.studycaster.servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "AppletFrameServlet", urlPatterns = {"/client/applet_frame.html"})
public class AppletFrameServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException
  {
    resp.setHeader("Cache-Control", "no-cache");
    resp.setHeader("Pragma"       , "no-cache");
    resp.setCharacterEncoding("UTF-8");

    try {
      /* Judged to be safe to do this dynamically, since whatever URL was used to request this page
      should be a valid way to continue to reach this server for the runtime of the applet. */
      final String serverURL = ServletUtil.getApplicationBase(req);
      final String configurationID = ServletUtil.getStringParam(req, "ci");
      req.setAttribute("serverURL", ServletUtil.ensureSafeString(serverURL));
      req.setAttribute("codebaseURL", serverURL + JNLPServlet.JNLP_DIR);
      req.setAttribute("configurationID", configurationID);
      req.setAttribute("prompt", req.getParameter("prompt") != null);
      getServletContext().getRequestDispatcher("/WEB-INF/appletFrame.jspx").forward(req, resp);
      // TODO: Fix logging ugliness, see JNLPServlet.
      ServletUtil.logRequest(req, "apl", null, null, null, null);
      ServletUtil.logRequest(req, "cid", null, null, null, configurationID);
    } catch (BadRequestException e) {
      e.sendError(resp);
    }
  }
}
