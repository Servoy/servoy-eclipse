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
package com.servoy.eclipse.ui.editors.table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TreeColumn;

import com.servoy.j2db.persistence.Column;

public class ColumnsSorter extends ViewerSorter
{
	// Simple data structure for grouping
	// sort information by column.
	private class SortInfo
	{
		int columnIndex;
		Comparator comparator;
		boolean descending;
		Item column;
	}

	private final ColumnViewer viewer;
	private final SortInfo[] infos;
	private final boolean[] firstTime = new boolean[] { true };

	public ColumnsSorter(ColumnViewer viewer, Item[] columns, Comparator[] comparators)
	{
		this.viewer = viewer;
		infos = new SortInfo[columns.length];
		for (int i = 0; i < columns.length; i++)
		{
			infos[i] = new SortInfo();
			infos[i].column = columns[i];
			infos[i].columnIndex = i;
			infos[i].comparator = comparators[i];
			infos[i].descending = false;
			createSelectionListener(columns[i], infos[i]);
		}
	}

	@Override
	public int compare(Viewer viewer, Object favorite1, Object favorite2)
	{
		if (firstTime[0]) return 0;

		//for table columns, let newly created columns add up at the end of the list, and allow full sorting only at save time
		if ((favorite1 instanceof Column && !((Column)favorite1).getExistInDB()) && (favorite2 instanceof Column && !((Column)favorite2).getExistInDB()))
		{
			List<Column> columns = new ArrayList<Column>(((Column)favorite1).getTable().getColumns());
			if (columns.indexOf(favorite1) < columns.indexOf(favorite2))
			{
				return -1;
			}
			return 1;
		}
		else if (favorite1 instanceof Column && !((Column)favorite1).getExistInDB())
		{
			return 1;
		}
		else if (favorite2 instanceof Column && !((Column)favorite2).getExistInDB())
		{
			return -1;
		}

		for (SortInfo element : infos)
		{
			int result = 0;
			if (element.comparator != null)
			{
				result = element.comparator.compare(favorite1, favorite2);
			}
			else if (favorite1 instanceof Comparable && favorite2 instanceof Comparable)
			{
				result = ((Comparable)favorite1).compareTo(favorite2);
			}
			if (result != 0)
			{
				if (element.descending) return -result;
				return result;
			}
		}
		return 0;
	}

	private void createSelectionListener(final Item column, final SortInfo info)
	{
		SelectionListener listener = new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				firstTime[0] = false;
				if (viewer instanceof TableViewer)
				{
					((TableViewer)viewer).getTable().setSortColumn((TableColumn)info.column);
				}
				if (viewer instanceof TreeViewer)
				{
					((TreeViewer)viewer).getTree().setSortColumn((TreeColumn)info.column);
				}
				sortUsing(info);
				if (viewer instanceof TableViewer)
				{
					((TableViewer)viewer).getTable().setSortDirection((info.descending ? SWT.UP : SWT.DOWN));
				}
				if (viewer instanceof TreeViewer)
				{
					((TreeViewer)viewer).getTree().setSortDirection((info.descending ? SWT.UP : SWT.DOWN));
				}
			}
		};
		if (viewer instanceof TableViewer)
		{
			((TableColumn)column).addSelectionListener(listener);
		}
		if (viewer instanceof TreeViewer)
		{
			((TreeColumn)column).addSelectionListener(listener);
		}
	}

	protected void sortUsing(SortInfo info)
	{
		if (info == infos[0])
		{
			info.descending = !info.descending;
		}
		else
		{
			for (int i = 0; i < infos.length; i++)
			{
				if (info == infos[i])
				{
					System.arraycopy(infos, 0, infos, 1, i);
					infos[0] = info;
					info.descending = false;
					break;
				}
			}
		}
		viewer.refresh();
	}
}
