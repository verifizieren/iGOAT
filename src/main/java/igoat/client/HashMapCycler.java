package igoat.client;
import java.util.*;
import java.util.Map;

/**
 * Helper class to cycle through the player hashmap in a consistent order
 */
public class HashMapCycler<K, V> {
    private final LinkedHashMap<K, V> map;
    private K currentKey = null;

    public HashMapCycler(LinkedHashMap<K, V> map) {
        this.map = map;
    }

    public V nextValue() {
        List<K> keyList = new ArrayList<>(map.keySet());
        if (keyList.isEmpty()) {
            currentKey = null;
            return null;
        }

        int index = currentKey == null ? -1 : keyList.indexOf(currentKey);
        int nextIndex = (index + 1) % keyList.size();
        currentKey = keyList.get(nextIndex);
        return map.get(currentKey);
    }

    public V getCurrentValue() {
        return currentKey == null ? null : map.get(currentKey);
    }
}