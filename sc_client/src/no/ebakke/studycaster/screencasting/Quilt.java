package no.ebakke.studycaster.screencasting;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Quilt {
  private List<Patch> patches = new ArrayList<Patch>();

  public Quilt() {
  }

  public Quilt(Rectangle rect) {
    addPatch(rect, true);
  }

  public void addPatch(Rectangle rect, boolean positive) {
    // Optimization: Removing completely covered patches from list.
    for (Iterator<Patch> it = patches.iterator(); it.hasNext();) {
      Patch patch = it.next();
      if (rect.contains(patch.rect))
        it.remove();
    }
    patches.add(new Patch(new Rectangle(rect), positive));
  }

  public boolean contains(int x, int y) {
    boolean ret = false;
    for (Patch patch : patches) {
      if (patch.rect.contains(x, y))
        ret = patch.positive;
    }
    return ret;
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
