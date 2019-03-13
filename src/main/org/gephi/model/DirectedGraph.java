package org.gephi.model;

import java.util.Set;

public interface DirectedGraph extends Graph {
    int getInDegree(Node node);

    int getOutDegree(Node node);

    Set<Node> getSuccessors(Node node);

    Set<Node> getPredecessors(Node node);
}
