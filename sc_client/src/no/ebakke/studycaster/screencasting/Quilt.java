package no.ebakke.studycaster.screencasting;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/** Collection that assigns values to areas on a plane. Not thread-safe. */
public final class Quilt<V> {
  // TODO: See if there's a better way to do this.
  // To avoid overflow errors, don't go all the way to the boundaries.
  private static final int MAXVAL = Integer.MAX_VALUE / 8;
  private static final int MINVAL = Integer.MIN_VALUE / 8;
  // Non-overlapping list of rectangles, each associated with a value of type V.
  private List<Patch<V>> patches = new ArrayList<Patch<V>>();

  public Quilt(V background) {
    addPatch(newRectangle(MINVAL, MINVAL, MAXVAL, MAXVAL), background);
  }

  /** Create a new rectangle using end coordinates. */
  private static Rectangle newRectangle(int x1, int y1, int x2, int y2) {
    return new Rectangle(x1, y1, x2 - x1, y2 - y1);
  }

  private static <V> void addIntersection(List<Patch<V>> result, Patch<V> original,
      int x1, int y1, int x2, int y2)
  {
    Rectangle rect = newRectangle(x1, y1, x2, y2).intersection(original.rect);
    if (!rect.isEmpty())
      result.add(new Patch<V>(rect, original.value));
  }

  private static <V> void subtractPatch(Patch<V> original, Patch<V> subtractMe, List<Patch<V>> res)
  {
    final Rectangle sM = subtractMe.rect;
    /* To get original minus subtractMe, intersect original with the entire plane minus subtractMe.
    The entire plane minus subtractMe can be represented by four rectangles, one on each of the four
    sides. To encourage longer runs, make the horizontal sides (rectangleAbove/rectangleBelow in the
    pseudo-formula below) the longer ones.

        original - subtractMe
      = original * (1 - subtractMe)
      = original * (rectangleAbove + rectangleBelow + rectangleLeft + rectangleRight)
    */
    addIntersection(res, original,          MINVAL,           MINVAL, MAXVAL,                sM.y);
    addIntersection(res, original,          MINVAL, sM.y + sM.height, MAXVAL,              MAXVAL);
    addIntersection(res, original,          MINVAL,             sM.y,   sM.x,    sM.y + sM.height);
    addIntersection(res, original, sM.x + sM.width,             sM.y, MAXVAL,    sM.y + sM.height);
  }

  public void addPatch(Rectangle rect, V value) {
    if (rect.isEmpty())
      return;
    Patch<V> newPatch = new Patch<V>(new Rectangle(rect), value);
    List<Patch<V>> oldPatches = patches;
    patches = new ArrayList<Patch<V>>(oldPatches.size() + 5);
    // Subtract from all existing patches first to avoid overlapping.
    for (Patch<V> oldPatch : oldPatches)
      subtractPatch(oldPatch, newPatch, patches);
    patches.add(newPatch);
  }

  /** Return the value and run length associated with the given coordinate. The run length is at
  least 1. */
  public ValueRun<V> getPatchRun(int x, int y) {
    for (Patch<V> patch : patches) {
      if (patch.rect.contains(x, y))
        return new ValueRun<V>(patch.value, patch.rect.x + patch.rect.width - x);
    }
    throw new IndexOutOfBoundsException();
  }

  public static final class ValueRun<V> {
    private V   value;
    private int runLength;

    public ValueRun(V value, int runLength) {
      this.value     = value;
      this.runLength = runLength;
    }

    public V getValue() {
      return value;
    }

    public int getRunLength() {
      return runLength;
    }
  }

  /* Intented to be immutable, but don't bother with encapsulation and defensive copying since this
  is a small private class and since getPatchRun() should be fast. */
  private static final class Patch<V> {
    Rectangle rect;
    V         value;

    Patch(Rectangle rect, V value) {
      this.rect  = rect;
      this.value = value;
    }
  }
}
