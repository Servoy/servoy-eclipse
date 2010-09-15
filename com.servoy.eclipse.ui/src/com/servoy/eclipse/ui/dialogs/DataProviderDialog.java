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
package com.servoy.eclipse.ui.dialogs;


import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderContentProvider;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Table;

/**
 * A cell editor that manages a dataprovider field.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public class DataProviderDialog extends TreeSelectDialog
{
	private final Table table;
	private final int treeStyle;
	private final FlattenedSolution flattenedSolution;
	private final DataProviderOptions input;
	private final IPersist persist;
	private final ILabelProvider labelProvider;

	/**
	 * Creates a new dataprovider dialog editor parented under the given shell.
	 * 
	 * @param parent the parent control
	 */
	public DataProviderDialog(Shell shell, ILabelProvider labelProvider, IPersist persist, FlattenedSolution flattenedSolution, Table table,
		DataProviderOptions input, ISelection selection, int treeStyle, String title)
	{
		super(shell, true, true, TreePatternFilter.FILTER_LEAFS, null, null, null, null, treeStyle, title, null, selection,
			TreeSelectDialog.DATAPROVIDER_DIALOG, null);
		this.labelProvider = labelProvider;
		this.persist = persist;
		this.flattenedSolution = flattenedSolution;
		this.table = table;
		this.input = input;
		this.treeStyle = treeStyle;
	}

	@Override
	protected FilteredTreeViewer createFilteredTreeViewer(Composite parent)
	{
		DataProviderTreeViewer dataProviderTreeViewer = new DataProviderTreeViewer(parent, labelProvider, new DataProviderContentProvider(persist,
			flattenedSolution, table), input, true, true, TreePatternFilter.getSavedFilterMode(getDialogBoundsSettings(), TreePatternFilter.FILTER_LEAFS),
			treeStyle);
		return dataProviderTreeViewer;
	}


	public IDialogSettings getDataProvideDialogSettings()
	{
		return this.getDialogBoundsSettings();
	}

}
