/*
 Copyright 2008-2011 Gephi
 Authors : Yudi Xue <yudi.xue@usask.ca>, Mathieu Bastian
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
package org.gephi.preview.renderers;

import org.gephi.model.Edge;
import org.gephi.model.Node;
import org.gephi.preview.*;
import org.gephi.preview.builders.EdgeBuilder;
import org.gephi.preview.builders.NodeBuilder;
import org.gephi.preview.items.EdgeItem;
import org.gephi.preview.items.NodeItem;
import org.gephi.preview.types.EdgeColor;
import org.w3c.dom.Element;

import java.awt.*;
import java.util.Locale;

/**
 * @author Yudi Xue, Mathieu Bastian
 */
public class EdgeRenderer implements Renderer {

    //Custom properties
    public static final String EDGE_MIN_WEIGHT = "edge.min-weight";
    public static final String EDGE_MAX_WEIGHT = "edge.max-weight";
    public static final String BEZIER_CURVENESS = "edge.bezier-curveness";
    public static final String SOURCE = "source";
    public static final String TARGET = "target";
    public static final String TARGET_RADIUS = "edge.target.radius";
    public static final String SOURCE_RADIUS = "edge.source.radius";
    //Default values
    protected boolean defaultShowEdges = true;
    protected float defaultThickness = 1;
    protected boolean defaultRescaleWeight = true;
    protected float defaultRescaleWeightMin = 0.1f;
    protected float defaultRescaleWeightMax = 1.0f;
    protected EdgeColor defaultColor = new EdgeColor(EdgeColor.Mode.MIXED);
    protected boolean defaultEdgeCurved = true;
    protected float defaultBezierCurviness = 0.2f;
    protected int defaultOpacity = 100;
    protected float defaultRadius = 0f;

    private static final StraightEdgeRenderer STRAIGHT_RENDERER
            = new StraightEdgeRenderer();
    private static final CurvedEdgeRenderer CURVED_RENDERER
            = new CurvedEdgeRenderer();
    private static final SelfLoopEdgeRenderer SELF_LOOP_RENDERER
            = new SelfLoopEdgeRenderer();

