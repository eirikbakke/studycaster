package no.ebakke.studycaster.servlets;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import no.ebakke.studycaster.backend.BackendUtil;
import no.ebakke.studycaster.backend.Request;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
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

    File targetC;
    try {
      targetC = (new File(rootC, untrustedRelativePath)).getCanonicalFile();
    } catch (IOException e) {
      throw new BadRequestException(MSG);
    }
    if (!allowDir && targetC.isDirectory())
      throw new BadRequestException(MSG);
    String base   = rootC.getPath();
    String target = targetC.getParent();
    if (target.length() < base.length() || !base.equals(target.substring(0, base.length()))) {
      throw new BadRequestException(MSG);
    }
    return targetC;
  }

  public static String toHex(byte[] value) {
    return toHex(value, value.length);
  }

  public static String toHex(byte[] value, int length) {
    StringBuilder ret = new StringBuilder();
    for (int i = 0; i < length; i++) {
      ret.append(Character.forDigit((value[i] & 0xF0) >> 4, 16));
      ret.append(Character.forDigit((value[i] & 0x0F)     , 16));
    }
    return ret.toString();
  }

  public static byte[] sha1(String input) {
    try {
      MessageDigest sha1 = MessageDigest.getInstance("SHA1");
      return sha1.digest(input.getBytes("UTF-8"));
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public static byte[] randomBytes(Random random, int length) {
    byte[] ret = new byte[length];
    random.nextBytes(ret);
    return ret;
  }

  public static void sendFile(HttpServletResponse resp, File input, String mimeType)
      throws IOException, BadRequestException
  {
    resp.setContentType(mimeType);
    if (mimeType.equals("application/octet-stream")) {
      resp.setHeader("Content-Disposition",
          "attachment; filename=" + input.getName());
    }
    long length = input.length();
    if (length > Integer.MAX_VALUE)
      throw new BadRequestException("Requested file too large", HttpServletResponse.SC_FORBIDDEN);
    resp.setContentLength((int) length);
    // TODO: Check the number of bytes copied.
    InputStream is = new FileInputStream(input);
    try {
      IOUtils.copy(is, resp.getOutputStream());
    } finally {
      is.close();
    }
  }

  public static String humanReadableInterval(long seconds) {
    return humanReadableInterval(seconds, Integer.MAX_VALUE);
  }

  public static String humanReadableInterval(long seconds, int maxComponents) {
    StringBuilder ret = new StringBuilder();
    long remainingSeconds = seconds;
    if (remainingSeconds < 0) {
      ret.append("-");
      remainingSeconds = -remainingSeconds;
    }
    boolean first = true;
    int n = 0;
    for (TimeUnit tu : TimeUnit.UNITS) {
      long inUnit = remainingSeconds / tu.seconds;
      if (inUnit > 0 || (tu.seconds == 1 && first)) {
        if (!first)
          ret.append(" ");
        ret.append(inUnit);
        ret.append(" ");
        ret.append(inUnit == 1 ? tu.singular : tu.plural);
        remainingSeconds -= inUnit * tu.seconds;
        first = false;
        n++;
        if (n >= maxComponents)
          break;
      }
    }
    return ret.toString();
  }

  private static class TimeUnit {
    public static final List<TimeUnit> UNITS = Arrays.asList(new TimeUnit[] {
      new TimeUnit("yrs", "yr" , 365 * 24 * 60 * 60),
      new TimeUnit("mos", "mo" ,  30 * 24 * 60 * 60),
      new TimeUnit("wks", "wk" ,   7 * 24 * 60 * 60),
      new TimeUnit("dys", "dy" ,       24 * 60 * 60),
      new TimeUnit("hrs", "hr" ,            60 * 60),
      new TimeUnit("min", "min",                 60),
      new TimeUnit("sec", "sec",                  1)
    });

    String plural, singular;
    long   seconds;

    TimeUnit(String plural, String singular, long seconds) {
      this.plural = plural;
      this.singular = singular;
      this.seconds = seconds;
    }
  }

  public static String getMimeType(ServletContext ctx, String file) {
    String lower = file.toLowerCase();
    if (lower.endsWith(".mp4")) {
      return "video/mp4";
    } else if (lower.endsWith(".txt")) {
      return "text/plain";
    } else {
      return ctx.getMimeType(file);
    }
  }

  public static void logRequest(
      HttpServletRequest req, String type, Long contentSize,
      String launchTicket, String clientCookie, String logEntry)
  {
    BackendUtil.storeRequest(LifeCycle.getSessionFactory(req),
        new Request(new Date(), type, contentSize,
        ServletUtil.toHex(ServletUtil.sha1("stick " + req.getRemoteAddr()), APIServlet.IPHASH_SZ),
        BackendUtil.getGeoInfo(req), launchTicket, clientCookie, logEntry));
  }

  /* TODO: If we ever create a set of libraries common to the client and the server, deduplicate the
  following with ServerTimeLogFormatter on the client (but beware that we don't say "UTC" explicitly
  in the format string here). */
  private static final ThreadLocal<DateFormat> SERVER_DATE_FORMAT =
    new ThreadLocal<DateFormat>() {
      @Override
      public DateFormat initialValue() {
        DateFormat ret = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ret.setTimeZone(TimeZone.getTimeZone("GMT"));
        return ret;
      }
    };

  /** The returned DateFormat must only be used in the current thread. */
  public static DateFormat getServerDateFormat() {
    return SERVER_DATE_FORMAT.get();
  }

  public static void main(String args[]) {
    System.out.println(humanReadableInterval(0));
    System.out.println(humanReadableInterval(1));
    System.out.println(humanReadableInterval(59));
    System.out.println(humanReadableInterval(60));
    System.out.println(humanReadableInterval(61));
    System.out.println(humanReadableInterval(62));
    System.out.println(humanReadableInterval(3598));
    System.out.println(humanReadableInterval(3600));
    System.out.println(humanReadableInterval(3600 * 2 + 60 * 3 + 4));
  }
}
