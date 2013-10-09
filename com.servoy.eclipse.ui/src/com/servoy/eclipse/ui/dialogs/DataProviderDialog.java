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


import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderContentProvider;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderNodeWrapper;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewVariableAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewVariableAction.VariableEditDialog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ScriptVariable;
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
	private final PersistContext persistContext;
	private final ILabelProvider labelProvider;
	private ITreeContentProvider contentProvider = null;

	private Button createVarButton;

	/**
	 * Creates a new dataprovider dialog editor parented under the given shell.
	 * 
	 * @param parent the parent control
	 */
	public DataProviderDialog(Shell shell, ILabelProvider labelProvider, PersistContext persistContext, FlattenedSolution flattenedSolution, Table table,
		DataProviderOptions input, ISelection selection, int treeStyle, String title)
	{
		super(shell, true, true, TreePatternFilter.FILTER_LEAFS, null, null, null, null, treeStyle, title, null, selection, false,
			TreeSelectDialog.DATAPROVIDER_DIALOG, null);
		this.labelProvider = labelProvider;
		this.persistContext = persistContext;
		this.flattenedSolution = flattenedSolution;
		this.table = table;
		this.input = input;
		this.treeStyle = treeStyle;
	}

	@Override
	protected FilteredTreeViewer createFilteredTreeViewer(Composite parent)
	{
		DataProviderTreeViewer dataProviderTreeViewer = new DataProviderTreeViewer(parent, labelProvider, contentProvider != null ? contentProvider
			: new DataProviderContentProvider(persistContext, flattenedSolution, table), input, true, true, TreePatternFilter.getSavedFilterMode(
			getDialogBoundsSettings(), TreePatternFilter.FILTER_LEAFS), treeStyle);
		return dataProviderTreeViewer;
	}


	public IDialogSettings getDataProvideDialogSettings()
	{
		return this.getDialogBoundsSettings();
	}

	public void setContentProvider(ITreeContentProvider contentProvider)
	{
		this.contentProvider = contentProvider;
	}

	@Override
	public void refreshTree()
	{
		getTreeViewer().setInput(getTreeViewer().getInput());
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent)
	{
		createVarButton = createButton(parent, IDialogConstants.PROCEED_ID, "Create Variable", false);
		super.createButtonsForButtonBar(parent);
	}

	@Override
	protected void updateButtons()
	{
		super.updateButtons();
		if (createVarButton != null)
		{
			boolean enabledState = false;
			IStructuredSelection selection = (IStructuredSelection)getTreeViewer().treeViewer.getSelection();
			Object o = selection.getFirstElement(); // single selection is allowed in this dialog, therefore selection will contain only one element
			if (o instanceof DataProviderNodeWrapper)
			{
				if (((DataProviderNodeWrapper)o).node.equals(DataProviderTreeViewer.FORM_VARIABLES) || ((DataProviderNodeWrapper)o).scope != null)
				{
					enabledState = true;
				}
			}
			createVarButton.setEnabled(enabledState);
		}
	}

	@Override
	protected void buttonPressed(int buttonId)
	{
		super.buttonPressed(buttonId);
		if (buttonId == IDialogConstants.PROCEED_ID) createVarButtonPressed();
	}

	protected void createVarButtonPressed()
	{
		IStructuredSelection selection = (IStructuredSelection)getTreeViewer().treeViewer.getSelection();
		Object o = selection.getFirstElement();
		if (o instanceof DataProviderNodeWrapper)
		{
			String variableScopeType = null;
			Object validationContext = null;
			IPersist parent = null;
			String scopeName = null;
			if (((DataProviderNodeWrapper)o).node.equals(DataProviderTreeViewer.FORM_VARIABLES))
			{
				variableScopeType = "form";
				parent = persistContext.getPersist();
				validationContext = parent;
			}
			else if (((DataProviderNodeWrapper)o).scope != null)
			{
				variableScopeType = "global";
				validationContext = scopeName = ((DataProviderNodeWrapper)o).scope.getName();
				parent = flattenedSolution.getSolution();
			}

			VariableEditDialog askUserDialog = NewVariableAction.showVariableEditDialog(getTreeViewer().getShell(), validationContext, variableScopeType);
			String variableName = askUserDialog.getVariableName();
			int variableType = askUserDialog.getVariableType();
			String defaultValue = askUserDialog.getVariableDefaultValue();

			if (variableName != null)
			{
				ScriptVariable variable = NewVariableAction.createNewVariable(getTreeViewer().getShell(), parent, scopeName, variableName, variableType,
					defaultValue, false);
				getTreeViewer().treeViewer.refresh();
				getTreeViewer().setSelection(new StructuredSelection(variable));
			}
		}
	}
}