    @Override
    public void preProcess(PreviewModel previewModel) {
        PreviewProperties properties = previewModel.getProperties();
        Item[] edgeItems = previewModel.getItems(Item.EDGE);

        //Put nodes in edge item
        for (Item item : edgeItems) {
            Edge edge = (Edge) item.getSource();
            Node source = edge.getSource();
            Node target = edge.getTarget();
            Item nodeSource = previewModel.getItem(Item.NODE, source);
            Item nodeTarget = previewModel.getItem(Item.NODE, target);
            item.setData(SOURCE, nodeSource);
            item.setData(TARGET, nodeTarget);
        }

        //Calculate max and min weight
        double minWeight = Double.POSITIVE_INFINITY;
        double maxWeight = Double.NEGATIVE_INFINITY;

        for (Item edge : edgeItems) {
            minWeight = Math.min(
                    minWeight,
                    edge.getData(EdgeItem.WEIGHT));
            maxWeight = Math.max(
                    maxWeight,
                    edge.getData(EdgeItem.WEIGHT));
        }
        properties.putValue(EDGE_MIN_WEIGHT, minWeight);
        properties.putValue(EDGE_MAX_WEIGHT, maxWeight);

        //Put bezier curveness in properties
        if (!properties.hasProperty(BEZIER_CURVENESS)) {
            properties.putValue(BEZIER_CURVENESS, defaultBezierCurviness);
        }

        //Rescale weight if necessary - and avoid negative weights
        boolean rescaleWeight = properties.getBooleanValue(
                PreviewProperty.EDGE_RESCALE_WEIGHT);

        if (rescaleWeight) {
            double weightDiff = maxWeight - minWeight;
            double minRescaledWeight = properties.getFloatValue(PreviewProperty.EDGE_RESCALE_WEIGHT_MIN);
            double maxRescaledWeight = properties.getFloatValue(PreviewProperty.EDGE_RESCALE_WEIGHT_MAX);

            if (minRescaledWeight < 0) {
                minRescaledWeight = defaultRescaleWeightMin;
                properties.putValue(PreviewProperty.EDGE_RESCALE_WEIGHT_MIN, defaultRescaleWeightMin);
            }

            if (maxRescaledWeight < 0) {
                maxRescaledWeight = defaultRescaleWeightMax;
                properties.putValue(PreviewProperty.EDGE_RESCALE_WEIGHT_MAX, defaultRescaleWeightMax);
            }

            if (minRescaledWeight > maxRescaledWeight) {
                minRescaledWeight = maxRescaledWeight;
            }

            double rescaledWeightsDiff = maxRescaledWeight - minRescaledWeight;

            if (!Double.isInfinite(minWeight)
                    && !Double.isInfinite(maxWeight)
                    && !(maxWeight == minWeight || Math.abs(maxWeight - minWeight) < 1e-5)) {
                for (Item item : edgeItems) {
                    double weight = item.getData(EdgeItem.WEIGHT);
                    weight = rescaledWeightsDiff * (weight - minWeight) / weightDiff + minRescaledWeight;
                    setEdgeWeight(weight, properties, item);
                }
            } else {
                for (Item item : edgeItems) {
                    setEdgeWeight(1.0, properties, item);
                }
            }
        } else {
            for (Item item : edgeItems) {
                double weight = item.getData(EdgeItem.WEIGHT);

                if (minWeight <= 0) {
                    //Avoid negative weight
                    weight += Math.abs(minWeight) + 1;
                }

                //Multiply by thickness
                setEdgeWeight(weight, properties, item);
            }
        }

        //Radius
        for (Item item : edgeItems) {
            if (!(Boolean) item.getData(EdgeItem.SELF_LOOP)) {
                float edgeRadius
                        = properties.getFloatValue(PreviewProperty.EDGE_RADIUS);

                boolean isDirected = item.getData(EdgeItem.DIRECTED);
                if (isDirected
                        || edgeRadius > 0F) {
                    //Target
                    Item targetItem = item.getData(TARGET);
                    Double weight = item.getData(EdgeItem.WEIGHT);
                    //Avoid negative arrow size:
                    float arrowSize = properties.getFloatValue(
                            PreviewProperty.ARROW_SIZE);
                    if (arrowSize < 0F) {
                        arrowSize = 0F;
                    }

                    float arrowRadiusSize = isDirected ? arrowSize * weight.floatValue() : 0f;

                    float targetRadius = -(edgeRadius
                            + (Float) targetItem.getData(NodeItem.SIZE) / 2f
                            + properties.getFloatValue(PreviewProperty.NODE_BORDER_WIDTH) / 2f //We have to divide by 2 because the border stroke is not only an outline but also draws the other half of the curve inside the node
                            + arrowRadiusSize);
                    item.setData(TARGET_RADIUS, targetRadius);

                    //Source
                    Item sourceItem = item.getData(SOURCE);
                    float sourceRadius = -(edgeRadius
                            + (Float) sourceItem.getData(NodeItem.SIZE) / 2f
                            + properties.getFloatValue(PreviewProperty.NODE_BORDER_WIDTH) / 2f);
                    item.setData(SOURCE_RADIUS, sourceRadius);
                }
            }
        }
    }

    private void setEdgeWeight(double weight, PreviewProperties properties, Item item) {
        //Multiply by thickness
        weight *= properties.getFloatValue(PreviewProperty.EDGE_THICKNESS);
        item.setData(EdgeItem.WEIGHT, weight);
    }

    @Override
    public void render(
            Item item,
            RenderTarget target,
            PreviewProperties properties) {
        if (isSelfLoopEdge(item)) {
            SELF_LOOP_RENDERER.render(item, target, properties);
        } else if (properties.getBooleanValue(PreviewProperty.EDGE_CURVED)) {
            CURVED_RENDERER.render(item, target, properties);
        } else {
            STRAIGHT_RENDERER.render(item, target, properties);
        }
    }

    @Override
    public CanvasSize getCanvasSize(Item item, PreviewProperties properties) {
        if (isSelfLoopEdge(item)) {
            return SELF_LOOP_RENDERER.getCanvasSize(item, properties);
        } else if (properties.getBooleanValue(PreviewProperty.EDGE_CURVED)) {
            return CURVED_RENDERER.getCanvasSize(item, properties);
        } else {
            return STRAIGHT_RENDERER.getCanvasSize(item, properties);
        }
    }

