package edu.stanford.cs276.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


public class Dictionary implements Serializable {
    private static final long serialVersionUID = 1L;

    private int termCount;
    private HashMap<String, Double> map;

    public int termCount() {
        return termCount;
    }

    public Dictionary() {
        termCount = 0;
        map = new HashMap<String, Double>();
    }
    public Map<String, Double> getMap() {
        return map;
    }

    public void add(String term) {

        termCount++;
        if (map.containsKey(term)) {
            map.put(term, map.get(term) + 1.0);
        } else {
            map.put(term, 1.0);
        }
    }

    public double count(String term) {

        if (map.containsKey(term)) {
            return map.get(term);
        } else {
            return 0;
        }
    }

    public boolean containsTerm(String term) {
        return map.containsKey(term);
    }
}
