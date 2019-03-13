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

import org.gephi.model.Graph;

/**
 * @author Mathieu Bastian
 */
public class PreviewControllerImpl implements PreviewController {

    private PreviewModelImpl model = new PreviewModelImpl(this);

    @Override
    public synchronized void refreshPreview(Graph graph) {
        PreviewModelImpl previewModel = getModel();
        previewModel.clear();

        //Directed graph?
        previewModel.getProperties().putValue(PreviewProperty.DIRECTED, graph.isDirected());

        Renderer[] renderers = model.getManagedEnabledRenderers();

        //Build items
        for (ItemBuilder b : getModel().getItemBuilders()) {
            //Only build items of this builder if some renderer needs it:
            if (isItemBuilderNeeded(b, previewModel.getProperties(), renderers)) {
                try {
                    Item[] items = b.getItems(graph);
                    if (items != null) {
                        previewModel.loadItems(b.getType(), items);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        //Pre process renderers
        for (Renderer r : renderers) {
            r.preProcess(model);
        }
    }

    private boolean isItemBuilderNeeded(ItemBuilder itemBuilder, PreviewProperties properties, Renderer[] renderers) {
        for (Renderer r : renderers) {
            if (r.needsItemBuilder(itemBuilder, properties)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void render(RenderTarget target) {
        PreviewModelImpl m = getModel();
        render(target, m.getManagedEnabledRenderers(), m);
    }

    @Override
    public void render(RenderTarget target, Renderer[] renderers) {
        render(target, renderers, getModel());
    }

    private synchronized void render(RenderTarget target, Renderer[] renderers, PreviewModelImpl previewModel) {
        if (previewModel != null) {
            PreviewProperties properties = previewModel.getProperties();

            //Render items
            for (Renderer r : renderers) {
                for (String type : previewModel.getItemTypes()) {
                    for (Item item : previewModel.getItems(type)) {
                        if (r.isRendererForItem(item, properties)) {
                            r.render(item, target, properties);
                        }
                    }
                }
            }
        }
    }

    @Override
    public synchronized PreviewModelImpl getModel() {
        return model;
    }

    @Override
    public RenderTarget getRenderTarget(String name) {
        return getRenderTarget(name, getModel());
    }

    private synchronized RenderTarget getRenderTarget(String name, PreviewModel m) {
        if (m != null) {
            for (RenderTargetBuilder rtb : getModel().getRenderTargetBuilders()) {
                if (rtb.getName().equals(name)) {
                    return rtb.buildRenderTarget(m);
                }
            }
        }
        return null;
    }
}
