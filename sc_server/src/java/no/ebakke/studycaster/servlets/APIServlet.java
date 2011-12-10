package no.ebakke.studycaster.servlets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;

@WebServlet(name = "APIServlet", urlPatterns = {"/client/api"})
public class APIServlet extends HttpServlet {
  private static final Logger LOG                 = Logger.getLogger("no.ebakke.studycaster");
  private static final long   serialVersionUID    = 1L;
  // TODO: Make this configurable.
  private static final long   MAX_FILE_SIZE       =  512 * 1024 * 1024;
  private static final long   MIN_AVAILABLE_SPACE = 1024 * 1024 * 1024;
  private static final int    MAX_APPEND_CHUNK    = 1024 * 256;
  private static final int    CLIENT_COOKIE_BYTES = 6;
  private static final int    LAUNCH_TICKET_BYTES = 6;
  public  static final int    IPHASH_SZ           = 3;
  private static final String UPLOAD_DIR          = "uploads";
  private Random random                           = new Random();
  // TODO: Figure out a better storage strategy.

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException
  {
    resp.setHeader("Cache-Control", "no-cache");
    resp.setHeader("Pragma"       , "no-cache");
    resp.setCharacterEncoding("UTF-8");

    Map<String,FileItem[]> multiPart = null;
    String cmd = null, logEntry = null, clientCookie = null, launchTicket = null;
    Long wroteContent = null;

    File storageDir = LifeCycle.getBackend(req).getStorageDirectory();
    File uploadDir = new File(storageDir, UPLOAD_DIR);
    uploadDir.mkdir();

    try {
      multiPart    = ServletUtil.parseMultiPart(req, MAX_APPEND_CHUNK);
      cmd          = ServletUtil.getMultipartStringParam(multiPart, "cmd");
      launchTicket = ServletUtil.getMultipartStringParam(multiPart, "lt");
      if (launchTicket.isEmpty()) {
        if (!cmd.equals("gsi"))
          throw new BadRequestException("Missing launch ticket");
        launchTicket = ServletUtil.toHex(ServletUtil.randomBytes(random, LAUNCH_TICKET_BYTES));
      }

      File ticketDir = ServletUtil.getSaneFile(uploadDir, launchTicket.toString(), true);
      ticketDir.mkdir();

      if        (cmd.equals("gsi")) {
        // Idempotent.
        clientCookie = ServletUtil.getMultipartStringParam(multiPart, "arg");
        if (clientCookie.isEmpty())
          clientCookie = ServletUtil.toHex(ServletUtil.randomBytes(random, CLIENT_COOKIE_BYTES));
        resp.setHeader("X-StudyCaster-LaunchTicket", launchTicket.toString());
        resp.setHeader("X-StudyCaster-ClientCookie", clientCookie.toString());
        resp.setHeader("X-StudyCaster-OK", "gsi");
      } else if (cmd.equals("tim")) {
        // Idempotent (read-only).
        final long realTimeNanos = System.currentTimeMillis() * 1000000L;
        final long tickTimeNanos = System.nanoTime();
        resp.setHeader("X-StudyCaster-RealTimeNanos", Long.toString(realTimeNanos));
        resp.setHeader("X-StudyCaster-TickTimeNanos", Long.toString(tickTimeNanos));
        resp.setHeader("X-StudyCaster-OK", "tim");
      } else if (cmd.equals("upc")) {
        // Idempotent.
        String base = ServletUtil.getMultipartStringParam(multiPart, "content");
        logEntry = base;
        File outFile = ServletUtil.getSaneFile(ticketDir, base, false);

        // TODO: Share similar rename functionality with client.
        /* Rename old files with the same name until we can create a new one
        with the specified name. The length check ensures idempotence. */
        while (!outFile.createNewFile() && outFile.length() > 0) {
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
        // Idempotent.
        String argS = ServletUtil.getMultipartStringParam(multiPart, "arg");
        long writtenArg;
        try {
          writtenArg = Long.parseLong(argS);
        } catch (NumberFormatException e) {
          throw new BadRequestException("Malformed integer argument");
        }
        FileItem content = ServletUtil.getParam(multiPart, "content");
        logEntry = content.getName();
        File outFile = ServletUtil.getSaneFile(ticketDir, content.getName(), false);
        long existingLength = outFile.length();
        if (existingLength + content.getSize() > MAX_FILE_SIZE) {
          throw new BadRequestException("File size reached limit",
              HttpServletResponse.SC_FORBIDDEN, true);
        }
        if (outFile.getUsableSpace() < MIN_AVAILABLE_SPACE) {
          LOG.severe("Server low on disk space");
          throw new BadRequestException("Server low on disk space",
              HttpServletResponse.SC_FORBIDDEN, true);
        }
        InputStream is = content.getInputStream();
        try {
          // This test ensures idempotency.
          if (writtenArg == existingLength) {
            wroteContent = content.getSize();
            OutputStream os = new FileOutputStream(outFile, true);
            try {
              IOUtils.copy(is, os);
            } finally {
              os.close();
            }
          } else {
            LOG.warning("Ignored double append.");
          }
        } finally {
          is.close();
        }
        resp.setHeader("X-StudyCaster-OK", "upa");
      } else if (cmd.equals("dnl")) {
        // Idempotent (read-only).
        String fileName = ServletUtil.getMultipartStringParam(multiPart, "content");
        logEntry = fileName;
        File input = ServletUtil.getSaneFile(storageDir, fileName, false);
        if (!input.exists()) {
          throw new BadRequestException("File \"" + StringEscapeUtils.escapeJava(fileName) +
              "\" not found", HttpServletResponse.SC_NOT_FOUND, true);
        } else {
          resp.setHeader("X-StudyCaster-OK", "dnl");
          ServletUtil.sendFile(resp, input, "application/octet-stream");
        }
      } else {
        throw new BadRequestException("Invalid command \"" +
            StringEscapeUtils.escapeJava(cmd) + "\"");
      }
    } catch (BadRequestException e) {
      e.sendError(resp);
    }
    // TODO: Split off locations into separate table.
    if (cmd != null && !cmd.equals("tim"))
      ServletUtil.logRequest(req, cmd, wroteContent, launchTicket, clientCookie, logEntry);
  }
}
