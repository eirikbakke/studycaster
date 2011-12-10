package no.ebakke.studycaster.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class OrderedBidiMap<K,V> {
  private Map<K,V> contents = ColUtil.newOrderedMap();
  private Map<V,Set<K>> reverse  = ColUtil.newOrderedMap();

  public void put(K key, V val) {
    remove(key);
    contents.put(key, val);
    Set<K> reverseKeys = reverse.get(val);
    if (reverseKeys == null) {
      reverseKeys = ColUtil.newOrderedSet();
      reverse.put(val, reverseKeys);
    }
    reverseKeys.add(key);
  }

  public void remove(K key) {
    if (!contents.containsKey(key))
      return;
    V val = contents.get(key);
    reverse.get(val).remove(key);
    contents.remove(key);
  }

  public void removeValue(V val) {
    for (K key : reverse.get(val))
      remove(key);
  }

  public Set<K> getKeysForValue(V val) {
    return ColUtil.newOrderedSet(reverse.get(val));
  }

  public V get(K key) {
    return contents.get(key);
  }

  public Collection<V> values() {
    // TODO: Consider defensive copying.
    return contents.values();
  }
}