    @Override
    public PreviewProperty[] getProperties() {
        return new PreviewProperty[]{
                PreviewProperty.createProperty(this, PreviewProperty.SHOW_EDGES, Boolean.class).setValue(defaultShowEdges),
                PreviewProperty.createProperty(this, PreviewProperty.EDGE_THICKNESS, Float.class).setValue(defaultThickness),
                PreviewProperty.createProperty(this, PreviewProperty.EDGE_RESCALE_WEIGHT, Boolean.class).setValue(defaultRescaleWeight),
                PreviewProperty.createProperty(this, PreviewProperty.EDGE_RESCALE_WEIGHT_MIN, Float.class).setValue(defaultRescaleWeightMin),
                PreviewProperty.createProperty(this, PreviewProperty.EDGE_RESCALE_WEIGHT_MAX, Float.class).setValue(defaultRescaleWeightMax),
                PreviewProperty.createProperty(this, PreviewProperty.EDGE_COLOR, EdgeColor.class).setValue(defaultColor),
                PreviewProperty.createProperty(this, PreviewProperty.EDGE_OPACITY, Float.class).setValue(defaultOpacity),
                PreviewProperty.createProperty(this, PreviewProperty.EDGE_CURVED, Boolean.class).setValue(defaultEdgeCurved),
                PreviewProperty.createProperty(this, PreviewProperty.EDGE_RADIUS, Float.class).setValue(defaultRadius),};
    }

    @Override
    public boolean isRendererForItem(Item item, PreviewProperties properties) {
        if (item instanceof EdgeItem) {
            return showEdges(properties);
        }
        return false;
    }

    @Override
    public boolean needsItemBuilder(ItemBuilder itemBuilder, PreviewProperties properties) {
        return (itemBuilder instanceof EdgeBuilder
                || itemBuilder instanceof NodeBuilder)
                && showEdges(properties);//Needs some properties of nodes
    }

