package no.ebakke.studycaster.servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import no.ebakke.studycaster.backend.BackendException;
import no.ebakke.studycaster.backend.Backend;
import no.ebakke.studycaster.backend.BackendConfiguration;
import org.apache.commons.lang3.StringEscapeUtils;

@WebServlet(name = "AdminActionsServlet", urlPatterns = {"/admin"})
public class AdminActionsServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException
  {
    try {
      String dbAction      = ServletUtil.getStringParam(req, "dbAction");
      String connectionURL = ServletUtil.getStringParam(req, "connectionURL");
      String newPassword = null;
      boolean create;
      if        (dbAction.equals("validate")) {
        create = false;
      } else if (dbAction.equals("create")) {
        newPassword = ServletUtil.getStringParam(req, "newPassword");
        create = true;
      } else {
        throw new BadRequestException("Invalid action \"" +
            StringEscapeUtils.escapeJava(dbAction) + "\"");
      }
      String msg;
      Backend testBackend = new Backend(new BackendConfiguration(connectionURL, null), create);
      try {
        msg = testBackend.getStatusMessage();
      } finally {
        testBackend.getSessionFactory().close();
      }
      resp.getWriter().print(msg);
    } catch (BadRequestException e) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
    }
  }
}
