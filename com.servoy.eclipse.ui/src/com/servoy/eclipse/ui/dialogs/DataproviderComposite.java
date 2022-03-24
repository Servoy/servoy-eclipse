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

package com.servoy.eclipse.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderContentProvider;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions;
import com.servoy.eclipse.ui.labelproviders.DataProviderLabelProvider;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.util.Pair;

/**
 * @author emera
 */
public class DataproviderComposite extends Composite
{
	private final DataProviderTreeViewer dataproviderTreeViewer;
	private final WizardConfigurationViewer tableViewer;
	private final List<Pair<IDataProvider, Object>> input = new ArrayList<>();
	private final IDialogSettings settings;

	public DataproviderComposite(final Composite parent, PersistContext persistContext, FlattenedSolution flattenedSolution, ITable table,
		DataProviderOptions dataproviderOptions, final IDialogSettings settings)
	{
		super(parent, SWT.None);
		this.settings = settings;

		this.setLayout(new FillLayout());
		SashForm form = new SashForm(this, SWT.HORIZONTAL);
		form.setLayout(new FillLayout());

		dataproviderTreeViewer = createDataproviderTree(form, persistContext, flattenedSolution, table, dataproviderOptions);

		tableViewer = createTableViewer(form);
		tableViewer.setInput(input);
	}


	private WizardConfigurationViewer createTableViewer(SashForm form)
	{
		final Composite container = new Composite(form, SWT.NONE);
		// define layout for the viewer

		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 1;
		gridData.verticalSpan = 1;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.minimumWidth = 250;

		container.setLayoutData(gridData);

		final WizardConfigurationViewer viewer = new WizardConfigurationViewer(container,
			SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		return viewer;
	}

	private DataProviderTreeViewer createDataproviderTree(SashForm form, PersistContext persistContext, FlattenedSolution flattenedSolution, ITable table,
		DataProviderOptions dataproviderOptions)
	{
		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 1;
		gridData.verticalSpan = 1;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.minimumWidth = 400;
		gridData.heightHint = 600;

		Composite parent = new Composite(form, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.marginRight = 5;
		parent.setLayout(layout);
		parent.setLayoutData(gridData);

		Composite confAndDelete = new Composite(parent, SWT.NONE);
		GridLayout confAndDeleteLayout = new GridLayout(3, false);
		confAndDeleteLayout.marginHeight = 0;
		confAndDeleteLayout.marginWidth = 0;
		confAndDelete.setLayout(confAndDeleteLayout);
		confAndDelete.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

		DataProviderTreeViewer treeviewer = new DataProviderTreeViewer(parent, DataProviderLabelProvider.INSTANCE_HIDEPREFIX, // label provider will be overwritten when superform is known
			new DataProviderContentProvider(persistContext, flattenedSolution, table), dataproviderOptions, true, true,
			TreePatternFilter.getSavedFilterMode(settings, TreePatternFilter.FILTER_LEAFS), SWT.MULTI);
		treeviewer.setLayoutData(gridData);
		treeviewer.addSelectionChangedListener(new ISelectionChangedListener()
		{
			@Override
			public void selectionChanged(SelectionChangedEvent event)
			{
				moveDataproviderSelection();
			}
		});

		treeviewer.getViewer().getTree().setToolTipText("Select the dataprovders for which you want to place fields");

		treeviewer.addOpenListener(new IOpenListener()
		{
			@Override
			public void open(OpenEvent event)
			{
				moveDataproviderSelection();
			}
		});

		return treeviewer;
	}

	private void moveDataproviderSelection()
	{
		IDataProvider dataprovider = (IDataProvider)((StructuredSelection)dataproviderTreeViewer.getSelection()).getFirstElement();
		if (dataprovider != null)
		{
			Pair<IDataProvider, Object> row = new Pair<>(dataprovider, Column.getDisplayTypeString(dataprovider.getDataProviderType()));
			input.add(row);
//			for (IReadyListener rl : readyListeners)
//			{
//				rl.isReady(true);
//			}
			tableViewer.refresh();
		}
	}

	public void setTable(ITable table, PersistContext context)
	{
		((DataProviderContentProvider)dataproviderTreeViewer.getContentProvider()).setTable(table, context);
		dataproviderTreeViewer.refreshTree();
	}


	public PlaceDataProviderConfiguration getDataProviderConfiguration()
	{
		// TODO rem
		return null;
	}
}