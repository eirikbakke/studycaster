package no.ebakke.studycaster.servlets;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
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
import no.ebakke.studycaster.backend.Backend;
import no.ebakke.studycaster.backend.BackendUtil;
import no.ebakke.studycaster.backend.Request;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;

@WebServlet(name = "APIServlet", urlPatterns = {"/client/api"})
public class APIServlet extends HttpServlet {
  private static final long   serialVersionUID    = 1L;
  private static final int    MAX_FILE_SIZE       = 50000000;
  private static final int    MAX_APPEND_CHUNK    = 1024 * 256;
  private static final int    CLIENT_COOKIE_BYTES = 6;
  private static final int    LAUNCH_TICKET_BYTES = 6;
  private static final int    IPHASH_BYTES        = 3;
  private static final String UPLOAD_DIR          = "uploads";
  // TODO: Figure out a better storage strategy.

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException
  {
    Map<String,FileItem[]> multiPart = null;
    String cmd = null, logEntry = null, clientCookie = null,
        launchTicket = null;
    Long wroteContent = null;

    File storageDir = Backend.INSTANCE.getStorageDirectory();
    File uploadDir = new File(storageDir, UPLOAD_DIR);
    uploadDir.mkdir();

    try {
      multiPart    = ServletUtil.parseMultiPart(req, MAX_APPEND_CHUNK);
      cmd          = ServletUtil.getMultipartStringParam(multiPart, "cmd");
      launchTicket = ServletUtil.getMultipartStringParam(multiPart, "lt");
      if (launchTicket.isEmpty()) {
        if (!cmd.equals("gsi"))
          throw new BadRequestException("Missing launch ticket");
        launchTicket =
            ServletUtil.toHex(ServletUtil.randomBytes(LAUNCH_TICKET_BYTES));
      }

      File ticketDir =
          ServletUtil.getSaneFile(uploadDir, launchTicket.toString(), true);
      ticketDir.mkdir();

      if        (cmd.equals("gsi")) {
        // Idempotent.
        clientCookie = ServletUtil.getMultipartStringParam(multiPart, "arg");
        if (clientCookie.isEmpty()) {
          clientCookie =
              ServletUtil.toHex(ServletUtil.randomBytes(CLIENT_COOKIE_BYTES));
        }
        resp.setHeader("X-StudyCaster-LaunchTicket", launchTicket.toString());
        resp.setHeader("X-StudyCaster-ClientCookie", clientCookie.toString());
        resp.setHeader("X-StudyCaster-ServerTime"  ,
                Long.toString(new Date().getTime()));
        resp.setHeader("X-StudyCaster-OK", "gsi");
      } else if (cmd.equals("log")) {
        // TODO: Make this properly idempotent.
        String content =
            ServletUtil.getMultipartStringParam(multiPart, "content");
        String argS = ServletUtil.getMultipartStringParam(multiPart, "arg");
        logEntry = content + " (nonce=" + argS + ")";
        resp.setHeader("X-StudyCaster-OK", "log");
      } else if (cmd.equals("upc")) {
        // Idempotent.
        String base = ServletUtil.getMultipartStringParam(multiPart, "content");
        File outFile = ServletUtil.getSaneFile(ticketDir, base, false);

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
        if (existingLength + content.getSize() > MAX_FILE_SIZE)
          throw new BadRequestException("File size reached limit");
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
            // TODO: Do proper logging.
            System.err.println("Warning: Ignored double append.");
          }
        } finally {
          is.close();
        }
        resp.setHeader("X-StudyCaster-OK", "upa");
      } else if (cmd.equals("dnl")) {
        // Idempotent (read-only).
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
    // TODO: Split off locations into separate table.
    BackendUtil.storeRequest(new Request(new Date(), cmd, wroteContent,
        ServletUtil.toHex(ServletUtil.sha1("stick " + req.getRemoteAddr()),
        IPHASH_BYTES), BackendUtil.getGeoInfo(req), launchTicket, clientCookie,
        logEntry));
  }
}