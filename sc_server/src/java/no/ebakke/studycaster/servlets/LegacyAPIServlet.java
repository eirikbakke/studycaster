package no.ebakke.studycaster.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import no.ebakke.studycaster.api.Ticket;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;

@WebServlet(name = "LegacyAPIServlet", urlPatterns = {"/client/legacy_api"})
public class LegacyAPIServlet extends HttpServlet {
  private static final long   serialVersionUID = 1L;
  private static final int    MAX_FILE_SIZE = 50000000;
  private static final int    MAX_APPEND_CHUNK = 1024 * 256;
  private static final int    SERVER_TICKET_BYTES = 3;
  private static final String UPLOAD_DIR   = "upload";
  // TODO: Figure out a better storage strategy.
  private static final String STORAGE_DIR  = "z:/studycaster_storagedir";

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    resp.getWriter().println("Version 14");
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException
  {
    try {
      Map<String,FileItem[]> multiPart =
              ServletUtil.parseMultiPart(req, MAX_APPEND_CHUNK);

      File storageDir = new File(STORAGE_DIR);
      File uploadDir = new File(storageDir, UPLOAD_DIR);

      String cmd = ServletUtil.getMultipartStringParam(multiPart, "cmd");

      HttpSession session = req.getSession(false);
      Ticket serverTicket;
      if (cmd.equals("gsi")) {
        if (session != null)
          throw new BadRequestException("Session already established");
        session = req.getSession(true);
        // TODO: Use an IP hash. But then don't use it for file naming.
        serverTicket = new Ticket(SERVER_TICKET_BYTES);
        session.setAttribute("serverTicket", serverTicket);
      } else {
        if (session == null)
          throw new BadRequestException("Missing session");
        Object obj = session.getAttribute("serverTicket");
        if (obj == null || !(obj instanceof Ticket))
          throw new ServletException("Invalid session: " + obj);
        serverTicket = (Ticket) obj;
      }
      uploadDir.mkdir();
      File ticketDir = ServletUtil.getSaneFile(uploadDir, serverTicket.toString(), true);
      ticketDir.mkdir();
      if        (cmd.equals("gsi")) {
        resp.setHeader("X-StudyCaster-ServerTicket", serverTicket.toString());
        resp.setHeader("X-StudyCaster-ServerTime",
                Long.toString(new Date().getTime() / 1000));
        resp.setHeader("X-StudyCaster-OK", "gsi");
      } else if (cmd.equals("log")) {
        String content =
            ServletUtil.getMultipartStringParam(multiPart, "content");
        // TODO: Do proper logging here.
        System.err.println("Log entry from client: \"" +
            StringEscapeUtils.escapeJava(content) + "\"");
        resp.setHeader("X-StudyCaster-OK", "log");
      } else if (cmd.equals("upc")) {
        String base = ServletUtil.getMultipartStringParam(multiPart, "content");
        File outFile = ServletUtil.getSaneFile(ticketDir, base, false);

        /* Rename old files with the same name until we can create a new one
        with the specified name. */
        while (!outFile.createNewFile()) {
          int suffixNo = 1;
          do {
            File suffixedFile = ServletUtil.getSaneFile(ticketDir,
                base + "_" + Integer.toString(suffixNo), false);
            suffixNo++;
            if (suffixedFile.createNewFile()) {
              if (!outFile.renameTo(suffixedFile)) {
                suffixedFile.delete();
                if (!outFile.renameTo(suffixedFile))
                  throw new ServletException("Failed to rename existing file");
              }
              break;
            }
          } while (true);          
        }
        resp.setHeader("X-StudyCaster-OK", "upc");
      } else if (cmd.equals("upa")) {
        FileItem content = ServletUtil.getParam(multiPart, "content");
        File outFile = ServletUtil.getSaneFile(ticketDir, content.getName(), false);
        if (outFile.length() + content.getSize() > MAX_FILE_SIZE)
          throw new BadRequestException("File size reached limit");
        // TODO: Switch to RandomAccessFile and make idempotent.
        OutputStream os = new FileOutputStream(outFile, true);
        try {
          IOUtils.copy(content.getInputStream(), os);
        } finally {
          os.close();
        }
        resp.setHeader("X-StudyCaster-OK", "upa");
      } else if (cmd.equals("dnl")) {
        File input = ServletUtil.getSaneFile(storageDir,
            ServletUtil.getMultipartStringParam(multiPart, "content"), false);
        if (!input.exists()) {
          resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } else {
          resp.setContentType("application/octet-stream");
          resp.setHeader("Content-Disposition",
              "attachment; filename=" + input.getName());
          long length = input.length();
          if (length > Integer.MAX_VALUE)
            throw new BadRequestException("Requested file too large");
          resp.setContentLength((int) length);
          resp.setHeader("X-StudyCaster-OK", "dnl");
          // TODO: Check the number of bytes copied.
          InputStream is = new FileInputStream(input);
          try {
            IOUtils.copy(is, resp.getOutputStream());
          } finally {
            is.close();
          }
        }
      } else {
        throw new BadRequestException("Invalid command \"" +
            StringEscapeUtils.escapeJava(cmd) + "\"");
      }
    } catch (BadRequestException e) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
    }
  }
}
