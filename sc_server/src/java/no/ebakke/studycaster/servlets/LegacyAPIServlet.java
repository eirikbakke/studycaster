package no.ebakke.studycaster.servlets;

import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringEscapeUtils;

@WebServlet(name = "LegacyAPIServlet", urlPatterns = {"/client/legacy_api"})
public class LegacyAPIServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final long MAX_FILE_SIZE = 50000000;
  private static final long MAX_APPEND_CHUNK = 1024 * 256;
  private static final int  SERVER_TICKET_BYTES = 3;
  private static final String DOWNLOAD_DIR = "files";
  private static final String UPLOAD_DIR   = "files/upload";

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    resp.getWriter().println("Version 4");
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException
  {
    System.out.println("++++++++++++++++++++++");
    for (Enumeration<String> e = req.getParameterNames(); e.hasMoreElements();)
      System.out.println(e.nextElement());
    System.out.println("++++++++++++++++++++++");
    try {
      String cmd = ServletUtil.getStringParamChecked(req, "cmd");
      if        (cmd.equals("gsi")) {
        
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
