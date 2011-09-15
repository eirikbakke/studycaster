package no.ebakke.studycaster.servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import no.ebakke.studycaster.backend.Backend;
import no.ebakke.studycaster.backend.BackendConfiguration;
import org.apache.commons.lang3.StringEscapeUtils;
import org.hibernate.HibernateException;

@WebServlet(name = "AdminActionsServlet", urlPatterns = {"/admin"})
public class AdminActionsServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException
  {
    if (!AdminServlet.isAdminLoggedIn(req, null)) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Not logged in.");
      return;
    }
    try {
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
          testBackend.getSessionFactory().close();
        } catch (HibernateException e){
          // Take no action.
        }
      }
      resp.getWriter().print(msg);
    } catch (BadRequestException e) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
    }
  }
}
