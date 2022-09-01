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

package com.servoy.eclipse.ui.dialogs.autowizard.nattable;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.nebula.widgets.nattable.data.convert.DisplayConverter;
import org.eclipse.nebula.widgets.nattable.edit.gui.AbstractDialogCellEditor;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer.MoveDirectionEnum;
import org.eclipse.swt.SWT;

import com.servoy.eclipse.ui.dialogs.LeafnodesSelectionFilter;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;

/**
 * @author emera
 */
public class NatListDialogCellEditor extends AbstractDialogCellEditor
{
	private Object input;
	private final String name;
	private final int treeStyle;
	private final ITreeContentProvider contentProvider;
	private IFilter selectionFilter = null;
	private boolean showFilterMenu = false;
	private int defaultFilterMode = TreePatternFilter.FILTER_LEAFS;
	private final ILabelProvider dialogLabelProvider;
	private Object canonicalValue;
	private final String title;
	private boolean closed;
	private Object selected;

	public NatListDialogCellEditor(String title, ITreeContentProvider contentProvider, ILabelProvider labelProvider, Object input, int treeStyle,
		String name)
	{
		this.title = title;
		this.name = name;
		this.contentProvider = contentProvider;
		this.input = input;
		this.treeStyle = treeStyle;
		this.dialogLabelProvider = labelProvider;
	}

	public void setDisplayConverter(DisplayConverter displayConverter)
	{
		this.displayConverter = displayConverter;
	}

	public void setInput(Object input)
	{
		this.input = input;
	}

	public void setShowFilterMenu(boolean showFilterMenu)
	{
		this.showFilterMenu = showFilterMenu;
	}

	public void setDefaultFilterMode(int defaultFilterMode)
	{
		this.defaultFilterMode = defaultFilterMode;
	}

	public void setSelectionFilter(IFilter selectionFilter)
	{
		this.selectionFilter = selectionFilter;
	}

	public IFilter getSelectionFilter()
	{
		if (selectionFilter == null)
		{
			selectionFilter = new LeafnodesSelectionFilter(contentProvider);
		}
		return selectionFilter;
	}

	protected StructuredSelection getSelection()
	{
		if (selected == null) return StructuredSelection.EMPTY;
		if (selected instanceof Object[])
		{
			return new StructuredSelection((Object[])selected);
		}
		return new StructuredSelection(
			new Object[] { selected });
	}

	@Override
	public Object getCanonicalValue()
	{
		return canonicalValue;
	}

	@Override
	public int open()
	{
		TreeSelectDialog dialogInstance = createDialogInstance();
		int open = dialogInstance.open();
		this.selected = ((StructuredSelection)dialogInstance.getSelection()).getFirstElement();
		if (open == Window.OK)
		{
			canonicalValue = getCanonicalValue(selected);
			commit(MoveDirectionEnum.NONE);
		}
		this.closed = true;
		return open;
	}

	protected Object getCanonicalValue(Object firstElement)
	{
		return firstElement;
	}

	@Override
	public TreeSelectDialog createDialogInstance()
	{
		this.closed = false;
		boolean allowEmptySelection = false;
		if ((treeStyle & SWT.MULTI) != 0) allowEmptySelection = true;
		return new TreeSelectDialog(this.parent.getShell(), true, showFilterMenu, defaultFilterMode, contentProvider,
			dialogLabelProvider, null, getSelectionFilter(), treeStyle, title, input, getSelection(), allowEmptySelection, name, null, false);
	}

	@Override
	public TreeSelectDialog getDialogInstance()
	{
		return (TreeSelectDialog)this.dialog;
	}

	@Override
	public Object getEditorValue()
	{
		return this.selected;
	}

	@Override
	public void setEditorValue(Object value)
	{
		this.selected = value;
	}

	@Override
	public void close()
	{
	}

	@Override
	public boolean isClosed()
	{
		return closed;
	}
}
