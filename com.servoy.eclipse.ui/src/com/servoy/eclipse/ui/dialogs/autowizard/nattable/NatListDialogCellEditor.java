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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.ui.dialogs.LeafnodesSelectionFilter;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;

/**
 * @author emera
 */
public abstract class NatListDialogCellEditor extends NatTextDialogCellEditor
{
	private Object input;
	private final String name;
	private final int treeStyle;
	private final ITreeContentProvider contentProvider;
	private IFilter selectionFilter = null;
	private boolean showFilterMenu = false;
	private int defaultFilterMode = TreePatternFilter.FILTER_LEAFS;
	private final ILabelProvider dialogLabelProvider;

	public NatListDialogCellEditor(String title, Image icon, ITreeContentProvider contentProvider, ILabelProvider labelProvider, Object input, int treeStyle,
		String name)
	{
		super(title, icon, labelProvider);
		this.name = name;
		this.contentProvider = contentProvider;
		this.input = input;
		this.treeStyle = treeStyle;
		this.dialogLabelProvider = labelProvider;
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
		if (canonicalValue == null) return StructuredSelection.EMPTY;
		if (canonicalValue instanceof Object[])
		{
			return new StructuredSelection((Object[])canonicalValue);
		}
		return new StructuredSelection(
			new Object[] { getDisplayConverter() != null ? getDisplayConverter().canonicalToDisplayValue(canonicalValue) : canonicalValue });
	}

	@Override
	public Object getCanonicalValue()
	{
		return canonicalValue;
	}

	@Override
	public void openDialog(Shell shell, String value)
	{
		boolean allowEmptySelection = false;
		if ((treeStyle & SWT.MULTI) != 0) allowEmptySelection = true;

		TreeSelectDialog dialog = new TreeSelectDialog(shell, true, showFilterMenu, defaultFilterMode, contentProvider,
			dialogLabelProvider, null, getSelectionFilter(), treeStyle, title, input, getSelection(), allowEmptySelection, name, null, false);
		dialog.open();

		if (dialog.getReturnCode() != Window.CANCEL)
		{
			Object firstElement = ((IStructuredSelection)dialog.getSelection()).getFirstElement();
			setEditorValue(firstElement);
			canonicalValue = getCanonicalValue(firstElement);
		}
	}
}
