/*
GNU Lesser General Public License

ExtendedHTMLEditorKit
Copyright (C) 2001  Frits Jalvingh, Jerry Pommer & Howard Kistler

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package com.hexidec.ekit.component;

import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
//SD: For zooming
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.BlockView;
import javax.swing.text.html.CSS;
import javax.swing.text.html.ParagraphView;
import javax.swing.text.FlowView;
import javax.swing.text.FlowView.FlowStrategy;
import javax.swing.text.Position;
import java.awt.*;
import java.awt.geom.AffineTransform;

import com.hexidec.ekit.component.ExtendedHTMLDocument;
import javax.swing.text.LabelView;
import javax.swing.text.html.InlineView;

/**
  * This class extends HTMLEditorKit so that it can provide other renderer classes
  * instead of the defaults. Most important is the part which renders relative
  * image paths.
  *
  * @author <a href="mailto:jal@grimor.com">Frits Jalvingh</a>
  * @version 1.0
  */

public class ExtendedHTMLEditorKit extends HTMLEditorKit
{
	/** Constructor
	  */
	public ExtendedHTMLEditorKit()
	{
	}

	/** Method for returning a ViewFactory which handles the image rendering.
	  */
	public ViewFactory getViewFactory()
	{
		return new HTMLFactoryExtended();
	}

	public Document createDefaultDocument()
	{
		StyleSheet styles = getStyleSheet();
		StyleSheet ss = new StyleSheet();
		ss.addStyleSheet(styles);
		ExtendedHTMLDocument doc = new ExtendedHTMLDocument(ss);
		doc.setParser(getParser());
		doc.setAsynchronousLoadPriority(4);
		doc.setTokenThreshold(100);
		return doc;
	}

/* Inner Classes --------------------------------------------- */

    public static class HTMLBlockView extends BlockView
    {
        public HTMLBlockView(Element elem)
        {
            super(elem,  View.Y_AXIS);
        }

        @Override
        protected void layout(int width, int height)
        {
            if (width<Integer.MAX_VALUE)
            {
                super.layout(new Double(width / getZoomFactor()).intValue(),
                     new Double(height * getZoomFactor()).intValue());
            }
        }

        public double getZoomFactor()
        {
            Double scale = (Double) getDocument().getProperty("ZOOM_FACTOR");
            if (scale != null)
            {
                return scale.doubleValue();
            }

            return 1;
        }

        @Override
        public void paint(Graphics g, Shape allocation)
        {
            Graphics2D g2d = (Graphics2D) g;
            double zoomFactor = getZoomFactor();
            AffineTransform old = g2d.getTransform();
            g2d.scale(zoomFactor, zoomFactor);
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            super.paint(g2d, allocation);
            g2d.setTransform(old);
        }

        @Override
        public float getMinimumSpan(int axis)
        {
            float f = super.getMinimumSpan(axis);
            f *= getZoomFactor();
            return f;
        }

        @Override
        public float getMaximumSpan(int axis)
        {
            float f = super.getMaximumSpan(axis);
            f *= getZoomFactor();
            return f;
        }

        @Override
        public float getPreferredSpan(int axis)
        {
            float f = super.getPreferredSpan(axis);
            f *= getZoomFactor();
            return f;
        }

        @Override
        public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException
        {
            double zoomFactor = getZoomFactor();
            Rectangle alloc;
            alloc = a.getBounds();
            Shape s = super.modelToView(pos, alloc, b);
            alloc = s.getBounds();
            alloc.x *= zoomFactor;
            alloc.y *= zoomFactor;
            alloc.width *= zoomFactor;
            alloc.height *= zoomFactor;

            return alloc;
        }

        @Override
        public int viewToModel(float x, float y, Shape a,
                               Position.Bias[] bias)
       {
            double zoomFactor = getZoomFactor();
            Rectangle alloc = a.getBounds();
            x /= zoomFactor;
            y /= zoomFactor;
            alloc.x /= zoomFactor;
            alloc.y /= zoomFactor;
            alloc.width /= zoomFactor;
            alloc.height /= zoomFactor;

            return super.viewToModel(x, y, alloc, bias);
        }
    } //end class HTMLBlockView

    public static class HTMLParagraphView extends ParagraphView
    {
        public static int MAX_VIEW_SIZE=100;

        public HTMLParagraphView(Element elem)
        {
            super(elem);
            strategy = new HTMLParagraphView.HTMLFlowStrategy();
        }

        public static class HTMLFlowStrategy extends FlowStrategy
        {
            protected View createView(FlowView fv, int startOffset, int spanLeft, int rowIndex)
            {
                View res=super.createView(fv, startOffset, spanLeft, rowIndex);
                if (res.getEndOffset()-res.getStartOffset()> MAX_VIEW_SIZE)
                {
                    res = res.createFragment(startOffset, startOffset+ MAX_VIEW_SIZE);
                }
                return res;
            }

        }
        public int getResizeWeight(int axis)
        {
            return 0;
        }
    } //end class HTMLParagraphView

	/** Class that replaces the default ViewFactory and supports
	  * the proper rendering of both URL-based and local images.
	  */
	public static class HTMLFactoryExtended extends HTMLFactory implements ViewFactory
	{
		/** Constructor
		  */
		public HTMLFactoryExtended()
		{
		}

		/** Method to handle IMG tags and
		  * invoke the image loader.
		  */
        @Override
		public View create(Element elem)
		{
            AttributeSet attrs = elem.getAttributes();
            Object obj = attrs.getAttribute(StyleConstants.NameAttribute);
			if(obj instanceof HTML.Tag)
			{
				HTML.Tag tagType = (HTML.Tag)obj;
				if(tagType == HTML.Tag.IMG)
				{
					return new RelativeImageView(elem);
				}
			}
            //SD: For zooming
            Object elementName = attrs.getAttribute(AbstractDocument.ElementNameAttribute);
            Object o = (elementName != null) ? null : obj;
            if (o instanceof HTML.Tag)
            {
                HTML.Tag kind = (HTML.Tag) o;
                if (kind == HTML.Tag.CONTENT)
                {
                    return new ScaledInlineView(elem);
                }
                else if (kind == HTML.Tag.HTML)
                {
                    return new HTMLBlockView(elem);
                }
                else if (kind == HTML.Tag.IMPLIED)
                {
                    String ws = (String) elem.getAttributes().getAttribute(CSS.Attribute.WHITE_SPACE);
                    if ((ws != null) && ws.equals("pre"))
                    {
                        return super.create(elem);
                    }
                    return new HTMLParagraphView(elem);
                }
                else if ((kind == HTML.Tag.P) ||
                        (kind == HTML.Tag.H1) ||
                        (kind == HTML.Tag.H2) ||
                        (kind == HTML.Tag.H3) ||
                        (kind == HTML.Tag.H4) ||
                        (kind == HTML.Tag.H5) ||
                        (kind == HTML.Tag.H6) ||
                        (kind == HTML.Tag.DT))
                {
                    // paragraph
                    return new HTMLParagraphView(elem);
                }
            }
			return super.create(elem);
		}
    } //end class HTMLFactoryExtended
}

class ScaledInlineView extends InlineView
{
    static GlyphPainter defaultPainter;

    public ScaledInlineView(Element elem)
    {
        super(elem);
    }

    protected void checkPainter()
    {
        if (getGlyphPainter() == null)
        {
            if (defaultPainter == null)
            {
                defaultPainter = new ScaledGlyphPainter();
            }
            setGlyphPainter(defaultPainter.getPainter(this, getStartOffset(), getEndOffset()));
        }
    }
}
