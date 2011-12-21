package no.ebakke.studycaster.screencasting;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import no.ebakke.studycaster.screencasting.desktop.DesktopMeta;
import no.ebakke.studycaster.screencasting.desktop.WindowInfo;
import no.ebakke.studycaster.ui.UIUtil;
import no.ebakke.studycaster.util.ImageDebugFrame;

public class ScreenCastOverlay {
  private static final String POINTER_IMAGE_FILE     = "pointer_shine_weaker.png";
  private static final String FILEPATH               = "icon22.png";
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

  private static double getStringHeight(Graphics2D g, String s, Font f) {
    // TODO: getBounds2D() is not guaranteed to be smallest bounding box.
    return f.createGlyphVector(g.getFontRenderContext(), s).getOutline().getBounds2D().getHeight();
  }

  public ScreenCastOverlay(Dimension inputDimension) throws IOException {
    iconImage       = UIUtil.loadImage(FILEPATH, true);
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

  /** Argument extendedMeta may be null. */
  public void drawStatus(Graphics2D g, String formattedStatus) {
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
    final Font FONT     = fontSansRegular;
    final int  MARGIN_Y = 3, MARGIN_X = 6;
    final int  OFFSET_Y = g.getFontMetrics(FONT).getAscent();
    for (WindowInfo windowInfo : meta.getWindowList()) {
      Rectangle rect = windowInfo.getBounds();
      g.setColor(new Color(255, 255, 255, 128));
      g.fill(new Rectangle(rect.x, rect.y, rect.width, MARGIN_Y * 2 + OFFSET_Y));
      drawString(g, windowInfo.getTitle(), Color.BLACK, FONT, -1,
          MARGIN_X + rect.getX(), MARGIN_Y + rect.getY() + OFFSET_Y);
      g.setColor(Color.BLACK);
      g.draw(rect);
    }
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
    overlay.drawStatus(g, "UFM / 0000-00-00 00:00:00.000 /      0");
    overlay.drawWarning(g, "This is a test warning.");
    g.dispose();
    ImageDebugFrame.showImage(target);
  }
}
