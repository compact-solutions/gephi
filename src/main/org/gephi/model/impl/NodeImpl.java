package org.gephi.model.impl;

import org.gephi.model.DataContainer;
import org.gephi.model.Node;
import org.gephi.preview.TextProperties;

import java.awt.*;

public class NodeImpl implements Node {

    private final String id;
    private String label;
    private float x;
    private float y;
    private float dx;
    private float dy;
    private float oldDx;
    private float oldDy;
    private float size;
    private float mass;
    private boolean isFixed;
    private Color color;
    private String category;
    private TextProperties textProperties;
    private final DataContainer dataContainer = new DataContainerImpl();


    public NodeImpl(String id) {
        this.id = id;
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
    public float getX() {
        return x;
    }

    @Override
    public void setX(float x) {
        this.x = x;
    }

    @Override
    public float getY() {
        return y;
    }

    @Override
    public void setY(float y) {
        this.y = y;
    }

    @Override
    public float getMass() {
        return mass;
    }

    @Override
    public void setMass(float mass) {
        this.mass = mass;
    }

    @Override
    public float getDx() {
        return dx;
    }

    @Override
    public void setDx(float dx) {
        this.dx = dx;
    }

    @Override
    public float getDy() {
        return dy;
    }

    @Override
    public void setDy(float dy) {
        this.dy = dy;
    }

    @Override
    public float getOldDx() {
        return oldDx;
    }

    @Override
    public void setOldDx(float oldDx) {
        this.oldDx = oldDx;
    }

    @Override
    public float getOldDy() {
        return oldDy;
    }

    @Override
    public void setOldDy(float oldDy) {
        this.oldDy = oldDy;
    }

    @Override
    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    @Override
    public synchronized void shiftDx(double value) {
        dx += value;
    }

    @Override
    public synchronized void shiftDy(double value) {
        dy += value;
    }

    @Override
    public boolean isFixed() {
        return isFixed;
    }

    public void setFixed(boolean fixed) {
        isFixed = fixed;
    }

    @Override
    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public TextProperties getTextProperties() {
        return textProperties;
    }

    public void setTextProperties(TextProperties textProperties) {
        this.textProperties = textProperties;
    }

    @Override
    public <D> D getData(String key) {
        return dataContainer.getData(key);
    }

    @Override
    public boolean hasData(String key) {
        return dataContainer.hasData(key);
    }

    @Override
    public void setData(String key, Object value) {
        dataContainer.setData(key, value);
    }

    @Override
    public String[] getDataKeys() {
        return dataContainer.getDataKeys();
    }
}
