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
package com.servoy.eclipse.ui.editors.relation;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.DataSourceWrapperFactory;
import com.servoy.eclipse.model.util.IDataSourceWrapper;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.dialogs.TableContentProvider;
import com.servoy.eclipse.ui.dialogs.TableContentProvider.TableListOptions;
import com.servoy.eclipse.ui.editors.RelationEditor;
import com.servoy.eclipse.ui.labelproviders.DatasourceLabelProvider;
import com.servoy.eclipse.ui.property.TableValueEditor;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.TreeSelectViewer;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;

public class DatasourceSelectComposite extends Composite
{

	private final TreeSelectViewer sourceTable;
	private final TreeSelectViewer destinationTable;
	private ISelectionChangedListener sourceListener;
	private ISelectionChangedListener destinationListener;

	/**
	 * Create the composite
	 *
	 * @param parent
	 * @param style
	 */
	public DatasourceSelectComposite(Composite parent, int style)
	{
		super(parent, style);

		Label sourceLabel = new Label(this, SWT.NONE);
		sourceLabel.setText("Source");

		Label destinationLabel = new Label(this, SWT.NONE);
		destinationLabel.setText("Destination");

		sourceTable = new TreeSelectViewer(this, SWT.NONE, TableValueEditor.INSTANCE);
		sourceTable.setName("relationSourceDialog");
		Control sourceTableControl = sourceTable.getControl();
		sourceTable.setContentProvider(new TableContentProvider());
		sourceTable.setLabelProvider(DatasourceLabelProvider.INSTANCE_IMAGE_NAMEONLY);
		sourceTable.setTextLabelProvider(new DatasourceLabelProvider(Messages.LabelSelect, false, true));

		sourceTable.setInput(new TableContentProvider.TableListOptions(TableListOptions.TableListType.ALL, false, true));
		sourceTable.setEditable(true);

		destinationTable = new TreeSelectViewer(this, SWT.NONE, TableValueEditor.INSTANCE);
		destinationTable.setName("relationDestinationDialog");
		Control destinationTableControl = destinationTable.getControl();
		destinationTable.setContentProvider(new TableContentProvider());
		destinationTable.setLabelProvider(DatasourceLabelProvider.INSTANCE_IMAGE_NAMEONLY);
		destinationTable.setTextLabelProvider(new DatasourceLabelProvider(Messages.LabelSelect, false, true));
		destinationTable.setInput(new TableContentProvider.TableListOptions(TableListOptions.TableListType.ALL, false, false));
		destinationTable.setEditable(true);

		Label glue = new Label(this, SWT.NONE);

		final GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().add(6, 6, 6).add(groupLayout.createParallelGroup(GroupLayout.TRAILING).add(
				groupLayout.createSequentialGroup().add(sourceLabel, GroupLayout.DEFAULT_SIZE, 160, Short.MAX_VALUE).add(76, 76, 76)).add(
					groupLayout.createSequentialGroup().add(sourceTableControl, GroupLayout.DEFAULT_SIZE, 198, Short.MAX_VALUE).addPreferredGap(
						LayoutStyle.RELATED).add(glue, GroupLayout.PREFERRED_SIZE, 26, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.RELATED)))
				.add(6,
					6, 6)
				.add(
					groupLayout.createParallelGroup(GroupLayout.TRAILING).add(groupLayout.createSequentialGroup().add(destinationTableControl,
						GroupLayout.DEFAULT_SIZE, 236, Short.MAX_VALUE).addContainerGap()).add(
							groupLayout.createSequentialGroup().add(destinationLabel, GroupLayout.DEFAULT_SIZE, 222, Short.MAX_VALUE).add(28, 28,
								28)))));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(groupLayout.createSequentialGroup().addContainerGap().add(
			groupLayout.createParallelGroup(GroupLayout.TRAILING).add(glue).add(groupLayout.createSequentialGroup().add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(sourceLabel).add(destinationLabel)).addPreferredGap(LayoutStyle.RELATED).add(
					groupLayout.createParallelGroup(GroupLayout.LEADING).add(sourceTableControl).add(destinationTableControl))))
			.addContainerGap(10,
				Short.MAX_VALUE)));
		setLayout(groupLayout);

	}

	public class NameFiller
	{
		private final Text nameFieldToModify;
		private String oldSourceTable;
		private String oldDestinationTable;

		public NameFiller(Text nameField, String oldSourceTable, String oldDestinationServer)
		{
			nameFieldToModify = nameField;
			this.oldSourceTable = oldSourceTable;
			this.oldDestinationTable = oldDestinationServer;
		}

		public void modifyText()
		{
			if (sourceTable.getSelection() != null && !sourceTable.getSelection().isEmpty())
			{
				String oldRelationName = getRelationNameWithDefaultDefault(oldSourceTable, oldDestinationTable);
				if (oldRelationName.equalsIgnoreCase(nameFieldToModify.getText()) || "untitled".equals(nameFieldToModify.getText()))
				{
					IStructuredSelection selection = (IStructuredSelection)destinationTable.getSelection();
					if (!selection.isEmpty())
					{
						IDataSourceWrapper tableWrapper = ((IDataSourceWrapper)selection.getFirstElement());
						oldDestinationTable = tableWrapper.getTableName();
					}
					else
					{
						oldDestinationTable = "";
					}
					selection = (IStructuredSelection)sourceTable.getSelection();
					IDataSourceWrapper tableWrapper = ((IDataSourceWrapper)selection.getFirstElement());
					oldSourceTable = tableWrapper.getTableName();
					nameFieldToModify.setText(getRelationNameWithDefaultDefault(oldSourceTable, oldDestinationTable));
				}
			}

		}

		private String getRelationNameWithDefaultDefault(String primaryTable, String foreignTable)
		{
			String relationName = EditorUtil.getRelationName(primaryTable, foreignTable, null, null);
			if (relationName == null)
			{
				relationName = oldSourceTable + "_to_" + oldDestinationTable;
			}
			return relationName;
		}
	}


	@Override
	protected void checkSubclass()
	{
		// Disable the check that prevents subclassing of SWT components
	}

	private void setSelection(RelationEditor relationEditor)
	{
		IDataSourceWrapper primaryDataSourceWrapper = DataSourceWrapperFactory.getWrapper(relationEditor.getRelation().getPrimaryDataSource());
		if (primaryDataSourceWrapper != null)
		{
			sourceTable.setSelection(new StructuredSelection(primaryDataSourceWrapper));
		}
		else
		{
			sourceTable.setSelection(StructuredSelection.EMPTY);
		}
		IDataSourceWrapper foreignDataSourceWrapper = DataSourceWrapperFactory.getWrapper(relationEditor.getRelation().getForeignDataSource());
		if (primaryDataSourceWrapper != null)
		{
			destinationTable.setSelection(new StructuredSelection(foreignDataSourceWrapper));
		}
		else
		{
			destinationTable.setSelection(StructuredSelection.EMPTY);
		}
	}

	public void initDataBindings(final RelationEditor relationEditor, final NameFiller filler)
	{
		setSelection(relationEditor);
		sourceListener = new ISelectionChangedListener()
		{
			public void selectionChanged(SelectionChangedEvent event)
			{
				IStructuredSelection selection = (IStructuredSelection)sourceTable.getSelection();
				if (!selection.isEmpty())
				{
					relationEditor.unregisterListeners();
					IDataSourceWrapper tableWrapper = ((IDataSourceWrapper)selection.getFirstElement());
					relationEditor.getRelation().setPrimaryDataSource(tableWrapper.getDataSource());
					IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
					if (servoyModel.getDataModelManager() != null)
					{
						servoyModel.getDataModelManager().testTableAndCreateDBIFile(
							servoyModel.getDataSourceManager().getDataSource(relationEditor.getRelation().getPrimaryDataSource()));
					}
					relationEditor.registerListeners();
					relationEditor.createInput(false, true, true);
					relationEditor.flagModified(false);
					filler.modifyText();
				}
			}
		};
		destinationListener = new ISelectionChangedListener()
		{
			public void selectionChanged(SelectionChangedEvent event)
			{
				IStructuredSelection selection = (IStructuredSelection)destinationTable.getSelection();
				if (!selection.isEmpty())
				{
					relationEditor.unregisterListeners();
					IDataSourceWrapper tableWrapper = ((IDataSourceWrapper)selection.getFirstElement());
					Relation relation = relationEditor.getRelation();
					String oldServerName = relation.getForeignServerName();
					String oldTableName = relation.getForeignTableName();
					relationEditor.getRelation().setForeignDataSource(tableWrapper.getDataSource());
					IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
					if (servoyModel.getDataModelManager() != null)
					{
						servoyModel.getDataModelManager().testTableAndCreateDBIFile(
							servoyModel.getDataSourceManager().getDataSource(relationEditor.getRelation().getForeignDataSource()));
					}
					relationEditor.registerListeners();
					if (relationEditor.getRelation().getPrimaryDataSource() == null)
					{
						sourceTable.setSelection(new StructuredSelection(tableWrapper));
					}
					if (relation.getInitialSort() != null && tableWrapper.getServerName() != null && tableWrapper.getTableName() != null &&
						(!tableWrapper.getServerName().equals(oldServerName) || !tableWrapper.getTableName().equals(oldTableName)))
					{
						relation.setInitialSort(null);
						relationEditor.refreshOptions();
					}
					relationEditor.createInput(true, false, true);
					relationEditor.flagModified(false);
					filler.modifyText();
				}
			}
		};
		sourceTable.addSelectionChangedListener(sourceListener);
		destinationTable.addSelectionChangedListener(destinationListener);
	}

	public void refresh(RelationEditor relationEditor)
	{
		sourceTable.removeSelectionChangedListener(sourceListener);
		destinationTable.removeSelectionChangedListener(destinationListener);
		setSelection(relationEditor);
		relationEditor.createInput(false, false, false);
		sourceTable.addSelectionChangedListener(sourceListener);
		destinationTable.addSelectionChangedListener(destinationListener);
	}

	public void checkInconsistency() throws RepositoryException
	{
		if (!sourceTable.isValid())
		{
			throw new RepositoryException("Source server data is invalid.");
		}
		if (!destinationTable.isValid())
		{
			throw new RepositoryException("Destination server data is invalid.");
		}
	}

}
