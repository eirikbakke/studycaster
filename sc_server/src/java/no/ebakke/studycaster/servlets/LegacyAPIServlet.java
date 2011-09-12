package no.ebakke.studycaster.servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import no.ebakke.studycaster.domain.BackendException;
import no.ebakke.studycaster.domain.DomainUtil;
import org.apache.commons.lang3.StringEscapeUtils;
import org.hibernate.SessionFactory;

@WebServlet(name = "LegacyAPIServlet", urlPatterns = {"/client/legacy_api"})
public class LegacyAPIServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException
  {
    try {
      String cmd = ServletUtil.getStringParamChecked(req, "cmd");
      if        (cmd.equals("gsi")) {
        throw new BadRequestException("Not implemented");
      } else if (cmd.equals("log")) {
        throw new BadRequestException("Not implemented");
      } else if (cmd.equals("upc")) {
        throw new BadRequestException("Not implemented");
      } else if (cmd.equals("upa")) {
        throw new BadRequestException("Not implemented");
      } else if (cmd.equals("dnl")) {
        throw new BadRequestException("Not implemented");
      } else {
        throw new BadRequestException("Invalid command \"" +
            StringEscapeUtils.escapeJava(cmd) + "\"");
      }
    } catch (BadRequestException e) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
    }
  }
}
