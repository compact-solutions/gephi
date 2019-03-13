package org.gephi.model.impl;

import org.gephi.model.DataContainer;
import org.gephi.model.Edge;
import org.gephi.model.Node;
import org.gephi.preview.TextProperties;

import java.awt.*;
import java.util.Objects;

public class EdgeImpl implements Edge {

    private final String id;
    private final Node source;
    private final Node target;
    private String label;
    private double weight;
    private Color color;
    private boolean isDirected;
    private float alpha;
    private TextProperties textProperties;
    private final DataContainer dataContainer = new DataContainerImpl();

    public EdgeImpl(String id, Node source, Node target) {
        this.id = id;
        this.source = source;
        this.target = target;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public Node getSource() {
        return source;
    }

    @Override
    public Node getTarget() {
        return target;
    }

    @Override
    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    @Override
    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public boolean isDirected() {
        return isDirected;
    }

    public void setDirected(boolean directed) {
        isDirected = directed;
    }

    @Override
    public float alpha() {
        return alpha;
    }

    @Override
    public boolean isSelfLoop() {
        return Objects.equals(source, target);
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    @Override
    public TextProperties getTextProperties() {
        return textProperties;
    }

    public void setTextProperties(TextProperties textProperties) {
        this.textProperties = textProperties;
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
