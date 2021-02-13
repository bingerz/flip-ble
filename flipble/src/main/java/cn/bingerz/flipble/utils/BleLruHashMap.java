package cn.bingerz.flipble.utils;


import java.util.concurrent.ConcurrentHashMap;

/**
 * @author hanson
 */
public class BleLruHashMap<K, V> extends ConcurrentHashMap<K, V> {

    public BleLruHashMap(int saveSize) {
        super((int) Math.ceil(saveSize / 0.75) + 1, 0.75f);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Entry<K, V> entry : entrySet()) {
            sb.append(String.format("%s:%s ", entry.getKey(), entry.getValue()));
        }
        return sb.toString();
    }

}
