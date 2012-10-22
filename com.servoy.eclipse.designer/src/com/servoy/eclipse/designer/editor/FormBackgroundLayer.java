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
import java.util.NoSuchElementException;

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
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.util.PersistHelper;

/**
 * Layer to print transparency background and page-breaks.
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


	protected final BaseVisualFormEditor editorPart;

	public FormBackgroundLayer(BaseVisualFormEditor editorPart)
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

		if (editorPart.getForm() == null || editorPart.isClosing()) return;

		if (ModelUtils.getEditingFlattenedSolution(editorPart.getForm()) != null) paintTransparencyFormPattern(graphics,
			ModelUtils.getEditingFlattenedSolution(editorPart.getForm()).getFlattenedForm(editorPart.getForm()).getSize());
		paintPagebreaks(graphics);
	}

	/**
	 * Paint a block-pattern to indicate the dimensions of the transparent form.
	 * @param graphics
	 * @param flattenedForm
	 */
	protected void paintTransparencyFormPattern(Graphics graphics, Dimension size)
	{
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
