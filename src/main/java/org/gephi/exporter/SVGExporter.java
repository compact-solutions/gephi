/*
Copyright 2008-2010 Gephi
Authors : Jeremy Subtil <jeremy.subtil@gephi.org>
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
package org.gephi.exporter;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.svg2svg.SVGTranscoder;
import org.gephi.preview.*;
import org.w3c.dom.Document;

import java.io.Writer;

/**
 * Class exporting the preview graph as an SVG image.
 *
 * @author Jérémy Subtil
 */
public class SVGExporter {

    //Architecture
    private Document doc;
    private Writer writer;
    //Settings
    private boolean scaleStrokes = false;
    private double margin = 4;

    public void exportPreview(PreviewController controller) {

        PreviewProperties props = controller.getModel().getProperties();
        props.putValue(SVGTarget.SCALE_STROKES, scaleStrokes);
        props.putValue(PreviewProperty.MARGIN, margin);
        SVGTarget target = (SVGTarget) controller.getRenderTarget(RenderTarget.SVG_TARGET);

        try {
            controller.render(target);

            // creates SVG-to-SVG transcoder
            SVGTranscoder t = new SVGTranscoder();
            t.addTranscodingHint(SVGTranscoder.KEY_XML_DECLARATION, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            t.addTranscodingHint(SVGTranscoder.KEY_FORMAT, SVGTranscoder.VALUE_FORMAT_OFF);
            // sets transcoder input and output
            TranscoderInput input = new TranscoderInput(target.getDocument());

            // performs transcoding
            try {
                TranscoderOutput output = new TranscoderOutput(writer);
                t.transcode(input, output);
            } finally {
                writer.close();
                props.removeSimpleValue(PreviewProperty.MARGIN);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public void setWriter(Writer writer) {
        this.writer = writer;
    }

    public void setScaleStrokes(boolean scaleStrokes) {
        this.scaleStrokes = scaleStrokes;
    }

    public boolean isScaleStrokes() {
        return scaleStrokes;
    }
}
