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
import no.ebakke.studycaster.ui.ResourceUtil;
import no.ebakke.studycaster.util.ImageDebugFrame;

public class ScreenCastOverlay {
  private static final String POINTER_IMAGE_FILE = "pointer_shine_weaker.png";
  private static final String FILEPATH           = "icon32.png";
  private static final Point  POINTER_HOTSPOT    = new Point(41, 40);
  private static final String FONT_MONO_FILE     = "LiberationMono-Bold.ttf";
  private static final String FONT_SANS_FILE     = "LiberationSans-Bold.ttf";
  private static final int    STATUS_MARGIN = 6;
  private static final float  FONT_SIZE = 28.0f;
  private final Image pointerImage, iconImage;
  private final Font fontMono, fontSans;
  private final int fontCapHeight;
  private final Dimension outputDimension, iconDimensions;

  private static double getStringHeight(Graphics2D g, String s, Font f) {
    // TODO: getBounds2D() is not guaranteed to be smallest bounding box.
    return f.createGlyphVector(g.getFontRenderContext(), s).getOutline().getBounds2D().getHeight();
  }

  public ScreenCastOverlay(Dimension inputDimension) throws IOException {
    iconImage    = ResourceUtil.loadImage(FILEPATH, true);
    pointerImage = ResourceUtil.loadImage(POINTER_IMAGE_FILE, true);
    fontMono     = ResourceUtil.createFont(FONT_MONO_FILE, FONT_SIZE);
    fontSans     = ResourceUtil.createFont(FONT_SANS_FILE, FONT_SIZE);

    Graphics2D g = new ScreenCastImage(inputDimension).createGraphics();
    fontCapHeight = (int) Math.round(
        Math.max(getStringHeight(g, "H", fontMono), getStringHeight(g, "H", fontSans)));
    g.dispose();

    int iconWidth  = iconImage.getWidth(null);
    int iconHeight = iconImage.getWidth(null);
    if (iconWidth < 0 || iconHeight < 0)
      throw new IOException("Failed to get icon size");
    iconDimensions = new Dimension(iconWidth, iconHeight);

    outputDimension =
        new Dimension(inputDimension.width, inputDimension.height + getStatusAreaHeight());
  }

  public final int getStatusAreaHeight() {
    return fontCapHeight + 2 * STATUS_MARGIN;
  }

  private void drawImage(Graphics2D g, Image img, double x, double y) {
    if (!g.drawImage(img, (int) Math.round(x), (int) Math.round(y), null))
      throw new AssertionError("Expected immediate image conversion");
  }

  /** For align < 0, left-aligned, for align > 0, right-aligned, for align == 0, centered. */
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

  public void drawStatus(Graphics2D g, String formattedTimestamp) {
    g.setColor(Color.BLACK);
    g.fillRect(0, outputDimension.height - getStatusAreaHeight(),
        outputDimension.width, getStatusAreaHeight());
    final int baseLine = outputDimension.height - STATUS_MARGIN;
    drawString(g, formattedTimestamp, Color.WHITE, fontMono, -1, STATUS_MARGIN, baseLine);
    Rectangle2D titleBounds = drawString(g, "StudyCaster", Color.WHITE, fontSans, 1,
        outputDimension.width - STATUS_MARGIN, baseLine);
    drawImage(g, iconImage,
        outputDimension.width - (titleBounds.getWidth() + iconDimensions.width + 2 * STATUS_MARGIN),
        baseLine - fontCapHeight / 2 - iconDimensions.height / 2);
  }

  public void drawWarning(Graphics2D g, String str) {
    drawString(g, str, Color.RED, fontSans, 0,
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
    overlay.drawStatus(g, "FM / 0000-00-00 00:00:00.000 /      0");
    overlay.drawWarning(g, "This is a test warning.");
    g.dispose();
    ImageDebugFrame.showImage(target);
  }
}
