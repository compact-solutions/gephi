package org.gephi.forceAtlas2;


import org.gephi.model.Graph;
import org.gephi.model.Node;

public class RandomNodePositionInitializer implements NodePositionInitializer {
    @Override
    public void initializeNodePositions(Graph graph) {
        //All at 0.0, init some random positions
        for (Node node : graph.getNodes()) {
            node.setX((float) ((0.01 + Math.random()) * 1000) - 500);
            node.setY((float) ((0.01 + Math.random()) * 1000) - 500);
        }
    }
}
