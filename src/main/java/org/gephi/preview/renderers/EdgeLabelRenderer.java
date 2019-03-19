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
import org.gephi.preview.*;
import org.gephi.preview.builders.EdgeBuilder;
import org.gephi.preview.builders.EdgeLabelBuilder;
import org.gephi.preview.builders.NodeBuilder;
import org.gephi.preview.items.EdgeItem;
import org.gephi.preview.items.EdgeLabelItem;
import org.gephi.preview.items.NodeItem;
import org.gephi.preview.types.DependantColor;
import org.gephi.preview.types.DependantOriginalColor;
import org.gephi.preview.types.EdgeColor;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.awt.*;

/**
 * @author Yudi Xue, Mathieu Bastian
 */
public class EdgeLabelRenderer implements Renderer {
    //Custom properties

    public static final String EDGE_COLOR = "edge.label.edgeColor";
    public static final String LABEL_X = "edge.label.x";
    public static final String LABEL_Y = "edge.label.y";
    //Default values
    protected final boolean defaultShowLabels = true;
    protected final Font defaultFont = new Font("Arial", Font.PLAIN, 10);
    protected final boolean defaultShorten = false;
    protected final DependantOriginalColor defaultColor = new DependantOriginalColor(DependantOriginalColor.Mode.ORIGINAL);
    protected final int defaultMaxChar = 30;
    protected final float defaultOutlineSize = 2;
    protected final DependantColor defaultOutlineColor = new DependantColor(Color.WHITE);
    protected final float defaultOutlineOpacity = 40;
    //Font cache
    protected Font font;

    @Override
    public void preProcess(PreviewModel previewModel) {
        PreviewProperties properties = previewModel.getProperties();
        if (properties.getBooleanValue(PreviewProperty.EDGE_LABEL_SHORTEN)) {
            //Shorten labels
            Item[] EdgeLabelsItems = previewModel.getItems(Item.EDGE_LABEL);

            int maxChars = properties.getIntValue(PreviewProperty.EDGE_LABEL_MAX_CHAR);
            for (Item item : EdgeLabelsItems) {
                String label = item.getData(EdgeLabelItem.LABEL);
                if (label.length() >= maxChars + 3) {
                    label = label.substring(0, maxChars) + "...";
                    item.setData(EdgeLabelItem.LABEL, label);
                }
            }
        }

        //Put parent color, and calculate position
        for (Item item : previewModel.getItems(Item.EDGE_LABEL)) {
            Edge edge = (Edge) item.getSource();
            Item edgeItem = previewModel.getItem(Item.EDGE, edge);

            EdgeColor edgeColor = properties.getValue(PreviewProperty.EDGE_COLOR);
            NodeItem sourceItem = edgeItem.getData(EdgeRenderer.SOURCE);
            NodeItem targetItem = edgeItem.getData(EdgeRenderer.TARGET);
            Color color = edgeColor.getColor(item.getData(EdgeItem.COLOR),
                    sourceItem.getData(NodeItem.COLOR),
                    targetItem.getData(NodeItem.COLOR));
            item.setData(EDGE_COLOR, color);
            if (edge.isSelfLoop()) {
                //Middle
                Float x = sourceItem.getData(NodeItem.X);
                Float y = sourceItem.getData(NodeItem.Y);
                Float size = sourceItem.getData(NodeItem.SIZE);

                Vector v1 = new Vector(x, y);
                v1.add(size, -size);

                Vector v2 = new Vector(x, y);
                v2.add(size, size);

                Vector middle = bezierPoint(x, y, v1.x, v1.y, v2.x, v2.y, x, y, 0.5f);
                item.setData(LABEL_X, middle.x);
                item.setData(LABEL_Y, middle.y);

            } else if (properties.getBooleanValue(PreviewProperty.EDGE_CURVED)) {
                //Middle of the curve
                Float x1 = sourceItem.getData(NodeItem.X);
                Float x2 = targetItem.getData(NodeItem.X);
                Float y1 = sourceItem.getData(NodeItem.Y);
                Float y2 = targetItem.getData(NodeItem.Y);

                //Curved edgs
                Vector direction = new Vector(x2, y2);
                direction.sub(new Vector(x1, y1));
                float length = direction.mag();
                direction.normalize();

                float factor = properties.getFloatValue(EdgeRenderer.BEZIER_CURVENESS) * length;

                // normal vector to the edge
                Vector n = new Vector(direction.y, -direction.x);
                n.mult(factor);

                // first control point
                Vector v1 = new Vector(direction.x, direction.y);
                v1.mult(factor);
                v1.add(new Vector(x1, y1));
                v1.add(n);

                // second control point
                Vector v2 = new Vector(direction.x, direction.y);
                v2.mult(-factor);
                v2.add(new Vector(x2, y2));
                v2.add(n);

                Vector middle = bezierPoint(x1, y1, v1.x, v1.y, v2.x, v2.y, x2, y2, 0.5f);
                item.setData(LABEL_X, middle.x);
                item.setData(LABEL_Y, middle.y);
            } else {
                Float x = ((Float) sourceItem.getData(NodeItem.X) + (Float) targetItem.getData(NodeItem.X)) / 2f;
                Float y = ((Float) sourceItem.getData(NodeItem.Y) + (Float) targetItem.getData(NodeItem.Y)) / 2f;
                item.setData(LABEL_X, x);
                item.setData(LABEL_Y, y);
            }
        }

        //Property font
        font = properties.getFontValue(PreviewProperty.EDGE_LABEL_FONT);
    }

