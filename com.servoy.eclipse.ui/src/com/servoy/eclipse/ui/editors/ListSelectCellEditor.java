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
package com.servoy.eclipse.ui.editors;


import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.servoy.eclipse.ui.dialogs.FlatTreeContentProvider;
import com.servoy.eclipse.ui.dialogs.LeafnodesSelectionFilter;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.util.IControlFactory;

/**
 * A cell editor that manages a field.
 * <p>
 * This class may be instantiated; it is may be subclassed.
 * </p>
 * 
 * @author rgansevles
 */
public class ListSelectCellEditor extends DialogCellEditor
{
	private final Object input;
	private final String title;
	private final String name;
	private final int treeStyle;
	private final ITreeContentProvider contentProvider;
	private IFilter selectionFilter = null;
	private final ListSelectControlFactory controlFactory;
	private boolean showFilterMenu = false;
	private int defaultFilterMode = TreePatternFilter.FILTER_LEAFS;
	private final String optionalMessage;

	/**
	 * Creates a new list cell editor parented under the given control. Use this constructor for selecting from a list of values
	 * 
	 * @param parent the parent control
	 */
	public ListSelectCellEditor(Composite parent, String title, ILabelProvider labelProvider, IValueEditor< ? > valueEditor, boolean readOnly, Object[] values,
		int treeStyle, ListSelectControlFactory controlFactory, String name)
	{
		this(parent, title, FlatTreeContentProvider.INSTANCE, labelProvider, valueEditor, readOnly, values, treeStyle, controlFactory, name, null);
	}

	/**
	 * Creates a new list cell editor parented under the given control. Use this constructor when the content provider builds the list from the input (in
	 * getElements)
	 * 
	 * @param parent the parent control
	 */
	public ListSelectCellEditor(Composite parent, String title, ITreeContentProvider contentProvider, ILabelProvider labelProvider,
		IValueEditor< ? > valueEditor, boolean readOnly, Object input, int treeStyle, ListSelectControlFactory controlFactory, String name)
	{
		this(parent, title, contentProvider, labelProvider, valueEditor, readOnly, input, treeStyle, controlFactory, name, null);
	}


	/**
	 * Creates a new list cell editor parented under the given control. Use this constructor when the content provider builds the list from the input (in
	 * getElements)
	 * 
	 * @param parent the parent control
	 */
	public ListSelectCellEditor(Composite parent, String title, ITreeContentProvider contentProvider, ILabelProvider labelProvider,
		IValueEditor< ? > valueEditor, boolean readOnly, Object input, int treeStyle, ListSelectControlFactory controlFactory, String name,
		String optionalMessage)
	{
		super(parent, labelProvider, valueEditor, readOnly, SWT.NONE);
		this.title = title;
		this.name = name;
		this.contentProvider = contentProvider;
		this.input = input;
		this.treeStyle = treeStyle;
		this.controlFactory = controlFactory;
		this.optionalMessage = optionalMessage;
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

	@Override
	protected Object openDialogBox(Control cellEditorWindow)
	{
		boolean showFilter = (treeStyle & SWT.CHECK) == 0; // filter does not work correctly with CheckboxTreeViewer // TODO : fix

		boolean allowEmptySelection = false;
		if ((treeStyle & SWT.MULTI) != 0) allowEmptySelection = true;

		TreeSelectDialog dialog = new TreeSelectDialog(cellEditorWindow.getShell(), showFilter, showFilterMenu, defaultFilterMode, contentProvider,
			getLabelProvider(), null, getSelectionFilter(), treeStyle, title, input, getSelection(), allowEmptySelection, name, null);
		dialog.setOptionalMessage(optionalMessage);
		if (controlFactory != null)
		{
			controlFactory.setTreeSelectDialog(dialog);
			dialog.setOptionsAreaFactory(controlFactory);
		}
		dialog.open();

		if (dialog.getReturnCode() == Window.CANCEL)
		{
			return null;
		}
		if ((treeStyle & SWT.MULTI) == 0)
		{
			return ((IStructuredSelection)dialog.getSelection()).getFirstElement(); // single select
		}
		return ((IStructuredSelection)dialog.getSelection()).toArray(); // multi select
	}

	public interface ListSelectControlFactory extends IControlFactory
	{
		void setTreeSelectDialog(TreeSelectDialog dialog);
	}
}
