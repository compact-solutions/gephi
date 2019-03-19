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
import java.util.Objects;

public class TextPropertiesImpl implements TextProperties {

    protected boolean visible;
    protected int rgba;
    protected double size;
    protected String text;
    protected double width;
    protected double height;

    public TextPropertiesImpl() {
        this.rgba = 255 << 24; // Alpha set to 1
        this.size = 1f;
        this.visible = true;
    }

    @Override
    public double getR() {
        return ((rgba >> 16) & 0xFF) / 255.0;
    }

    @Override
    public double getG() {
        return ((rgba >> 8) & 0xFF) / 255.0;
    }

    @Override
    public double getB() {
        return (rgba & 0xFF) / 255.0;
    }

    @Override
    public double getAlpha() {
        return ((rgba >> 24) & 0xFF) / 255.0;
    }

    @Override
    public int getRGBA() {
        return rgba;
    }

    @Override
    public Color getColor() {
        return new Color(rgba, true);
    }

    @Override
    public double getSize() {
        return size;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public double getWidth() {
        return width;
    }

    @Override
    public double getHeight() {
        return height;
    }

    @Override
    public void setR(double r) {
        rgba = (rgba & 0xFF00FFFF) | (((int) (r * 255.0)) << 16);
    }

    @Override
    public void setG(double g) {
        rgba = (rgba & 0xFFFF00FF) | ((int) (g * 255.0)) << 8;
    }

    @Override
    public void setB(double b) {
        rgba = (rgba & 0xFFFFFF00) | ((int) (b * 255.0));
    }

    @Override
    public void setAlpha(double a) {
        rgba = (rgba & 0xFFFFFF) | ((int) (a * 255.0)) << 24;
    }

    @Override
    public void setColor(Color color) {
        this.rgba = (color.getAlpha() << 24) | color.getRGB();
    }

    @Override
    public void setSize(double size) {
        this.size = size;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void setText(String text) {
        this.text = text;
    }

    @Override
    public void setDimensions(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public int deepHashCode() {
        return Objects.hash(visible, rgba, size, width, height, text);
    }

    public boolean deepEquals(TextPropertiesImpl obj) {
        if (obj == null) {
            return false;
        }
        if (this.visible != obj.visible) {
            return false;
        }
        if (this.rgba != obj.rgba) {
            return false;
        }
        if (Double.doubleToLongBits(this.size) != Double.doubleToLongBits(obj.size)) {
            return false;
        }
        if (Double.doubleToLongBits(this.width) != Double.doubleToLongBits(obj.width)) {
            return false;
        }
        if (Double.doubleToLongBits(this.height) != Double.doubleToLongBits(obj.height)) {
            return false;
        }
        if ((this.text == null) ? (obj.text != null) : !this.text.equals(obj.text)) {
            return false;
        }
        return true;
    }
}
