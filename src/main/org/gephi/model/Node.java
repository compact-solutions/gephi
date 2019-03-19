package org.gephi.model;

import org.gephi.preview.TextProperties;

import java.awt.*;

public interface Node extends DataContainer {

    String getId();

    String getLabel();

    float getX();

    void setX(float x);

    float getY();

    void setY(float y);

    float getMass();

    void setMass(float mass);

    float getDx();

    void setDx(float dx);

    float getDy();

    void setDy(float dy);

    float getOldDx();

    void setOldDx(float oldDx);

    float getOldDy();

    void setOldDy(float oldDy);

    float getSize();

    void shiftDx(double value);

    void shiftDy(double value);

    boolean isFixed();

    Color getColor();

    String getCategory();

    TextProperties getTextProperties();

    Color getBorderColor();

    Float getBorderWidth();
}
