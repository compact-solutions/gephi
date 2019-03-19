/*
        Copyright 2008-2011 Gephi
        Authors : Mathieu Jacomy <mathieu.jacomy@gmail.com>
        Website : http://www.gephi.org

        This file is part of Gephi.

        DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

        Copyright 2011 Gephi Consortium. All rights reserved.

        The contents of this file are subject to the terms of either the GNU
        General Public License Version 3 only ("GPL") or the Common
        Development and Distribution License("CDDL") (collectively, the
        "License"). You may not use this file except in compliance with the
        License. You can obtain a copy of the License at
        http://gephi.org/about/legal/license-notice/
        or /cddl-1.0.txt and /gpl-3.0.txt. See the License for the
        specific language governing permissions and limitations under the
        License.  When distributing the software, include this License Header
        Notice in each file and include the License files at
        /cddl-1.0.txt and /gpl-3.0.txt. If applicable, add the following below the
        License Header, with the fields enclosed by brackets [] replaced by
        your own identifying information:
        "Portions Copyrighted [year] [name of copyright owner]"

        If you wish your version of this file to be governed by only the CDDL
        or only the GPL Version 3, indicate your decision by adding
        "[Contributor] elects to include this software in this distribution
        under the [CDDL or GPL Version 3] license." If you do not indicate a
        single choice of license, a recipient has the option to distribute
        your version of this file under either the CDDL, the GPL Version 3 or
        to extend the choice of license to its licensees as provided above.
        However, if you add GPL Version 3 code and therefore, elected the GPL
        Version 3 license, then the option applies only if the new code is
        made subject to such option by the copyright holder.

        Contributor(s):

        Portions Copyrighted 2011 Gephi Consortium.
        */
package org.gephi.forceAtlas2;

import org.gephi.model.Edge;
import org.gephi.model.Graph;
import org.gephi.model.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * ForceAtlas 2 Layout, manages each step of the computations.
 *
 * @author Mathieu Jacomy
 */
public class ForceAtlas2 {

    private Graph graph;
    private double edgeWeightInfluence;
    private double jitterTolerance;
    private double scalingRatio;
    private double gravity;
    private double speed;
    private double speedEfficiency;
    private boolean outboundAttractionDistribution;
    private boolean adjustSizes;
    private boolean barnesHutOptimize;
    private double barnesHutTheta;
    private boolean linLogMode;
    private boolean strongGravityMode;
    private RepulsionForceProvider repulsionForceProvider;
    private ForceFactory.CustomForce customForce;
    private int threadCount;
    private int currentThreadCount;
    private Region rootRegion;
    double outboundAttCompensation = 1;
    private ExecutorService pool;

    private HashMap<Node, ForceAtlas2StrengthsLog> nodesProperties;
    private NodePositionInitializer nodePositionInitializer;