    @Override
    public void render(Item item, RenderTarget target, PreviewProperties properties) {
        Edge edge = (Edge) item.getSource();
        //Label
        Color edgeColor = item.getData(EDGE_COLOR);
        Color color = item.getData(EdgeLabelItem.COLOR);
        DependantOriginalColor propColor = properties.getValue(PreviewProperty.EDGE_LABEL_COLOR);
        color = propColor.getColor(edgeColor, color);
        String label = item.getData(EdgeLabelItem.LABEL);
        Float x = item.getData(LABEL_X);
        Float y = item.getData(LABEL_Y);

        //Skip if empty
        if (label == null || label.trim().isEmpty()) {
            return;
        }

        //Outline
        DependantColor outlineDependantColor = properties.getValue(PreviewProperty.EDGE_LABEL_OUTLINE_COLOR);
        Float outlineSize = properties.getFloatValue(PreviewProperty.EDGE_LABEL_OUTLINE_SIZE);
        outlineSize = outlineSize * (font.getSize() / 32f);
        int outlineAlpha = (int) ((properties.getFloatValue(PreviewProperty.EDGE_LABEL_OUTLINE_OPACITY) / 100f) * 255f);
        if (outlineAlpha < 0) {
            outlineAlpha = 0;
        }
        if (outlineAlpha > 255) {
            outlineAlpha = 255;
        }
        Color outlineColor = outlineDependantColor.getColor(edgeColor);
        outlineColor = new Color(outlineColor.getRed(), outlineColor.getGreen(), outlineColor.getBlue(), outlineAlpha);

        if (target instanceof SVGTarget) {
            renderSVG((SVGTarget) target, edge, label, x, y, color, outlineSize, outlineColor);
        }
    }

    @Override
    public CanvasSize getCanvasSize(
            Item item,
            PreviewProperties properties) {
        //FIXME Compute the label canvas
        return new CanvasSize();
    }

