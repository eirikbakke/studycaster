package no.ebakke.studycaster.screencasting;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/** Not thread-safe. */
public final class Quilt {
  // Non-overlapping list of rectangles, each either positive or negative.
  private List<Patch> patches = new ArrayList<Patch>();

  public Quilt() {
  }

  public Quilt(Rectangle rect) {
    this();
    addPatch(rect, true);
  }

  private static void addIntersection(List<Patch> result, Patch original,
      int x1, int y1, int x2, int y2)
  {
    Rectangle rect = new Rectangle(x1, y1, x2-x1, y2-y1).intersection(original.rect);
    if (!rect.isEmpty())
      result.add(new Patch(rect, original.positive));
  }

  /** Return an integer larger than any coordinate touched by the specified rectangle. */
  private static int getMaxVal(Rectangle rect) {
    return Math.max(
        Math.abs(rect.x) + Math.abs(rect.width ),
        Math.abs(rect.y) + Math.abs(rect.height)) + 2;
  }

  private static void subtractPatch(Patch original, Patch subtractMe, List<Patch> res) {
    /* Could use Integer.MAX_VALUE/MIN_VALUE, but would then have to think hard about overflow. */
    final int MAXVAL = Math.max(getMaxVal(original.rect), getMaxVal(subtractMe.rect));
    final int MINVAL = -MAXVAL;

    final Rectangle sM = subtractMe.rect;
    // original - subtractMe = original * (1 - subtractMe)
    addIntersection(res, original,          MINVAL,           MINVAL, MAXVAL,                sM.y);
    addIntersection(res, original,          MINVAL, sM.y + sM.height, MAXVAL,              MAXVAL);
    addIntersection(res, original,          MINVAL,             sM.y,   sM.x,    sM.y + sM.height);
    addIntersection(res, original, sM.x + sM.width,             sM.y, MAXVAL,    sM.y + sM.height);
  }

  // TODO: Instead of positive/negative, allow an arbitrary value.
  public void addPatch(Rectangle rect, boolean positive) {
    Patch newPatch = new Patch(rect, positive);
    List<Patch> oldPatches = patches;
    patches = new ArrayList<Patch>(oldPatches.size() + 5);
    // Subtract from all existing patches first to avoid overlapping.
    for (Patch oldPatch : oldPatches)
      subtractPatch(oldPatch, newPatch, patches);
    patches.add(newPatch);
  }

  public boolean contains(int x, int y) {
    for (Patch patch : patches) {
      if (patch.rect.contains(x, y))
        return patch.positive;
    }
    return false;
  }

  /** Returns positive or negative non-zero integer. */
  public int getHorizontalRunLength(int x, int y) {
    for (Patch patch : patches) {
      if (patch.rect.contains(x, y)) {
        return (patch.rect.x + patch.rect.width - x) *
            (patch.positive ? 1 : -1);
      }
    }
    // TODO: Avoid using these extremes.
    // Add 1 to make sure the value can be negated without overflow.
    return Integer.MIN_VALUE + 1;
  }

  private static final class Patch {
    Rectangle rect;
    boolean   positive;

    public Patch(Rectangle rect, boolean positive) {
      this.rect     = rect;
      this.positive = positive;
    }
  }
}
