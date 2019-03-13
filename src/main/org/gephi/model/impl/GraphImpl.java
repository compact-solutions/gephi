package org.gephi.model.impl;

import org.gephi.model.DataContainer;
import org.gephi.model.Edge;
import org.gephi.model.Graph;
import org.gephi.model.Node;

public abstract class GraphImpl implements Graph {

    protected DataContainer dataContainer = new DataContainerImpl();

    private Node[] nodes;
    private Edge[] edges;

    private boolean isDirected;

    protected GraphImpl(Node[] nodes, Edge[] edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    @Override
    public Node[] getNodes() {
        return nodes;
    }

    @Override
    public Edge[] getEdges() {
        return edges;
    }

    @Override
    public boolean isDirected() {
        return isDirected;
    }

    protected void setDirected(boolean directed) {
        isDirected = directed;
    }

    @Override
    public int getNodeCount() {
        return nodes == null ? 0 : nodes.length;
    }

    @Override
    public int getEdgeCount() {
        return edges == null ? 0 : edges.length;
    }

    @Override
    public String[] getDataKeys() {
        return dataContainer.getDataKeys();
    }

    @Override
    public void setData(String key, Object value) {
        dataContainer.setData(key, value);
    }

    @Override
    public <D> D getData(String key) {
        return dataContainer.getData(key);
    }

    @Override
    public boolean hasData(String key) {
        return dataContainer.hasData(key);
    }
}
