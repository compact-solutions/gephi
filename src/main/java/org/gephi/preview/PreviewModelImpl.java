/*
 Copyright 2008-2011 Gephi
 Authors : Mathieu Bastian
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
package org.gephi.preview;

import org.gephi.preview.presets.DefaultPreset;

import java.util.*;

/**
 * @author Mathieu Bastian
 */
public class PreviewModelImpl implements PreviewModel {

    private final PreviewController previewController;
    //Items
    private final Map<String, List<Item>> typeMap;
    private final Map<Object, Object> sourceMap;
    //Renderers
    private ManagedRenderer[] managedRenderers;
    //Properties
    private PreviewProperties properties;

    private ItemBuilder[] itemBuilders = new ItemBuilder[0];
    private RenderTargetBuilder[] renderTargetBuilders = new RenderTargetBuilder[0];

    public PreviewModelImpl(PreviewController previewController) {
        this.previewController = previewController;
        typeMap = new HashMap<>();
        sourceMap = new HashMap<>();

    }

    private synchronized void initProperties() {
        if (properties == null) {
            properties = new PreviewProperties();

            //Properties from renderers
            for (Renderer renderer : getManagedEnabledRenderers()) {
                PreviewProperty[] props = renderer.getProperties();
                for (PreviewProperty p : props) {
                    properties.addProperty(p);
                }
            }

            //Default preset
            properties.applyPreset(new DefaultPreset());

            //Defaut values
            properties.putValue(PreviewProperty.VISIBILITY_RATIO, 1f);
        }
    }

    @Override
    public PreviewProperties getProperties() {
        initProperties();
        return properties;
    }

    @Override
    public Item[] getItems(String type) {
        List<Item> list = typeMap.get(type);
        if (list != null) {
            return list.toArray(new Item[0]);
        }
        return new Item[0];
    }

    @Override
    public Item getItem(String type, Object source) {
        Item[] items = getItems(source);
        for (Item item : items) {
            if (item.getType().equals(type)) {
                return item;
            }
        }
        return null;
    }

    @Override
    public Item[] getItems(Object source) {
        Object value = sourceMap.get(source);
        if (value instanceof List) {
            return ((List<Item>) value).toArray(new Item[0]);
        } else if (value instanceof Item) {
            return new Item[]{(Item) value};
        }
        return new Item[0];
    }

    public String[] getItemTypes() {
        return typeMap.keySet().toArray(new String[0]);
    }

    public void loadItems(String type, Item[] items) {
        //Add to type map
        List<Item> typeList = typeMap.get(type);
        if (typeList == null) {
            typeList = new ArrayList<>(items.length);
            typeList.addAll(Arrays.asList(items));
            typeMap.put(type, typeList);

            //Add to source map
            for (Item item : items) {
                Object value = sourceMap.get(item.getSource());
                if (value == null) {
                    sourceMap.put(item.getSource(), item);
                } else if (value instanceof List) {
                    ((List) value).add(item);
                }
            }
        } else {
            //Possible items to merge
            for (Item item : items) {
                Object value = sourceMap.get(item.getSource());
                if (value == null) {
                    //No other object attached to this item
                    typeList.add(item);
                    sourceMap.put(item.getSource(), item);
                } else if (value instanceof Item && ((Item) value).getType().equals(item.getType())) {
                    //An object already exists with the same type and source, merge them
                    mergeItems(item, ((Item) value));
                } else if (value instanceof List) {
                    List<Item> list = (List<Item>) value;
                    for (Item itemSameSource : list) {
                        if (itemSameSource.getType().equals(item.getType())) {
                            //An object already exists with the same type and source, merge them
                            mergeItems(item, itemSameSource);
                            break;
                        }
                    }
                }
            }
        }
    }

    private Item mergeItems(Item item, Item toBeMerged) {
        for (String key : toBeMerged.getKeys()) {
            item.setData(key, toBeMerged.getData(key));
        }
        return item;
    }

    public void clear() {
        typeMap.clear();
        sourceMap.clear();
    }

    @Override
    public CanvasSize getGraphicsCanvasSize() {
        float x1 = Float.MAX_VALUE;
        float y1 = Float.MAX_VALUE;
        float x2 = Float.MIN_VALUE;
        float y2 = Float.MIN_VALUE;
        for (Renderer r : getManagedEnabledRenderers()) {
            for (String type : getItemTypes()) {
                for (Item item : getItems(type)) {
                    if (r.isRendererForItem(item, getProperties())) {
                        CanvasSize cs = r.getCanvasSize(item, getProperties());
                        x1 = Math.min(x1, cs.getX());
                        y1 = Math.min(y1, cs.getY());
                        x2 = Math.max(x2, cs.getMaxX());
                        y2 = Math.max(y2, cs.getMaxY());
                    }
                }
            }
        }
        return new CanvasSize(x1, y1, x2 - x1, y2 - y1);
    }

    @Override
    public ManagedRenderer[] getManagedRenderers() {
        return managedRenderers;
    }

    /**
     * Removes unnecessary properties from not enabled renderers
     */
    private void reloadProperties() {
        if (properties == null) {
            initProperties();
        } else {
            PreviewProperties newProperties = new PreviewProperties();//Ensure that the properties object doesn't change

            //Properties from renderers
            for (Renderer renderer : getManagedEnabledRenderers()) {
                PreviewProperty[] props = renderer.getProperties();
                for (PreviewProperty p : props) {
                    newProperties.addProperty(p);
                    if (properties.hasProperty(p.getName())) {
                        newProperties.putValue(p.getName(), properties.getValue(p.getName()));//Keep old values
                    }
                }
            }

            //Remove old properties (this keeps simple values)
            for (PreviewProperty p : properties.getProperties()) {
                properties.removeProperty(p);
            }

            //Set new properties
            for (PreviewProperty property : newProperties.getProperties()) {
                properties.addProperty(property);
            }
        }
    }

    @Override
    public void setManagedRenderers(ManagedRenderer[] managedRenderers) {
        //Validate no null ManagedRenderers
        for (ManagedRenderer managedRenderer : managedRenderers) {
            if (managedRenderer == null) {
                throw new IllegalArgumentException("managedRenderers should not contain null values");
            }
        }

        this.managedRenderers = managedRenderers;
        reloadProperties();
    }

    @Override
    public Renderer[] getManagedEnabledRenderers() {
        if (managedRenderers != null) {
            ArrayList<Renderer> renderers = new ArrayList<>();
            for (ManagedRenderer mr : managedRenderers) {
                if (mr.isEnabled()) {
                    renderers.add(mr.getRenderer());
                }
            }
            return renderers.toArray(new Renderer[0]);
        } else {
            return null;
        }
    }

    @Override
    public void setItemBuilders(ItemBuilder... itemBuilders) {
        this.itemBuilders = Arrays.stream(Optional.ofNullable(itemBuilders).orElse(new ItemBuilder[0])).filter(ib -> ib != null).toArray(ItemBuilder[]::new);
    }

    @Override
    public ItemBuilder[] getItemBuilders() {
        return itemBuilders;
    }

    @Override
    public void setRenderTargetBuilders(RenderTargetBuilder... renderTargetBuilders) {
        this.renderTargetBuilders = Arrays.stream(Optional.ofNullable(renderTargetBuilders).orElse(new RenderTargetBuilder[0])).filter(rtb -> rtb != null).toArray(RenderTargetBuilder[]::new);
    }

    @Override
    public RenderTargetBuilder[] getRenderTargetBuilders() {
        return renderTargetBuilders;
    }
}
