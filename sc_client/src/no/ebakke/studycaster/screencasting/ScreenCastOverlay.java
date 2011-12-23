package no.ebakke.studycaster.screencasting;

import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Date;
import no.ebakke.studycaster.backend.ServerTimeLogFormatter;
import no.ebakke.studycaster.backend.TimeSource;
import no.ebakke.studycaster.screencasting.desktop.DesktopMeta;
import no.ebakke.studycaster.screencasting.desktop.DesktopMetaFactory;
import no.ebakke.studycaster.screencasting.desktop.Win32DesktopLibrary;
import no.ebakke.studycaster.screencasting.desktop.WindowInfo;
import no.ebakke.studycaster.ui.UIUtil;
import no.ebakke.studycaster.util.ImageDebugFrame;

public class ScreenCastOverlay {
  private static final String POINTER_IMAGE_FILE     = "pointer_shine_weaker.png";
  private static final String LOGO_IMAGE_FILE        = "icon22.png";
  private static final Point  POINTER_HOTSPOT        = new Point(41, 40);
  private static final String FONT_MONO_FILE         = "LiberationMono-Bold.ttf";
  private static final String FONT_SANS_BOLD_FILE    = "LiberationSans-Bold.ttf";
  private static final String FONT_SANS_REGULAR_FILE = "LiberationSans-Regular.ttf";
  private static final int    STATUS_MARGIN = 3;
  private static final float  FONT_SIZE_LARGE = 22.0f;
  private static final float  FONT_SIZE_SMALL = 15.0f;
  private final Image pointerImage, iconImage;
  private final Font fontMono, fontSansBold, fontSansRegular;
  private final int fontCapHeight;
  private final Dimension outputDimension, iconDimensions;
  private final BufferedImage desktopMetaOverlayImage;

  private static double getStringHeight(Graphics2D g, String s, Font f) {
    // TODO: getBounds2D() is not guaranteed to be smallest bounding box.
    return f.createGlyphVector(g.getFontRenderContext(), s).getOutline().getBounds2D().getHeight();
  }

  public ScreenCastOverlay(Dimension inputDimension) throws IOException {
    iconImage       = UIUtil.loadImage(LOGO_IMAGE_FILE, true);
    pointerImage    = UIUtil.loadImage(POINTER_IMAGE_FILE, true);
    fontMono        = UIUtil.createFont(FONT_MONO_FILE, FONT_SIZE_LARGE);
    fontSansBold    = UIUtil.createFont(FONT_SANS_BOLD_FILE, FONT_SIZE_LARGE);
    fontSansRegular = UIUtil.createFont(FONT_SANS_REGULAR_FILE, FONT_SIZE_SMALL);

    Graphics2D g = new ScreenCastImage(inputDimension).createGraphics();
    fontCapHeight = (int) Math.round(
        Math.max(getStringHeight(g, "H", fontMono), getStringHeight(g, "H", fontSansBold)));
    g.dispose();

    int iconWidth  = iconImage.getWidth(null);
    int iconHeight = iconImage.getWidth(null);
    if (iconWidth < 0 || iconHeight < 0)
      throw new IOException("Failed to get icon size");
    iconDimensions = new Dimension(iconWidth, iconHeight);

    outputDimension =
        new Dimension(inputDimension.width, inputDimension.height + getStatusAreaHeight());

    desktopMetaOverlayImage = new BufferedImage(outputDimension.width, outputDimension.height,
        BufferedImage.TYPE_4BYTE_ABGR);
  }

  @SuppressWarnings("FinalMethod")
  public final int getStatusAreaHeight() {
    return fontCapHeight + 2 * STATUS_MARGIN;
  }

  private void drawImage(Graphics2D g, Image img, double x, double y) {
    if (!g.drawImage(img, (int) Math.round(x), (int) Math.round(y), null))
      throw new AssertionError("Expected immediate image conversion");
  }

  /** For align &lt; 0, left-aligned, for align &gt; 0, right-aligned, for align == 0, centered. */
  private Rectangle2D drawString(
      Graphics2D g, String str, Color color, Font font, int align, double x, double y)
  {
    final Rectangle2D stringBounds = g.getFontMetrics(font).getStringBounds(str, g);
    final double width = (float) stringBounds.getWidth();
    final double alignedX;
    if        (align < 0) {
      alignedX = x;
    } else if (align > 0) {
      alignedX = x - width;
    } else {
      alignedX = x - width / 2.0f;
    }
    g.setFont(font);
    g.setColor(color);
    g.drawString(str, Math.round(alignedX), Math.round(y));
    return stringBounds;
  }

