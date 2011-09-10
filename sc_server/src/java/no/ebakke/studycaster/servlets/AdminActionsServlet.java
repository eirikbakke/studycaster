package no.ebakke.studycaster.servlets;

import java.io.IOException;
import java.sql.DriverManager;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import no.ebakke.studycaster.domain.DomainUtil;
import org.apache.commons.lang3.StringEscapeUtils;

@WebServlet(name = "AdminActionsServlet", urlPatterns = {"/admin"})
public class AdminActionsServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException
  {
    try {
      String dbAction      = ServletUtil.getStringParamChecked(req, "dbAction");
      String connectionURL = ServletUtil.getStringParamChecked(req, "connectionURL");
      String newPassword = null;
      boolean create;
      if (dbAction.equals("validate")) {
        create = false;
      } else if (dbAction.equals("create")) {
        newPassword = ServletUtil.getStringParamChecked(req, "newPassword");
        create = true;
      } else {
        throw new BadRequestException("Invalid action \"" +
            StringEscapeUtils.escapeJava(dbAction) + "\"");
      }
      Throwable error = null;
      try {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        Class.forName("com.mysql.jdbc.Driver");
        DriverManager.getConnection(connectionURL).close();
        DomainUtil.createHibernateConfiguration(connectionURL, create).buildSessionFactory().close();
      } catch (Throwable e) {
        error = e;
        /*
        System.err.println("===========================================");
        e.printStackTrace();
        System.err.println("===========================================");*/
      }
      resp.getWriter().print(error == null ? "Success." : error.toString());
    } catch (BadRequestException e) {
      //resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      //resp.getWriter().print(e.getMessage());
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
    }
  }
}
