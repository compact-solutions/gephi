package org.gephi.model;

public interface DataContainer {

    String[] getDataKeys();

    void setData(String key, Object value);

    <D> D getData(String key);

    boolean hasData(String key);

}
