package org.gephi.model.impl;

import org.gephi.model.DataContainer;

import java.util.HashMap;

public class DataContainerImpl implements DataContainer {

    private final HashMap<String, Object> data = new HashMap<>();

    @Override
    public String[] getDataKeys() {
        return data.keySet().toArray(new String[0]);
    }

    @Override
    public void setData(String key, Object value) {
        data.put(key, value);
    }

    @Override
    public <D> D getData(String key) {
        return (D) data.get(key);
    }

    @Override
    public boolean hasData(String key) {
        return data.containsKey(key);
    }
}