    public static Color getColor(
            Item item,
            PreviewProperties properties) {
        Item sourceItem = item.getData(SOURCE);
        Item targetItem = item.getData(TARGET);
        EdgeColor edgeColor
                = properties.getValue(PreviewProperty.EDGE_COLOR);
        Color color = edgeColor.getColor(
                item.getData(EdgeItem.COLOR),
                sourceItem.getData(NodeItem.COLOR),
                targetItem.getData(NodeItem.COLOR));
        return new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                (int) (getAlpha(properties) * 255));
    }

    private boolean showEdges(PreviewProperties properties) {
        return properties.getBooleanValue(PreviewProperty.SHOW_EDGES);
    }

    private static boolean isSelfLoopEdge(Item item) {
        Item sourceItem = item.getData(SOURCE);
        Item targetItem = item.getData(TARGET);
        return item instanceof EdgeItem && sourceItem == targetItem;
    }

    private static float getAlpha(PreviewProperties properties) {
        float opacity = properties.getIntValue(PreviewProperty.EDGE_OPACITY) / 100F;
        if (opacity < 0) {
            opacity = 0;
        }
        if (opacity > 1) {
            opacity = 1;
        }
        return opacity;
    }

    private static float getThickness(Item item) {
        return ((Double) item.getData(EdgeItem.WEIGHT)).floatValue();
    }

    private static class StraightEdgeRenderer {

        public void render(
                Item item,
                RenderTarget target,
                PreviewProperties properties) {
            Helper h = new Helper(item);
            Color color = getColor(item, properties);

            if (target instanceof SVGTarget) {
                SVGTarget svgTarget = (SVGTarget) target;
                Element edgeElem = svgTarget.createElement("path");
                edgeElem.setAttribute("class", String.format(
                        "%s %s",
                        SVGUtils.idAsClassAttribute(((Node) h.sourceItem.getSource()).getId()),
                        SVGUtils.idAsClassAttribute(((Node) h.targetItem.getSource()).getId())
                ));
                edgeElem.setAttribute("d", String.format(
                        Locale.ENGLISH,
                        "M %f,%f L %f,%f",
                        h.x1, h.y1, h.x2, h.y2));
                edgeElem.setAttribute("stroke", svgTarget.toHexString(color));
                edgeElem.setAttribute(
                        "stroke-width",
                        Float.toString(getThickness(item)
                                * svgTarget.getScaleRatio()));
                edgeElem.setAttribute(
                        "stroke-opacity",
                        (color.getAlpha() / 255f) + "");
                edgeElem.setAttribute("fill", "none");
                svgTarget.getTopElement(SVGTarget.TOP_EDGES)
                        .appendChild(edgeElem);
            }
        }

        public CanvasSize getCanvasSize(
                Item item,
                PreviewProperties properties) {
            Item sourceItem = item.getData(SOURCE);
            Item targetItem = item.getData(TARGET);
            Float x1 = sourceItem.getData(NodeItem.X);
            Float x2 = targetItem.getData(NodeItem.X);
            Float y1 = sourceItem.getData(NodeItem.Y);
            Float y2 = targetItem.getData(NodeItem.Y);
            float minX = Math.min(x1, x2);
            float minY = Math.min(y1, y2);
            float maxX = Math.max(x1, x2);
            float maxY = Math.max(y1, y2);
            return new CanvasSize(minX, minY, maxX - minX, maxY - minY);
        }

        private class Helper {

            public final Item sourceItem;
            public final Item targetItem;
            public final Float x1;
            public final Float x2;
            public final Float y1;
            public final Float y2;

            public Helper(Item item) {
                sourceItem = item.getData(SOURCE);
                targetItem = item.getData(TARGET);

                Float _x1 = sourceItem.getData(NodeItem.X);
                Float _x2 = targetItem.getData(NodeItem.X);
                Float _y1 = sourceItem.getData(NodeItem.Y);
                Float _y2 = targetItem.getData(NodeItem.Y);

                //Target radius - to start at the base of the arrow
                Float targetRadius = item.getData(TARGET_RADIUS);
                //Avoid edge from passing the node's center:
                if (targetRadius != null && targetRadius < 0) {
                    Vector direction = new Vector(_x2, _y2);
                    direction.sub(new Vector(_x1, _y1));
                    direction.normalize();
                    direction.mult(targetRadius);
                    direction.add(new Vector(_x2, _y2));
                    _x2 = direction.x;
                    _y2 = direction.y;
                }

                //Source radius
                Float sourceRadius = item.getData(SOURCE_RADIUS);
                //Avoid edge from passing the node's center:
                if (sourceRadius != null && sourceRadius < 0) {
                    Vector direction = new Vector(_x1, _y1);
                    direction.sub(new Vector(_x2, _y2));
                    direction.normalize();
                    direction.mult(sourceRadius);
                    direction.add(new Vector(_x1, _y1));
                    _x1 = direction.x;
                    _y1 = direction.y;
                }

                x1 = _x1;
                y1 = _y1;
                x2 = _x2;
                y2 = _y2;
            }
        }
    }

    private static class CurvedEdgeRenderer {

        public void render(
                Item item,
                RenderTarget target,
                PreviewProperties properties) {
            Helper h = new Helper(item, properties);
            Color color = getColor(item, properties);

            if (target instanceof SVGTarget) {
                SVGTarget svgTarget = (SVGTarget) target;
                Element edgeElem = svgTarget.createElement("path");
                edgeElem.setAttribute("class", String.format(
                        "%s %s",
                        SVGUtils.idAsClassAttribute(((Node) h.sourceItem.getSource()).getId()),
                        SVGUtils.idAsClassAttribute(((Node) h.targetItem.getSource()).getId())
                ));
                edgeElem.setAttribute("d", String.format(
                        Locale.ENGLISH,
                        "M %f,%f C %f,%f %f,%f %f,%f",
                        h.x1, h.y1,
                        h.v1.x, h.v1.y, h.v2.x, h.v2.y, h.x2, h.y2));
                edgeElem.setAttribute("stroke", svgTarget.toHexString(color));
                edgeElem.setAttribute(
                        "stroke-width",
                        Float.toString(getThickness(item)
                                * svgTarget.getScaleRatio()));
                edgeElem.setAttribute(
                        "stroke-opacity",
                        (color.getAlpha() / 255f) + "");
                edgeElem.setAttribute("fill", "none");
                svgTarget.getTopElement(SVGTarget.TOP_EDGES)
                        .appendChild(edgeElem);
            }
        }

        public CanvasSize getCanvasSize(
                Item item,
                PreviewProperties properties) {
            Helper h = new Helper(item, properties);
            float minX
                    = Math.min(Math.min(Math.min(h.x1, h.x2), h.v1.x), h.v2.x);
            float minY
                    = Math.min(Math.min(Math.min(h.y1, h.y2), h.v1.y), h.v2.y);
            float maxX
                    = Math.max(Math.max(Math.max(h.x1, h.x2), h.v1.x), h.v2.x);
            float maxY
                    = Math.max(Math.max(Math.max(h.y1, h.y2), h.v1.y), h.v2.y);
            return new CanvasSize(minX, minY, maxX - minX, maxY - minY);
        }

        private class Helper {

            public final Item sourceItem;
            public final Item targetItem;
            public final Float x1;
            public final Float x2;
            public final Float y1;
            public final Float y2;
            public final Vector v1;
            public final Vector v2;

            public Helper(
                    Item item,
                    PreviewProperties properties) {
                sourceItem = item.getData(SOURCE);
                targetItem = item.getData(TARGET);

                x1 = sourceItem.getData(NodeItem.X);
                x2 = targetItem.getData(NodeItem.X);
                y1 = sourceItem.getData(NodeItem.Y);
                y2 = targetItem.getData(NodeItem.Y);

                Vector direction = new Vector(x2, y2);
                direction.sub(new Vector(x1, y1));

                float length = direction.mag();

                direction.normalize();
                float factor
                        = properties.getFloatValue(BEZIER_CURVENESS) * length;

                Vector n = new Vector(direction.y, -direction.x);
                n.mult(factor);

                v1 = computeCtrlPoint(x1, y1, direction, factor, n);
                v2 = computeCtrlPoint(x2, y2, direction, -factor, n);
            }

            private Vector computeCtrlPoint(
                    Float x,
                    Float y,
                    Vector direction,
                    float factor,
                    Vector normalVector) {
                Vector v = new Vector(direction.x, direction.y);
                v.mult(factor);
                v.add(new Vector(x, y));
                v.add(normalVector);
                return v;
            }
        }
    }

    private static class SelfLoopEdgeRenderer {

        public static final String ID = "SelfLoopEdge";

        public void render(
                Item item,
                RenderTarget target,
                PreviewProperties properties) {
            Helper h = new Helper(item);
            Color color = getColor(item, properties);

            if (target instanceof SVGTarget) {
                SVGTarget svgTarget = (SVGTarget) target;

                Element selfLoopElem = svgTarget.createElement("path");
                selfLoopElem.setAttribute("d", String.format(
                        Locale.ENGLISH,
                        "M %f,%f C %f,%f %f,%f %f,%f",
                        h.x, h.y, h.v1.x, h.v1.y, h.v2.x, h.v2.y, h.x, h.y));
                selfLoopElem.setAttribute("class", SVGUtils.idAsClassAttribute(h.node.getId()));
                selfLoopElem.setAttribute(
                        "stroke",
                        svgTarget.toHexString(color));
                selfLoopElem.setAttribute(
                        "stroke-opacity",
                        (color.getAlpha() / 255f) + "");
                selfLoopElem.setAttribute("stroke-width", Float.toString(
                        getThickness(item) * svgTarget.getScaleRatio()));
                selfLoopElem.setAttribute("fill", "none");
                svgTarget.getTopElement(SVGTarget.TOP_EDGES)
                        .appendChild(selfLoopElem);
            }
        }

        public CanvasSize getCanvasSize(
                Item item,
                PreviewProperties properties) {
            Helper h = new Helper(item);
            float minX = Math.min(Math.min(h.x, h.v1.x), h.v2.x);
            float minY = Math.min(Math.min(h.y, h.v1.y), h.v2.y);
            float maxX = Math.max(Math.max(h.x, h.v1.x), h.v2.x);
            float maxY = Math.max(Math.max(h.y, h.v1.y), h.v2.y);
            return new CanvasSize(minX, minY, maxX - minX, maxY - minY);
        }

        private class Helper {

            public final Float x;
            public final Float y;
            public final Node node;
            public final Vector v1;
            public final Vector v2;

            public Helper(Item item) {
                node = ((Edge) item.getSource()).getSource();

                Item nodeSource = item.getData(SOURCE);
                x = nodeSource.getData(NodeItem.X);
                y = nodeSource.getData(NodeItem.Y);
                Float size = nodeSource.getData(NodeItem.SIZE);

                v1 = new Vector(x, y);
                v1.add(size, -size);

                v2 = new Vector(x, y);
                v2.add(size, size);
            }
        }
    }
}
