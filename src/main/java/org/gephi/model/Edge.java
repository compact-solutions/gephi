package org.gephi.model;

import org.gephi.preview.TextProperties;

import java.awt.*;

public interface Edge extends DataContainer {

    Node getSource();

    Node getTarget();

    double getWeight();

    String getId();

    String getLabel();

    Color getColor();

    boolean isDirected();

    TextProperties getTextProperties();

    float alpha();

    boolean isSelfLoop();

}
