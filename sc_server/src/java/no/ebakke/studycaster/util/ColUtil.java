package no.ebakke.studycaster.util;

import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

// TODO: Find more places where these static initializers can be used.
public final class ColUtil {
  private ColUtil() {}

  public static <E> List<E> newList() {
    return new ArrayList<E>();
  }

  public static <E> List<E> newList(Collection<? extends E> c) {
    return new ArrayList<E>(c);
  }

  public static <E> Set<E> newOrderedSet() {
    return new LinkedHashSet<E>();
  }

  public static <E> Set<E> newOrderedSet(Collection<? extends E> c) {
    return new LinkedHashSet<E>(c);
  }

  public static <K,V> Map<K,V> newOrderedMap() {
    return new LinkedHashMap<K,V>();
  }

  public static <K,V> Map<K,V> newOrderedMap(Map<? extends K,? extends V> m) {
    return new LinkedHashMap<K,V>(m);
  }

  public static <K,V> void putChecked(Map<K,V> map, K key, V value) {
    if (map.containsKey(key))
      throw new BufferOverflowException();
    map.put(key, value);
  }

  public static <K,V> V getChecked(Map<K,V> map, K key) {
    if (!map.containsKey(key))
      throw new NoSuchElementException("Could not find key " + key.toString());
    return map.get(key);
  }
}