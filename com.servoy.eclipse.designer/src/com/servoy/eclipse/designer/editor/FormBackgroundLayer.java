/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */
package com.servoy.eclipse.designer.editor;

import java.awt.Dimension;
import java.awt.print.PageFormat;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.swing.text.Style;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.resource.ColorResource;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.util.FixedStyleSheet;
import com.servoy.j2db.util.PersistHelper;

/**
 * Layer to print form background stuff like data-renderers and page-breaks.
 * 
 * @author rgansevles
 * 
 */
public class FormBackgroundLayer extends FreeformLayer
{
	private static final Color TRANSPARENT_PATTERN_EVEN = ColorResource.INSTANCE.getColor(new RGB(0xf0, 0xf0, 0xf0));
	private static final Color TRANSPARENT_PATTERN_ODD = ColorConstants.white;

	/**
	 * A viewer property indicating whether page breaks should be painted in the form editor. The value must  be a Boolean.
	 */
	public static final String PROPERTY_PAINT_PAGEBREAKS = "FormBackground.paintPageBreaks"; //$NON-NLS-1$


	public static final int TRANSPARENT_PATTERN_SIZE = 14;


	protected final VisualFormEditor editorPart;

	public FormBackgroundLayer(VisualFormEditor editorPart)
	{
		this.editorPart = editorPart;
	}

	/**
	 * @see org.eclipse.draw2d.Figure#paintFigure(org.eclipse.draw2d.Graphics)
	 */
	@Override
	protected void paintFigure(Graphics graphics)
	{
		super.paintFigure(graphics);

		if (editorPart.getForm() == null) return;
		Form flattenedForm = ModelUtils.getEditingFlattenedSolution(editorPart.getForm()).getFlattenedForm(editorPart.getForm());
		if (flattenedForm.getTransparent())
		{
			paintTransparencyFormPattern(graphics, flattenedForm);
		}
		else
		{
			paintDatarenderers(graphics, flattenedForm);
		}
		paintPagebreaks(graphics);
	}

	/**
	 * Paint data-renderers.
	 * <p>
	 * graphics state is expected to be saved by caller.
	 * 
	 * @param graphics
	 */
	protected void paintDatarenderers(Graphics graphics, Form flattenedForm)
	{
		Iterator<Part> parts = flattenedForm.getParts();
		int prevY = 0;
		Color formBg = null;
		while (parts.hasNext())
		{
			Part part = parts.next();
			Color bg;
			if (part.getBackground() == null)
			{
				if (formBg == null)
				{
					formBg = ColorResource.INSTANCE.getColor(ColorResource.ColorAwt2Rgb(getFormBackground(flattenedForm)));
				}
				bg = formBg;
			}
			else
			{
				bg = ColorResource.INSTANCE.getColor(ColorResource.ColorAwt2Rgb(part.getBackground()));
			}

			graphics.setBackgroundColor(bg);
			graphics.fillRectangle(0, prevY, flattenedForm.getWidth(), part.getHeight() - prevY);
			prevY = part.getHeight();
		}
	}

	/**
	 * Paint a block-pattern to indicate the dimensions of the transparent form.
	 * @param graphics
	 * @param flattenedForm
	 */
	protected void paintTransparencyFormPattern(Graphics graphics, Form flattenedForm)
	{
		Dimension size = flattenedForm.getSize();
		paintTransparencypattern(graphics, true, size);
		paintTransparencypattern(graphics, false, size);
	}

	private void paintTransparencypattern(Graphics graphics, boolean even, Dimension size)
	{
		graphics.setBackgroundColor(even ? TRANSPARENT_PATTERN_EVEN : TRANSPARENT_PATTERN_ODD);
		for (int y = 0; y < size.height; y += TRANSPARENT_PATTERN_SIZE)
		{
			for (int x = (even == ((y / TRANSPARENT_PATTERN_SIZE) % 2 == 0)) ? TRANSPARENT_PATTERN_SIZE : 0; x < size.width; x += 2 * TRANSPARENT_PATTERN_SIZE)
			{
				graphics.fillRectangle(x, y, Math.min(TRANSPARENT_PATTERN_SIZE, size.width - x), Math.min(TRANSPARENT_PATTERN_SIZE, size.height - y));
			}
		}
	}

	/**
	 * get background based on form style
	 * 
	 * @param form
	 * @return
	 */
	public static java.awt.Color getFormBackground(Form form)
	{
		FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(form);
		FixedStyleSheet styleSheet = ComponentFactory.getCSSStyle(flattenedSolution.getStyleForForm(form, null));
		java.awt.Color background = null;
		if (styleSheet != null)
		{
			String lookupname = "form";
			if (form.getStyleClass() != null && !"".equals(form.getStyleClass()))
			{
				lookupname += '.' + form.getStyleClass();
			}
			Style style = styleSheet.getStyle(lookupname);
			if (style != null)
			{
				background = styleSheet.getBackground(style);
			}
		}

		if (background == null)
		{
			return java.awt.Color.white;
		}
		return background;
	}

	/**
	 * Paint page-breaks.
	 * <p>
	 * graphics state is expected to be saved by caller.
	 * 
	 * @param graphics
	 */
	protected void paintPagebreaks(Graphics graphics)
	{
		if (!Boolean.TRUE.equals(((GraphicalViewer)editorPart.getAdapter(GraphicalViewer.class)).getProperty(PROPERTY_PAINT_PAGEBREAKS)))
		{
			return;
		}

		Form flattenedForm = ModelUtils.getEditingFlattenedSolution(editorPart.getForm()).getFlattenedForm(editorPart.getForm());
		if (flattenedForm == null) return;

		String defaultPageFormat = flattenedForm.getDefaultPageFormat();
		PageFormat currentPageFormat = null;
		try
		{
			currentPageFormat = PersistHelper.createPageFormat(defaultPageFormat);
		}
		catch (NoSuchElementException e)
		{
			ServoyLog.logWarning("Could not parse page format '" + defaultPageFormat + '\'', null);
		}
		if (currentPageFormat == null)
		{
			currentPageFormat = Activator.getDefault().getDesignClient().getPageFormat();
		}

		int w = (int)(currentPageFormat.getImageableWidth() * (flattenedForm.getPaperPrintScale() / 100d));
		int h = (int)(currentPageFormat.getImageableHeight() * (flattenedForm.getPaperPrintScale() / 100d));

		graphics.setForegroundColor(ColorConstants.white);
		graphics.setLineStyle(SWT.LINE_DASH);
		FigureUtilities.paintGrid(graphics, this, new Point(), w, h);
	}
}
