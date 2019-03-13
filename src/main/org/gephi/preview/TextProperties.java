/*
 * Copyright 2012-2013 Gephi Consortium
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.gephi.preview;

import java.awt.*;

/**
 * Text visual properties.
 */
public interface TextProperties {

    /**
     * Returns the red color component between zero and one.
     *
     * @return the red color component
     */
    double getR();

    /**
     * Returns the green color component between zero and one.
     *
     * @return the green color component
     */
    double getG();

    /**
     * Returns the blue color component between zero and one.
     *
     * @return the blue color component
     */
    double getB();

    /**
     * Returns the RGBA color.
     *
     * @return the color
     */
    int getRGBA();

    /**
     * Returns the color.
     *
     * @return the color
     */
    Color getColor();

    /**
     * Returns the alpha (transparency) component between zero and one.
     *
     * @return the alpha
     */
    double getAlpha();

    /**
     * Returns the size.
     *
     * @return the size
     */
    double getSize();

    /**
     * Returns true if visible.
     *
     * @return true if visible, false otherwise
     */
    boolean isVisible();

    /**
     * Returns the text.
     *
     * @return the text
     */
    String getText();

    /**
     * Returns the text's width.
     *
     * @return the width
     */
    double getWidth();

    /**
     * Returns the text's height.
     *
     * @return the height
     */
    double getHeight();

    /**
     * Sets the red color component.
     *
     * @param r the color component, between zero and one
     */
    void setR(double r);

    /**
     * Sets the green color component.
     *
     * @param g the color component, between zero and one
     */
    void setG(double g);

    /**
     * Sets the blue color component.
     *
     * @param b the color component, between zero and one
     */
    void setB(double b);

    /**
     * Sets the alpha (transparency) component.
     *
     * @param a the alpha component, between zero and one
     */
    void setAlpha(double a);

    /**
     * Sets the color.
     *
     * @param color the color
     */
    void setColor(Color color);

    /**
     * Sets the size.
     *
     * @param size the size
     */
    void setSize(double size);

    /**
     * Sets the visibility.
     *
     * @param visible true if visible, false otherwise
     */
    void setVisible(boolean visible);

    /**
     * Sets the text.
     *
     * @param text the text
     */
    void setText(String text);

    /**
     * Sets the text's dimensions.
     *
     * @param width  width
     * @param height height
     */
    void setDimensions(double width, double height);
}
