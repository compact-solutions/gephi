package org.gephi.model.impl;

import org.gephi.model.DirectedGraph;
import org.gephi.model.Edge;
import org.gephi.model.Node;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class DirectedGraphImpl extends GraphImpl implements DirectedGraph {

    private class DirectedGraphNodeProperties {
        private int inDegree = 0;
        private int outDegree = 0;
        private Set<Node> successors = new HashSet<>();
        private Set<Node> predecessors = new HashSet<>();

        private void finalizeProperty() {
            successors = Collections.unmodifiableSet(successors);
            predecessors = Collections.unmodifiableSet(predecessors);
        }
    }

    protected HashMap<Node, DirectedGraphNodeProperties> nodeProperties = new HashMap<>();

    public DirectedGraphImpl(Node[] nodes, Edge[] edges) {
        super(nodes, edges);
        setDirected(true);
        buildNodeProperties();
    }

    private void buildNodeProperties() {
        for (Node node : getNodes()) {
            nodeProperties.putIfAbsent(node, new DirectedGraphNodeProperties());
        }
        for (Edge edge : getEdges()) {
            nodeProperties.get(edge.getSource()).outDegree += 1;
            nodeProperties.get(edge.getSource()).successors.add(edge.getTarget());
            nodeProperties.get(edge.getTarget()).inDegree += 1;
            nodeProperties.get(edge.getTarget()).predecessors.add(edge.getSource());
        }
        nodeProperties.forEach((node, properties) -> properties.finalizeProperty());
    }

    @Override
    public int getDegree(Node node) {
        return getInDegree(node) + getOutDegree(node);
    }

    @Override
    public int getInDegree(Node node) {
        return getNodeProperties(node).inDegree;
    }

    @Override
    public int getOutDegree(Node node) {
        return getNodeProperties(node).outDegree;
    }

    @Override
    public Set<Node> getSuccessors(Node node) {
        return getNodeProperties(node).successors;
    }

    @Override
    public Set<Node> getPredecessors(Node node) {
        return getNodeProperties(node).predecessors;
    }

    protected DirectedGraphNodeProperties getNodeProperties(Node node) {
        checkNodeInGraph(node);
        return nodeProperties.get(node);
    }

    protected void checkNodeInGraph(Node node) {
        if (node == null || !nodeProperties.containsKey(node)) {
            throw new RuntimeException("Node not in graph.");
        }
    }
}
