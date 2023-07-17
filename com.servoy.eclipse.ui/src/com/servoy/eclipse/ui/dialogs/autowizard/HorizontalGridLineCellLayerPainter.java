/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

package com.servoy.eclipse.ui.dialogs.autowizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.LabelStack;
import org.eclipse.nebula.widgets.nattable.painter.layer.GridLineCellLayerPainter;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

/**
 * GridLine cell layer painter which prints only the horizontal lines.
 * @author emera
 */
public class HorizontalGridLineCellLayerPainter extends GridLineCellLayerPainter
{
	@Override
	public void paintLayer(ILayer natLayer, GC gc, int xOffset, int yOffset,
		Rectangle rectangle, IConfigRegistry configRegistry)
	{
		LabelStack stack = natLayer.getRegionLabelsByXY(xOffset, yOffset);
		List<String> labels = new ArrayList<>();
		if (stack != null)
		{
			labels = stack;
		}

		// check if there is a configuration for the grid line width
		Integer width = configRegistry.getConfigAttribute(
			CellConfigAttributes.GRID_LINE_WIDTH,
			DisplayMode.NORMAL,
			labels);
		this.gridLineWidth = (width != null) ? width : 1;

		int oldLineWidth = gc.getLineWidth();
		gc.setLineWidth(this.gridLineWidth);
		drawGridLines(natLayer, gc, rectangle, configRegistry, labels);
		gc.setLineWidth(oldLineWidth);

		super.paintLayer(natLayer, gc, xOffset, yOffset, rectangle, configRegistry);
	}

	@Override
	protected void drawGridLines(ILayer natLayer, GC gc, Rectangle rectangle, IConfigRegistry configRegistry, List<String> labels)
	{
//		if (natLayer instanceof DataLayer)
//		{
		Color gColor = configRegistry.getConfigAttribute(
			CellConfigAttributes.GRID_LINE_COLOR,
			DisplayMode.NORMAL,
			labels);
		gc.setForeground(gColor != null ? gColor : getGridColor());
		int adjustment = (this.gridLineWidth == 1) ? 1 : Math.round(this.gridLineWidth.floatValue() / 2);
		drawHorizontalLines(natLayer, gc, rectangle, adjustment);
//		}
	}

	private void drawHorizontalLines(ILayer natLayer, GC gc, Rectangle rectangle, int adjustment)
	{
		int endX = rectangle.x + Math.min(natLayer.getWidth() - adjustment, rectangle.width);

		// this can happen on resizing if there is no CompositeLayer involved
		// without this check grid line fragments may be rendered below the last
		// column
		if (endX > natLayer.getWidth())
			return;

		int rowPositionByY = natLayer.getRowPositionByY(rectangle.y + rectangle.height);
		int maxRowPosition = rowPositionByY > 0
			? Math.min(natLayer.getRowCount(), rowPositionByY)
			: natLayer.getRowCount();
		for (int rowPosition = natLayer.getRowPositionByY(rectangle.y); rowPosition < maxRowPosition; rowPosition++)
		{
			final int size = natLayer.getRowHeightByPosition(rowPosition);
			if (size > 0)
			{
				int y = natLayer.getStartYOfRowPosition(rowPosition) + size - adjustment;

				gc.drawLine(rectangle.x, y, endX, y);
			}
		}
	}
}