package no.ebakke.studycaster.reporting;

// TODO: Find a better name.

import java.util.List;
import java.util.Map;
import java.util.Set;
import no.ebakke.studycaster.util.ColUtil;
import no.ebakke.studycaster.util.OrderedBidiMap;
import org.apache.commons.lang3.tuple.Pair;

public class Linker<V> {
  OrderedBidiMap<Pair<String,String>,Node<V>> contentMap =
      new OrderedBidiMap<Pair<String,String>,Node<V>>();

  public Linker() {
  }

  public void addLink(Map<Pair<String,Boolean>,String> linkMap, V val) {
    Node<V> node = new Node<V>();
    for (Map.Entry<Pair<String,Boolean>,String> linkEntry : linkMap.entrySet()) {
      if (linkEntry.getValue() == null)
        continue;
      if (linkEntry.getKey().getRight()) {
        Pair<String,String> p = Pair.of(linkEntry.getKey().getLeft(), linkEntry.getValue());
        Node<V> existingNode = contentMap.get(p);
        if (existingNode != null) {
          node.unionWith(existingNode);
          for (Pair<String,String> oldPair : contentMap.getKeysForValue(existingNode))
            contentMap.put(oldPair, node);
        }
        contentMap.put(p, node);
      }
      node.addLink(linkEntry.getKey().getLeft(), linkEntry.getValue());
    }
    node.addParticipant(val);
  }

  /*
  public List<List<V>> getContent() {
    List<List<V>> ret = ColUtil.newList();
    for (Node node : content)
      ret.add(ColUtil.newList(node.participants));
    return ret;
  }*/

  public List<Node<V>> getContent() {
    return ColUtil.newList(ColUtil.newOrderedSet(contentMap.values()));
  }

  /** Uses referential equality. */
  public static class Node<V> {
    private Set<V>                  participants = ColUtil.newOrderedSet();
    private Map<String,Set<String>> links        = ColUtil.newOrderedMap();

    public Node() {
    }

    public void unionWith(Node<V> other) {
      participants.addAll(other.participants);
      for (Map.Entry<String,Set<String>> entry : other.links.entrySet()) {
        for (String value : entry.getValue()) {
          addLink(entry.getKey(), value);
        }
      }
    }

    public void addParticipant(V participant) {
      participants.add(participant);
    }

    public void addLink(String key, String val) {
      Set<String> valueSet = links.get(key);
      if (valueSet == null) {
        valueSet = ColUtil.newOrderedSet();
        links.put(key, valueSet);
      }
      valueSet.add(val);
    }

    @Override
    public String toString() {
      StringBuilder ret = new StringBuilder();
      ret.append("Node:\n");
      for (Map.Entry<String,Set<String>> entry : links.entrySet())
        ret.append("  " + entry.getKey() + ": " + entry.getValue() + "\n");
      return ret.toString();
    }
  }
}
