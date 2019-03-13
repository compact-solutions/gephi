package org.gephi.preview;

import org.gephi.model.Graph;

public interface ItemBuilder {

    String NODE_BUILDER = Item.NODE;
    String NODE_LABEL_BUILDER = Item.NODE_LABEL;
    String EDGE_BUILDER = Item.EDGE;
    String EDGE_LABEL_BUILDER = Item.EDGE_LABEL;


    Item[] getItems(Graph graph);

    String getType();
}
