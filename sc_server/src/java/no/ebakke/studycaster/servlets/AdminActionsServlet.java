package no.ebakke.studycaster.servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import no.ebakke.studycaster.backend.Backend;
import no.ebakke.studycaster.backend.BackendConfiguration;
import no.ebakke.studycaster.backend.BackendUtil;
import org.apache.commons.lang3.StringEscapeUtils;
import org.hibernate.HibernateException;

@WebServlet(name = "AdminActionsServlet", urlPatterns = {"/admin"})
public class AdminActionsServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException
  {
    resp.setHeader("Cache-Control", "no-cache");
    resp.setHeader("Pragma"       , "no-cache");
    resp.setCharacterEncoding("UTF-8");

    try {
      if (!BackendUtil.isAdminLoggedIn(req, null))
        throw new BadRequestException("Not logged in", HttpServletResponse.SC_FORBIDDEN);
      String dbAction      = ServletUtil.getStringParam(req, "dbAction");
      String connectionURL = ServletUtil.getStringParam(req, "connectionURL");
      String createAndSetPassword;
      if        (dbAction.equals("validate")) {
        createAndSetPassword = null;
      } else if (dbAction.equals("create")) {
        createAndSetPassword = ServletUtil.getStringParam(req, "newPassword");
      } else {
        throw new BadRequestException("Invalid action \"" +
            StringEscapeUtils.escapeJava(dbAction) + "\"");
      }
      String msg;
      Backend testBackend = new Backend(
          new BackendConfiguration(connectionURL, null), createAndSetPassword);
      try {
        msg = testBackend.getDatabaseStatusMessage();
      } finally {
        try {
          testBackend.close();
        } catch (HibernateException e){
          // Take no action.
        }
      }
      resp.getWriter().print(msg);
    } catch (BadRequestException e) {
      e.sendError(resp);
    }
  }
}
