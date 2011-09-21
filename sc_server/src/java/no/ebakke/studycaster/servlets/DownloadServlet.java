package no.ebakke.studycaster.servlets;

import java.io.File;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import no.ebakke.studycaster.backend.Backend;
import no.ebakke.studycaster.backend.BackendUtil;

@WebServlet(name = "DownloadServlet", urlPatterns = {"/download"})
public class DownloadServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException
  {
    if (!BackendUtil.isAdminLoggedIn(req, null)) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Not logged in.");
      return;
    }

    File storageDir = Backend.INSTANCE.getStorageDirectory();
    try {
      File input = ServletUtil.getSaneFile(storageDir,
          ServletUtil.getStringParam(req, "path"), false);
      if (!input.exists()) {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      } else {
        ServletUtil.sendFile(resp, input, false);
      }
    } catch (BadRequestException e) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
    }
  }
}
