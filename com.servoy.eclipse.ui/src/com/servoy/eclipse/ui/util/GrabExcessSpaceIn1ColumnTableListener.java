/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.ui.util;

import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * A control listener that will resize one of the columns in a Table to fill excess horizontal space.
 *
 * @author acostescu
 */
public class GrabExcessSpaceIn1ColumnTableListener extends ControlAdapter
{
	protected boolean resizingColumn = false;
	protected TableColumn[] columns;
	protected Table table;
	protected final int grabberIndex;

	/**
	 * Creates a new instance. the
	 * @param columns
	 * @param grabberIndex
	 */
	public GrabExcessSpaceIn1ColumnTableListener(Table table, int grabberIndex)
	{
		this.table = table;
		columns = table.getColumns();
		this.grabberIndex = grabberIndex;
	}

	@Override
	public void controlResized(ControlEvent e)
	{
		grabExcessSpaceInColumn();
	}

	public void grabExcessSpaceInColumn()
	{
		if (!resizingColumn) // avoid loops
		{
			resizingColumn = true;
			table.setVisible(false);
			try
			{
				int allOtherColumnWidths = 0;
				for (int i = columns.length - 1; i >= 0; i--)
				{
					columns[i].pack();
					if (i != grabberIndex) allOtherColumnWidths += columns[i].getWidth();
				}

				int w = table.getClientArea().width - allOtherColumnWidths;
				if (w > columns[grabberIndex].getWidth())
				{
					columns[grabberIndex].setWidth(w);
				}
			}
			finally
			{
				table.setVisible(true);
				resizingColumn = false;
			}
		}
	}
}