  /** Argument pageName may be null. */
  public void drawStatus(Graphics2D g, long currentMetaTime, long timeSinceStart,
      boolean statUserInput, boolean statFrameIndicator, boolean statMetaIndicator,
      String pageName)
  {
    final String formattedStatus =
        (statUserInput      ? "U" : " ") +
        (statFrameIndicator ? "F" : " ") +
        (statMetaIndicator  ? "M" : " ") + " / " +
        ServerTimeLogFormatter.getServerDateFormat().format(new Date(currentMetaTime)) +
        String.format(" / %6.1fs", timeSinceStart / 1000.0) +
        ((pageName == null) ? "" : (" / " + pageName));

    g.setColor(Color.BLACK);
    g.fillRect(0, outputDimension.height - getStatusAreaHeight(),
        outputDimension.width, Integer.MAX_VALUE);
    final int baseLine = outputDimension.height - STATUS_MARGIN;
    drawString(g, formattedStatus, Color.WHITE, fontMono, -1, STATUS_MARGIN, baseLine);
    Rectangle2D titleBounds = drawString(g, "StudyCaster", Color.WHITE, fontSansBold, 1,
        outputDimension.width - STATUS_MARGIN, baseLine);
    drawImage(g, iconImage,
        outputDimension.width - (titleBounds.getWidth() + iconDimensions.width + 2 * STATUS_MARGIN),
        baseLine - fontCapHeight / 2 - iconDimensions.height / 2);
  }

  public void drawDesktopMeta(Graphics2D g, DesktopMeta meta) {
    final Font FONT = fontSansRegular;
    final int MARGIN_Y = 3, MARGIN_X = 6;
    final FontMetrics fontMetrics = g.getFontMetrics(FONT);
    final int ASCENT       = fontMetrics.getAscent();
    final int TITLE_HEIGHT = MARGIN_Y * 2 + ASCENT + fontMetrics.getDescent();

    /* Prepare the overlay graphics in a separate buffer before copying it to the main buffer, such
    that the clearRect() function can be used to erase covered portions of lower Z-order windows.
    The buffer is recycled between invocations, so erase it first. */
    final Graphics2D og = desktopMetaOverlayImage.createGraphics();
    og.setBackground(new Color(0, 0, 0, 0));
    og.clearRect(0, 0, desktopMetaOverlayImage.getWidth(), desktopMetaOverlayImage.getHeight());
    for (WindowInfo windowInfo : meta.getWindowList()) {
      final Rectangle rect = windowInfo.getBounds();
      // Erase any overlapping portions of previously drawn window overlay graphics (see above).
      og.clearRect(rect.x, rect.y, rect.width, rect.height);
      // Paint title text and its lightened background, clipped to the title bar area only.
      og.setColor(new Color(255, 255, 255, 196));
      og.fillRect(rect.x, rect.y, rect.width, TITLE_HEIGHT);
      final Shape oldClip = og.getClip();
      og.setClip(rect.x, rect.y, rect.width - MARGIN_X, TITLE_HEIGHT);
      drawString(og,
          (windowInfo.getTitle().length() == 0 ? "" : windowInfo.getTitle() + " ") +
          "(PID " + windowInfo.getPID() + ")", Color.BLACK,
          FONT, -1, MARGIN_X + rect.getX(), MARGIN_Y + rect.getY() + ASCENT);
      og.setClip(oldClip);
      // Paint window boundary and title bar border.
      og.setColor(new Color(0, 0, 0, 196));
      og.setStroke(new BasicStroke(windowInfo.isForeground() ? 3.0f : 1.0f));
      og.drawRect(rect.x, rect.y, rect.width, rect.height);
      og.drawLine(rect.x, rect.y + TITLE_HEIGHT, rect.x + rect.width, rect.y + TITLE_HEIGHT);
    }
    // Finally, draw the overlay onto the main image buffer.
    if (!g.drawImage(desktopMetaOverlayImage, 0, 0, null))
      throw new AssertionError("Expected drawImage() to complete immediately");
    og.dispose();
  }

  public void drawWarning(Graphics2D g, String str) {
    drawString(g, str, Color.RED, fontSansBold, 0,
        outputDimension.width / 2, fontCapHeight + STATUS_MARGIN);
  }

  public void drawPointer(Graphics2D g, int x, int y) {
    drawImage(g, pointerImage, x - POINTER_HOTSPOT.x, y - POINTER_HOTSPOT.y);
  }

  public static void main(String args[]) throws AWTException, IOException {
    Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    BufferedImage capture = new Robot().createScreenCapture(screenRect);
    ScreenCastOverlay overlay =
        new ScreenCastOverlay(new Dimension(screenRect.width, screenRect.height));
    ScreenCastImage target = new ScreenCastImage(CodecUtil.makeEven(
        new Dimension(screenRect.width, screenRect.height + overlay.getStatusAreaHeight())));
    CodecUtil.copyImage(capture, target);
    Graphics2D g = target.createGraphics();
    overlay.drawStatus(g, 0, 0, true, true, true, "pageName");
    overlay.drawDesktopMeta(g,
        new DesktopMetaFactory(Win32DesktopLibrary.create(), new TimeSource()).createMeta());
    overlay.drawWarning(g, "This is a test warning.");
    g.dispose();
    ImageDebugFrame.showImage(target);
  }
}
