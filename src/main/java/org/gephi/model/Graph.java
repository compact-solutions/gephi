package org.gephi.model;

public interface Graph extends DataContainer {

    Node[] getNodes();

    Edge[] getEdges();

    boolean isDirected();

    int getDegree(Node node);

    int getNodeCount();

    int getEdgeCount();
}
