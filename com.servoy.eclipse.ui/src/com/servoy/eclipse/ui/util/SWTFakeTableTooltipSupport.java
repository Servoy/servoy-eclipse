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

package com.servoy.eclipse.ui.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * @author lvostinar
 *
 */
public class SWTFakeTableTooltipSupport
{
	public static void enableTooltipFor(Table table, String tableTooltip, String[] columnTooltips)
	{
		// Disable native tooltip
		table.setToolTipText("");

		// Implement a "fake" tooltip
		final Listener labelListener = new Listener()
		{
			public void handleEvent(Event event)
			{
				Label label = (Label)event.widget;
				Shell shell = label.getShell();
				switch (event.type)
				{
					case SWT.MouseDown :
						Event e = new Event();
						e.item = (TableItem)label.getData("_TABLEITEM");
						// Assuming table is single select, set the selection as if
						// the mouse down event went through to the table
						table.setSelection(new TableItem[] { (TableItem)e.item });
						table.notifyListeners(SWT.Selection, e);
						// fall through
					case SWT.MouseExit :
						shell.dispose();
						break;
				}
			}
		};

		Listener tableListener = new Listener()
		{
			Shell tip = null;

			Label label = null;

			public void handleEvent(Event event)
			{
				switch (event.type)
				{
					case SWT.Dispose :
					case SWT.KeyDown :
					case SWT.MouseMove :
					{
						if (tip == null)
							break;
						tip.dispose();
						tip = null;
						label = null;
						break;
					}
					case SWT.MouseHover :
					{
						Point point = new Point(event.x, event.y);
						TableItem item = table.getItem(point);
						if (item != null)
						{
							int column = 0;
							for (int i = 0; i < table.getColumnCount(); i++)
							{
								if (item.getBounds(i).contains(point))
								{
									column = i;
									break;
								}
							}
							if (tip != null && !tip.isDisposed())
								tip.dispose();
							tip = new Shell(table.getShell(), SWT.ON_TOP | SWT.TOOL);
							tip.setLayout(new FillLayout());
							label = new Label(tip, SWT.NONE);
							label.setForeground(table.getDisplay()
								.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
							label.setBackground(table.getDisplay()
								.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
							label.setData("_TABLEITEM", item);
							if (column >= 0 && columnTooltips[column] != null)
							{
								label.setText(columnTooltips[column]);
							}
							else if (tableTooltip != null)
							{
								label.setText(tableTooltip);
							}
							label.addListener(SWT.MouseExit, labelListener);
							label.addListener(SWT.MouseDown, labelListener);
							Point size = tip.computeSize(SWT.DEFAULT, SWT.DEFAULT);
							//Rectangle rect = item.getBounds(column);
							Point pt = table.toDisplay(event.x, event.y);
							tip.setBounds(pt.x + 10, pt.y + 10, size.x, size.y);
							tip.setVisible(true);
						}
					}
				}
			}
		};
		table.addListener(SWT.Dispose, tableListener);
		table.addListener(SWT.KeyDown, tableListener);
		table.addListener(SWT.MouseMove, tableListener);
		table.addListener(SWT.MouseHover, tableListener);
	}
}
