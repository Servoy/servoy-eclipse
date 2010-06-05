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

import java.util.Comparator;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.repository.EclipseUserManager;
import com.servoy.eclipse.ui.editors.TableEditor;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;

public class SecurityComposite extends Composite implements EclipseUserManager.IUserGroupChangeListener
{
	private final TableViewer tableViewer;

	private final Composite tableContainer;

	private final TableSettingsComposite settingsComposite;

	public SecurityComposite(Composite parent, int style, final TableEditor te)
	{
		super(parent, style);

		this.setLayout(new FillLayout());

		SashForm sashForm;
		sashForm = new SashForm(this, SWT.HORIZONTAL);

		tableContainer = new Composite(sashForm, SWT.NONE);
		tableViewer = new TableViewer(tableContainer, SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().setLinesVisible(true);

		tableViewer.addSelectionChangedListener(new ISelectionChangedListener()
		{

			public void selectionChanged(SelectionChangedEvent event)
			{
				ISelection sel = tableViewer.getSelection();
				if (sel instanceof IStructuredSelection)
				{
					Object first = ((IStructuredSelection)sel).getFirstElement();
					settingsComposite.setValues(first.toString());
				}

			}
		});
		settingsComposite = new TableSettingsComposite(sashForm, SWT.NONE, te);

		sashForm.setWeights(new int[] { 288, 209 });

		initDataBindings();
	}

	public static final int CI_NAME = 0;

	protected void initDataBindings()
	{
		TableColumn nameColumn = new TableColumn(tableViewer.getTable(), SWT.LEFT, CI_NAME);
		nameColumn.setText("Group");
		// nameColumn.setWidth(400);

		TableColumnLayout layout = new TableColumnLayout();
		tableContainer.setLayout(layout);
		layout.setColumnData(nameColumn, new ColumnWeightData(20, 50, true));

		tableViewer.setLabelProvider(new ITableLabelProvider()
		{

			public Image getColumnImage(Object element, int columnIndex)
			{
				return null;
			}

			public String getColumnText(Object element, int columnIndex)
			{
				return element.toString();
			}

			public void addListener(ILabelProviderListener listener)
			{

			}

			public void dispose()
			{

			}

			public boolean isLabelProperty(Object element, String property)
			{
				return false;
			}

			public void removeListener(ILabelProviderListener listener)
			{

			}

		});
		tableViewer.setContentProvider(new IStructuredContentProvider()
		{

			public void dispose()
			{

			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
			{

			}

			public Object[] getElements(Object inputElement)
			{
				IDataSet localGroups = (IDataSet)inputElement;
				int localGroupsNr = localGroups.getRowCount();
				Object[] groupNames = new Object[localGroupsNr];
				for (int i = 0; i < localGroupsNr; i++)
				{
					groupNames[i] = localGroups.getRow(i)[1];
				}
				return groupNames;
			}
		});
		EclipseUserManager eum = ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager();
		tableViewer.setInput(eum.getGroups(ApplicationServerSingleton.get().getClientId()));
		tableViewer.setSorter(new ColumnsSorter(tableViewer, new TableColumn[] { nameColumn }, new Comparator[] { NameComparator.INSTANCE }));
		eum.addUserGroupChangeListener(this);
	}

	@Override
	protected void checkSubclass()
	{
		// Disable the check that prevents subclassing of SWT components
	}

	public void saveValues()
	{
		settingsComposite.saveValues();
	}

	public void userGroupChanged()
	{
		tableViewer.setInput(ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager().getGroups(
			ApplicationServerSingleton.get().getClientId()));
	}

	@Override
	public void dispose()
	{
		EclipseUserManager eum = ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager();
		eum.removeUserGroupChangeListener(this);
		super.dispose();
	}
}