    public void renderSVG(SVGTarget target, Edge edge, String label, float x, float y, Color color, float outlineSize, Color outlineColor) {
        Text labelText = target.createTextNode(label);

        if (outlineSize > 0) {
            Text labelTextOutline = target.createTextNode(label);
            Element outlineElem = target.createElement("text");
            outlineElem.setAttribute("class", SVGUtils.idAsClassAttribute(edge.getId()));
            outlineElem.setAttribute("x", String.valueOf(x));
            outlineElem.setAttribute("y", String.valueOf(y));
            outlineElem.setAttribute("style", "text-anchor: middle; dominant-baseline: central;");
            outlineElem.setAttribute("fill", target.toHexString(color));
            outlineElem.setAttribute("font-family", font.getFamily());
            outlineElem.setAttribute("font-size", font.getSize() + "");
            outlineElem.setAttribute("stroke", target.toHexString(outlineColor));
            outlineElem.setAttribute("stroke-width", (outlineSize * target.getScaleRatio()) + "px");
            outlineElem.setAttribute("stroke-linecap", "round");
            outlineElem.setAttribute("stroke-linejoin", "round");
            outlineElem.setAttribute("stroke-opacity", String.valueOf(outlineColor.getAlpha() / 255f));
            outlineElem.appendChild(labelTextOutline);
            target.getTopElement(SVGTarget.TOP_NODE_LABELS_OUTLINE).appendChild(outlineElem);
        }

        Element labelElem = target.createElement("text");
        labelElem.setAttribute("class", SVGUtils.idAsClassAttribute(edge.getId()));
        labelElem.setAttribute("x", x + "");
        labelElem.setAttribute("y", y + "");
        labelElem.setAttribute("style", "text-anchor: middle; dominant-baseline: central;");
        labelElem.setAttribute("fill", target.toHexString(color));
        labelElem.setAttribute("font-family", font.getFamily());
        labelElem.setAttribute("font-size", font.getSize() + "");
        labelElem.appendChild(labelText);
        target.getTopElement(SVGTarget.TOP_EDGE_LABELS).appendChild(labelElem);
    }

    @Override
    public PreviewProperty[] getProperties() {
        return new PreviewProperty[]{
                PreviewProperty.createProperty(this, PreviewProperty.SHOW_EDGE_LABELS, Boolean.class).setValue(defaultShowLabels),
                PreviewProperty.createProperty(this, PreviewProperty.EDGE_LABEL_FONT, Font.class).setValue(defaultFont),
                PreviewProperty.createProperty(this, PreviewProperty.EDGE_LABEL_COLOR, DependantOriginalColor.class).setValue(defaultColor),
                PreviewProperty.createProperty(this, PreviewProperty.EDGE_LABEL_SHORTEN, Boolean.class).setValue(defaultShorten),
                PreviewProperty.createProperty(this, PreviewProperty.EDGE_LABEL_MAX_CHAR, Integer.class).setValue(defaultMaxChar),
                PreviewProperty.createProperty(this, PreviewProperty.EDGE_LABEL_OUTLINE_SIZE, Float.class).setValue(defaultOutlineSize),
                PreviewProperty.createProperty(this, PreviewProperty.EDGE_LABEL_OUTLINE_COLOR, DependantColor.class).setValue(defaultOutlineColor),
                PreviewProperty.createProperty(this, PreviewProperty.EDGE_LABEL_OUTLINE_OPACITY, Float.class).setValue(defaultOutlineOpacity),};
    }

    private boolean showEdgeLabels(PreviewProperties properties) {
        return properties.getBooleanValue(PreviewProperty.SHOW_EDGE_LABELS);
    }

    @Override
    public boolean isRendererForItem(Item item, PreviewProperties properties) {
        return item instanceof EdgeLabelItem && showEdgeLabels(properties);
    }

    @Override
    public boolean needsItemBuilder(ItemBuilder itemBuilder, PreviewProperties properties) {
        return (itemBuilder instanceof EdgeLabelBuilder || itemBuilder instanceof NodeBuilder || itemBuilder instanceof EdgeBuilder) && showEdgeLabels(properties);//Needs some properties of nodes and edges
    }

    protected Vector bezierPoint(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, float c) {
        Vector ab = linearInterpolation(x1, y1, x2, y2, c);
        Vector bc = linearInterpolation(x2, y2, x3, y3, c);
        Vector cd = linearInterpolation(x3, y3, x4, y4, c);
        Vector abbc = linearInterpolation(ab.x, ab.y, bc.x, bc.y, c);
        Vector bccd = linearInterpolation(bc.x, bc.y, cd.x, cd.y, c);
        return linearInterpolation(abbc.x, abbc.y, bccd.x, bccd.y, c);
    }

    protected Vector linearInterpolation(float x1, float y1, float x2, float y2, float c) {
        Vector r = new Vector(x1 + (x2 - x1) * c, y1 + (y2 - y1) * c);
        return r;
    }

}