    public ForceAtlas2() {
        this.threadCount = Math.min(4, Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
    }

    public void initAlgo() {

        Optional.ofNullable(nodePositionInitializer)
                .orElse(new RandomNodePositionInitializer())
                .initializeNodePositions(graph);

        speed = 1.;
        speedEfficiency = 1.;

        Node[] nodes = graph.getNodes();

        // Initialise layout data
        for (Node n : nodes) {
            n.setMass(1 + graph.getDegree(n));
            n.setOldDx(0);
            n.setOldDy(0);
            n.setDx(0);
            n.setDy(0);
        }

        pool = Executors.newFixedThreadPool(threadCount);
        currentThreadCount = threadCount;
    }

    private double getEdgeWeight(Edge edge) {
        return edge.getWeight();
    }

    public void goAlgo() {
        Node[] nodes = graph.getNodes();
        Edge[] edges = graph.getEdges();

        // Initialise layout data
        for (Node n : nodes) {
            n.setMass(1 + graph.getDegree(n));
            n.setOldDx(n.getDx());
            n.setOldDy(n.getDy());
            n.setDx(0);
            n.setDy(0);
        }

        // If Barnes Hut active, initialize root region
        if (isBarnesHutOptimize()) {
            rootRegion = new Region(nodes);
            rootRegion.buildSubRegions();
        }

        // If outboundAttractionDistribution active, compensate.
        if (isOutboundAttractionDistribution()) {
            outboundAttCompensation = 0;
            for (Node n : nodes) {
                outboundAttCompensation += n.getMass();
            }
            outboundAttCompensation /= nodes.length;
        }

        // Repulsion (and gravity)
        // NB: Muti-threaded
        ForceFactory.RepulsionForce Repulsion = getRepulsionForceProvider() != null ?
                getRepulsionForceProvider().getRepulsionForce(isAdjustSizes(), getScalingRatio())
                : ForceFactory.getForceFactory().buildRepulsion(isAdjustSizes(), getScalingRatio());
        int taskCount =
                8 * currentThreadCount;  // The threadPool Executor Service will manage the fetching of tasks and threads.
        // We make more tasks than threads because some tasks may need more time to compute.
        ArrayList<Future> threads = new ArrayList();
        for (int t = taskCount; t > 0; t--) {
            int from = (int) Math.floor(nodes.length * (t - 1) / taskCount);
            int to = (int) Math.floor(nodes.length * t / taskCount);
            Future future = pool
                    .submit(new NodesThread(nodes, from, to, isBarnesHutOptimize(), getBarnesHutTheta(), getGravity(),
                            (isStrongGravityMode()) ?
                                    (ForceFactory.getForceFactory().getStrongGravity(getScalingRatio())) :
                                    (Repulsion), getScalingRatio(), rootRegion, Repulsion));
            threads.add(future);
        }
        for (Future future : threads) {
            try {
                future.get();
            } catch (Exception e) {
                throw new RuntimeException("Unable to layout " + this.getClass().getSimpleName() + ".", e);
            }
        }
        nodesProperties = new HashMap<>();
        for (Node node : nodes) {
            nodesProperties.put(node, new ForceAtlas2StrengthsLog());
            nodesProperties.get(node).setId(node.getId());
            nodesProperties.get(node).setLabel(node.getLabel());
            nodesProperties.get(node).setX(node.getX());
            nodesProperties.get(node).setY(node.getY());
            nodesProperties.get(node).setRepulsionDx(node.getDx());
            nodesProperties.get(node).setRepulsionDy(node.getDy());
        }

        // Attraction
        ForceFactory.AttractionForce Attraction = ForceFactory.getForceFactory()
                .buildAttraction(isLinLogMode(), isOutboundAttractionDistribution(), isAdjustSizes(),
                        1 * ((isOutboundAttractionDistribution()) ? (outboundAttCompensation) : (1)));
        if (getEdgeWeightInfluence() == 0) {
            for (Edge e : edges) {
                Attraction.apply(e.getSource(), e.getTarget(), 1);
            }
        } else if (getEdgeWeightInfluence() == 1) {
            for (Edge e : edges) {
                Attraction.apply(e.getSource(), e.getTarget(), getEdgeWeight(e));
            }
        } else {
            for (Edge e : edges) {
                Attraction.apply(e.getSource(), e.getTarget(),
                        Math.pow(getEdgeWeight(e), getEdgeWeightInfluence()));
            }
        }


        for (Node node : nodes) {
            nodesProperties.get(node).setAttractionDx(node.getDx() - nodesProperties.get(node).getRepulsionDx());
            nodesProperties.get(node).setAttractionDy(node.getDy() - nodesProperties.get(node).getRepulsionDy());
        }

        if (customForce != null) {
            customForce.apply(nodes, edges);
        }

        for (Node node : nodes) {
            nodesProperties.get(node).setCustomForceDx(
                    node.getDx() - nodesProperties.get(node).getRepulsionDx() - nodesProperties.get(node)
                            .getAttractionDx());
            nodesProperties.get(node).setCustomForceDy(
                    node.getDy() - nodesProperties.get(node).getRepulsionDy() - nodesProperties.get(node)
                            .getAttractionDy());
        }

        // Auto adjust speed
        double totalSwinging = 0d;  // How much irregular movement
        double totalEffectiveTraction = 0d;  // Hom much useful movement
        for (Node n : nodes) {
            if (!n.isFixed()) {
                double swinging = Math
                        .sqrt(Math.pow(n.getOldDx() - n.getDx(), 2) + Math.pow(n.getOldDy() - n.getDy(), 2));
                totalSwinging +=
                        n.getMass() * swinging;   // If the node has a burst change of direction, then it's not converging.
                totalEffectiveTraction += n.getMass() * 0.5 * Math
                        .sqrt(Math.pow(n.getOldDx() + n.getDx(), 2) + Math.pow(n.getOldDy() + n.getDy(), 2));
            }
        }
        // We want that swingingMovement < tolerance * convergenceMovement

        // Optimize jitter tolerance
        // The 'right' jitter tolerance for this network. Bigger networks need more tolerance. Denser networks need less tolerance. Totally empiric.
        double estimatedOptimalJitterTolerance = 0.05 * Math.sqrt(nodes.length);
        double minJT = Math.sqrt(estimatedOptimalJitterTolerance);
        double maxJT = 10;
        double jt = jitterTolerance * Math.max(minJT,
                Math.min(maxJT, estimatedOptimalJitterTolerance * totalEffectiveTraction / Math.pow(nodes.length, 2)));

        double minSpeedEfficiency = 0.05;

        // Protection against erratic behavior
        if (totalSwinging / totalEffectiveTraction > 2.0) {
            if (speedEfficiency > minSpeedEfficiency) {
                speedEfficiency *= 0.5;
            }
            jt = Math.max(jt, jitterTolerance);
        }

        double targetSpeed = jt * speedEfficiency * totalEffectiveTraction / totalSwinging;

        // Speed efficiency is how the speed really corresponds to the swinging vs. convergence tradeoff
        // We adjust it slowly and carefully
        if (totalSwinging > jt * totalEffectiveTraction) {
            if (speedEfficiency > minSpeedEfficiency) {
                speedEfficiency *= 0.7;
            }
        } else if (speed < 1000) {
            speedEfficiency *= 1.3;
        }

        // But the speed shoudn't rise too much too quickly, since it would make the convergence drop dramatically.
        double maxRise = 0.5;   // Max rise: 50%
        speed = speed + Math.min(targetSpeed - speed, maxRise * speed);
        // Apply forces
        if (isAdjustSizes()) {
            // If nodes overlap prevention is active, it's not possible to trust the swinging mesure.
            for (Node n : nodes) {
                if (!n.isFixed()) {

                    // Adaptive auto-speed: the speed of each node is lowered
                    // when the node swings.
                    double swinging = n.getMass() * Math.sqrt((n.getOldDx() - n.getDx()) * (n.getOldDx() - n.getDx())
                            + (n.getOldDy() - n.getDy()) * (n.getOldDy() - n.getDy()));
                    double factor = 0.1 * speed / (1f + Math.sqrt(speed * swinging));

                    double df = Math.sqrt(Math.pow(n.getDx(), 2) + Math.pow(n.getDy(), 2));
                    factor = Math.min(factor * df, 10.) / df;

                    double x = n.getX() + n.getDx() * factor;
                    double y = n.getY() + n.getDy() * factor;

                    nodesProperties.get(n).setActualDx(n.getDx() * factor);
                    nodesProperties.get(n).setActualDy(n.getDy() * factor);

                    n.setX((float) x);
                    n.setY((float) y);
                }
            }
        } else {
            for (Node n : nodes) {
                if (!n.isFixed()) {

                    // Adaptive auto-speed: the speed of each node is lowered
                    // when the node swings.
                    double swinging = n.getMass() * Math.sqrt((n.getOldDx() - n.getDx()) * (n.getOldDx() - n.getDx())
                            + (n.getOldDy() - n.getDy()) * (n.getOldDy() - n.getDy()));
                    //double factor = speed / (1f + Math.sqrt(speed * swinging));
                    double factor = speed / (1f + Math.sqrt(speed * swinging));

                    double x = n.getX() + n.getDx() * factor;
                    double y = n.getY() + n.getDy() * factor;

                    nodesProperties.get(n).setActualDx(n.getDx() * factor);
                    nodesProperties.get(n).setActualDy(n.getDy() * factor);

                    n.setX((float) x);
                    n.setY((float) y);
                }
            }
        }
    }

    public boolean canAlgo() {
        return graph != null;
    }

    public void endAlgo() {
        nodesProperties = null;
        try {
            pool.shutdown();
        } finally {
        }
    }


    public void resetPropertiesValues() {
        int nodesCount = 0;

        if (graph != null) {
            nodesCount = graph.getNodeCount();
        }

        // Tuning
        if (nodesCount >= 100) {
            setScalingRatio(2.0);
        } else {
            setScalingRatio(10.0);
        }
        setStrongGravityMode(false);
        setGravity(1.);

        // Behavior
        setOutboundAttractionDistribution(false);
        setLinLogMode(false);
        setAdjustSizes(false);
        setEdgeWeightInfluence(1.);

        // Performance
        setJitterTolerance(1d);
        if (nodesCount >= 1000) {
            setBarnesHutOptimize(true);
        } else {
            setBarnesHutOptimize(false);
        }
        setBarnesHutTheta(1.2);
        setThreadsCount(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
        // Trick: reset here to take the profile of the graph in account for default values
        resetPropertiesValues();
    }

    public Double getBarnesHutTheta() {
        return barnesHutTheta;
    }

    public void setBarnesHutTheta(Double barnesHutTheta) {
        this.barnesHutTheta = barnesHutTheta;
    }

    public Double getEdgeWeightInfluence() {
        return edgeWeightInfluence;
    }

    public void setEdgeWeightInfluence(Double edgeWeightInfluence) {
        this.edgeWeightInfluence = edgeWeightInfluence;
    }

    public Double getJitterTolerance() {
        return jitterTolerance;
    }

    public void setJitterTolerance(Double jitterTolerance) {
        this.jitterTolerance = jitterTolerance;
    }

    public Boolean isLinLogMode() {
        return linLogMode;
    }

    public void setLinLogMode(Boolean linLogMode) {
        this.linLogMode = linLogMode;
    }

    public Double getScalingRatio() {
        return scalingRatio;
    }

    public void setScalingRatio(Double scalingRatio) {
        this.scalingRatio = scalingRatio;
    }

    public Boolean isStrongGravityMode() {
        return strongGravityMode;
    }

    public void setStrongGravityMode(Boolean strongGravityMode) {
        this.strongGravityMode = strongGravityMode;
    }

    public Double getGravity() {
        return gravity;
    }

    public void setGravity(Double gravity) {
        this.gravity = gravity;
    }

    public ForceFactory.CustomForce getCustomForce() {
        return customForce;
    }

    public void setCustomForce(ForceFactory.CustomForce customForce) {
        this.customForce = customForce;
    }

    public Integer getThreadsCount() {
        return threadCount;
    }

    public void setThreadsCount(Integer threadCount) {
        this.threadCount = Math.max(1, threadCount);
    }

    public Boolean isOutboundAttractionDistribution() {
        return outboundAttractionDistribution;
    }

    public void setOutboundAttractionDistribution(Boolean outboundAttractionDistribution) {
        this.outboundAttractionDistribution = outboundAttractionDistribution;
    }

    public Boolean isAdjustSizes() {
        return adjustSizes;
    }

    public void setAdjustSizes(Boolean adjustSizes) {
        this.adjustSizes = adjustSizes;
    }

    public Boolean isBarnesHutOptimize() {
        return barnesHutOptimize;
    }

    public void setBarnesHutOptimize(Boolean barnesHutOptimize) {
        this.barnesHutOptimize = barnesHutOptimize;
    }

    public HashMap<Node, ForceAtlas2StrengthsLog> getNodesProperties() {
        return nodesProperties;
    }

    public RepulsionForceProvider getRepulsionForceProvider() {
        return repulsionForceProvider;
    }

    public void setRepulsionForceProvider(RepulsionForceProvider repulsionForceProvider) {
        this.repulsionForceProvider = repulsionForceProvider;
    }

    public void setNodePositionInitializer(NodePositionInitializer nodePositionInitializer) {
        this.nodePositionInitializer = nodePositionInitializer;
    }

    public NodePositionInitializer getNodePositionInitializer() {
        return nodePositionInitializer;
    }
}