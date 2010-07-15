package no.ebakke.studycaster.util;

public class Pair<A, B> {
  private A a;
  private B b;

  public Pair(A a, B b) {
    this.a = a;
    this.b = b;
  }

  public A getFirst() {
    return a;
  }

  public B getLast() {
    return b;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof Pair<?, ?>))
      return false;

    Pair<?, ?> o = (Pair<?, ?>) obj;
    if (((a == null) != (o.a == null)) || ((a == null) != (o.a == null)))
      return false;

    return (o.a == null || o.a.equals(a))
        && (o.b == null || o.b.equals(b));
  }

  @Override
  public int hashCode() {
    return ((a == null) ? 0 : a.hashCode()) ^ ((b == null) ? 0 : b.hashCode());
  }

  @Override
  public String toString() {
    return "(" + a.toString() + "," + b.toString() + ")";
  }
}
