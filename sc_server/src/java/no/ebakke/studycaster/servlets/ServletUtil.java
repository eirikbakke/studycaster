package no.ebakke.studycaster.servlets;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.StringEscapeUtils;

public final class ServletUtil {
  private ServletUtil() { }

  // See http://stackoverflow.com/questions/675730 .
  public static String getApplicationBase(HttpServletRequest req) {
    String scheme = req.getScheme().toLowerCase();
    int port = req.getServerPort();
    String portS = ("http".equals(scheme)  && port != 80 ||
                    "https".equals(scheme) && port != 443) ? (":" + port) : "";
    return scheme + "://" + req.getServerName() + portS + req.getContextPath();
  }

  public static String renderServletToString(
      String servlet, HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException
  {
    final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    final ServletOutputStream stream = new ServletOutputStream() {
      @Override
      public void write(int b) throws IOException {
        write(new byte[] {(byte) b}, 0, 1);
      }

      @Override
      public void write(byte[] b, int off, int len) throws IOException {
        byteStream.write(b, off, len);
      }
    };
    final PrintWriter writer = new PrintWriter(stream);
    ServletResponse responseWrapped = new HttpServletResponseWrapper(response) {
      @Override
      public PrintWriter getWriter() throws IOException {
        return writer;
      }

      @Override
      public ServletOutputStream getOutputStream() throws IOException {
        return stream;
      }
    };
    request.getServletContext().getRequestDispatcher(servlet).include(
      new HttpServletRequestWrapper(request), responseWrapped);
    writer.close();

    return byteStream.toString(response.getCharacterEncoding());
  }

  public static String getStringParam(
      HttpServletRequest req, String paramName) throws BadRequestException
  {
    return getParam(req.getParameterMap(), paramName);
  }

  public static String getMultipartStringParam(
      Map<String,FileItem[]> multiPart, String paramName)
          throws BadRequestException, ServletException
  {
    try {
      // TODO: Check encoding on client end.
      return getParam(multiPart, paramName).getString("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new ServletException(e);
    }
  }

  public static <R> R getParam(
      Map<String,R[]> map, String paramName) throws BadRequestException
  {
    R[] v = map.get(paramName);
    if (v == null) {
      throw new BadRequestException("Missing parameter \"" +
          StringEscapeUtils.escapeJava(paramName) + "\"");
    }
    if (v.length != 1) {
      throw new BadRequestException("Expected exactly one parameter \"" +
          StringEscapeUtils.escapeJava(paramName) + "\"; got " + v.length);
    }
    return v[0];
  }

  public static String ensureSafeString(String s) throws BadRequestException {
    if (s.contains("\'") || s.contains("\"") || s.contains("</")) {
      throw new BadRequestException("Cannot use unsafe string \"" +
          StringEscapeUtils.escapeJava(s) + "\" in this context");
    }
    return s;
  }
  
  public static Map<String,FileItem[]> parseMultiPart(
      HttpServletRequest req, int maxPartThreshold) throws BadRequestException
  {
    if (!ServletFileUpload.isMultipartContent(req))
      throw new BadRequestException("Expected a multipart HTTP request.");
    DiskFileItemFactory dfif = new DiskFileItemFactory();
    // TODO: Should this be set slightly higher?
    dfif.setSizeThreshold(maxPartThreshold);
    ServletFileUpload upload = new ServletFileUpload(dfif);
    upload.setFileSizeMax(maxPartThreshold);

    Map<String,FileItem[]> ret = new LinkedHashMap<String,FileItem[]>();
    try {
      for (Object item : upload.parseRequest(req)) {
        FileItem fileItem = (FileItem) item;
        FileItem[] val = ret.get(fileItem.getFieldName());
        if (val == null)
          val = new FileItem[0];
        FileItem[] newVal = Arrays.copyOf(val, val.length + 1);
        newVal[newVal.length - 1] = fileItem;
        ret.put(fileItem.getFieldName(), newVal);
      }
    } catch (FileUploadException e) {
      throw new BadRequestException("Could not parse multipart request: " +
          e.getMessage());
    }
    return ret;
  }

  public static File getSaneFile(File root, String untrustedRelativePath, boolean allowDir)
      throws BadRequestException, ServletException {
    // Intentionally do not differentiate error messages here.
    final String MSG = "Invalid path \"" +
        StringEscapeUtils.escapeJava(untrustedRelativePath) + "\"";
    File rootC;
    try {
      rootC = root.getCanonicalFile();
    } catch (IOException e) {
      throw new ServletException(e);
    }
    if (!rootC.isDirectory())
      throw new ServletException(new FileNotFoundException(rootC.toString()));

    File targetFile = new File(rootC, untrustedRelativePath);
    String base   = rootC.getPath();
    String target = targetFile.getParent();
    if (target.length() < base.length() ||
        !base.equals(target.substring(0, base.length())))
    {
      throw new BadRequestException(MSG);
    }
    if (!allowDir && targetFile.isDirectory())
      throw new BadRequestException(MSG);

    try {
      return targetFile.getCanonicalFile();
    } catch (IOException e) {
      throw new BadRequestException(MSG);
    }
  }
}
