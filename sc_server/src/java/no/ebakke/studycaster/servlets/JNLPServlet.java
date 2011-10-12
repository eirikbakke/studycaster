package no.ebakke.studycaster.servlets;

import java.io.IOException;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import no.ebakke.studycaster.backend.BackendUtil;
import no.ebakke.studycaster.backend.Request;
import org.apache.commons.lang3.StringEscapeUtils;

@WebServlet(name = "JNLPServlet",
    urlPatterns = {JNLPServlet.JNLP_PATH})
public class JNLPServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  /* To change JNLP_DIR, would also have to update project properties (Build->Packaging) and
  build.xml. */
  public static final String JNLP_DIR  = "/client";
  public static final String JNLP_FILE = "sc_client.jnlp";
  public static final String JNLP_PATH = JNLPServlet.JNLP_DIR + "/" + JNLPServlet.JNLP_FILE;
  public static final String DEFAULT_CONFIGURATION_ID = "default";

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp)
          throws ServletException, IOException
  {
    resp.setHeader("Cache-Control", "no-cache");
    resp.setHeader("Pragma"       , "no-cache");
    resp.setCharacterEncoding("UTF-8");

    /* Note: The content type is set by the <jsp:directive.page/> element in
    master-application.jnlp (in the client project). */
    resp.setHeader("Content-Disposition", "attachment; filename=\"" +
        StringEscapeUtils.escapeJava(JNLPServlet.JNLP_FILE) + "\"");

    final String configurationID = req.getParameter("ci");
    req.setAttribute("configurationID",
        configurationID == null ? DEFAULT_CONFIGURATION_ID : configurationID);
    /* TODO: Consider whether the server URL should be a configuration
    parameter rather than being derived dynamically. */
    final String serverURL = ServletUtil.getApplicationBase(req);
    req.setAttribute("codebaseURL", serverURL + JNLPServlet.JNLP_DIR);
    req.setAttribute("jnlpFile", JNLPServlet.JNLP_FILE);
    getServletContext().getRequestDispatcher("/WEB-INF/generatedJNLP.jspx").forward(req, resp);

    // TODO: Redesign request schema to avoid fields with either compound values or mostly nulls.
    // TODO: Avoid having to log two separate requests here, or rename "jws".
    ServletUtil.logRequest(req, "jws", null, null, null, req.getParameter("ver"));
    ServletUtil.logRequest(req, "cid", null, null, null, configurationID);
  }
}
