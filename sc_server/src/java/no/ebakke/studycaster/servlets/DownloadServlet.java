package no.ebakke.studycaster.servlets;

import java.io.File;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import no.ebakke.studycaster.backend.BackendUtil;

@WebServlet(name = "DownloadServlet", urlPatterns = {"/download"})
public class DownloadServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException
  {
    if (!BackendUtil.isAdminLoggedIn(req, null)) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Not logged in.");
      return;
    }

    File storageDir = LifeCycle.getBackend(req).getStorageDirectory();
    try {
      File input = ServletUtil.getSaneFile(storageDir,
          ServletUtil.getStringParam(req, "path"), false);
      if (!input.exists()) {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      } else {
        String fileName = input.getName();
        String mimeType;
        if        (fileName.endsWith(".mp4")) {
          mimeType = "video/mp4";
        } else if (fileName.endsWith(".txt")) {
          mimeType = "text/plain";
        } else {
          mimeType = "application/octet-stream";
        }
        ServletUtil.sendFile(resp, input, mimeType);
      }
    } catch (BadRequestException e) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
    }
  }
}
