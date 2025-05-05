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

package com.servoy.eclipse.ui.dialogs.autowizard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.util.DataSourceWrapperFactory;
import com.servoy.eclipse.model.util.IDataSourceWrapper;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderContentProvider;
import com.servoy.eclipse.ui.dialogs.FormFoundsetEntryContentProvider;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider.RelationsWrapper;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.labelproviders.DataProviderLabelProvider;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.types.FormFoundsetSelectionFilter;
import com.servoy.eclipse.ui.property.types.FoundsetDesignToChooserConverter;
import com.servoy.eclipse.ui.property.types.FoundsetPropertyEditor;
import com.servoy.eclipse.ui.views.TreeSelectViewer;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedConfig;

/**
 * @author emera
 */
public class DataproviderPropertiesSelector
{
	private final FoundsetDesignToChooserConverter converter;
	private TreeSelectViewer dataSourceViewer;
	private final DataProviderTreeViewer dataproviderTreeViewer;
	private IDataSourceWrapper lastDatasourceValue;
	private PersistContext persistContext;
	private PropertyWizardDialog wizard;
	private List<PropertyDescription> dataproviderProperties;
	private Shell shell;

	DataproviderPropertiesSelector(PropertyWizardDialog wizard, final SashForm form, PersistContext persistContext,
		PropertyWizardDialogConfigurator configurator,
		FlattenedSolution flattenedSolution, ITable table, IDialogSettings settings, Shell shell)
	{
		converter = new FoundsetDesignToChooserConverter(flattenedSolution);
		this.persistContext = persistContext;
		this.dataproviderProperties = configurator.getDataproviderProperties();
		this.wizard = wizard;
		this.shell = shell;

		Composite parent = new Composite(form, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.marginRight = 5;
		parent.setLayout(layout);

		Composite datasourceComposite = new Composite(parent, SWT.NONE);
		GridLayout datasourceLayout = new GridLayout(2, false);
		datasourceLayout.marginHeight = 0;
		datasourceLayout.marginWidth = 0;
		datasourceComposite.setLayout(datasourceLayout);
		datasourceComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

		Label datasourceLabel = new Label(datasourceComposite, SWT.NONE);
		datasourceLabel.setText("&Datasource");

		dataSourceViewer = new TreeSelectViewer(datasourceComposite, SWT.NONE, 10)
		{
			@Override
			protected IStructuredSelection openDialogBox(Control control)
			{
				return super.openDialogBox(control);
			}
		};

		dataSourceViewer.setContentProvider(FoundsetPropertyEditor.getFoundsetContentProvider(persistContext));
		dataSourceViewer.setLabelProvider(FoundsetPropertyEditor.getFoundsetLabelProvider(null, converter, null));
		dataSourceViewer.setTextLabelProvider(FoundsetPropertyEditor.getFoundsetLabelProvider(persistContext, converter, null));

		Form frm = persistContext.getContext() != null ? (Form)persistContext.getContext().getAncestor(IRepository.FORMS) : null;
		ITable formTable = frm != null && frm.getDataSource() != null
			? ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(frm.getDataSource()) : null;
		dataSourceViewer.setInput(FoundsetPropertyEditor.getFoundsetInputOptions(formTable, null, false));
		dataSourceViewer.setSelectionFilter(new FormFoundsetSelectionFilter(null));
		GridData data = new GridData(SWT.FILL, SWT.NONE, true, false);
		data.horizontalAlignment = GridData.FILL;
		dataSourceViewer.getControl().setLayoutData(data);
		IPersist persist = persistContext.getPersist();
		if (persist instanceof WebComponent)
		{
			WebComponent component = (WebComponent)persist;
			String fsProperty = ((FoundsetLinkedConfig)dataproviderProperties.get(0).getConfig()).getForFoundsetName();
			Object fsValue = component.getProperty(fsProperty);
			if (fsValue == null)
			{
				fsValue = component.getPropertyDefaultValueClone(fsProperty);
			}
			dataSourceViewer.setSelection(new StructuredSelection(converter.convertJSONValueToChooserValue(fsValue)));
			dataSourceViewer.setValid(true);
			lastDatasourceValue = getTableWrapper();
		}
		dataproviderTreeViewer = new DataProviderTreeViewer(parent, DataProviderLabelProvider.INSTANCE_HIDEPREFIX, // label provider will be overwritten when superform is known
			new DataProviderContentProvider(persistContext, flattenedSolution,
				lastDatasourceValue != null ? ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(lastDatasourceValue.getDataSource())
					: table),
			configurator.getDataproviderOptions(), true, true,
			TreePatternFilter.getSavedFilterMode(settings, TreePatternFilter.FILTER_LEAFS), SWT.MULTI);
		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 1;
		gridData.verticalSpan = 1;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.minimumWidth = 10;
		gridData.heightHint = 450;

		dataproviderTreeViewer.setLayoutData(gridData);
		dataproviderTreeViewer.addSelectionChangedListener(event -> moveDataproviderSelection());
		dataproviderTreeViewer.addOpenListener(event -> moveDataproviderSelection());

		dataproviderTreeViewer.getViewer().getTree().setToolTipText("Select the dataproviders for which you want to place fields");

		dataSourceViewer.addSelectionChangedListener(new ISelectionChangedListener()
		{
			public void selectionChanged(SelectionChangedEvent event)
			{
				handleDataSourceSelected();
			}
		});
	}

	protected void handleDataSourceSelected()
	{
		IStructuredSelection selection = (IStructuredSelection)dataSourceViewer.getSelection();
		if (!selection.isEmpty())
		{
			IDataSourceWrapper tw = getTableWrapper();
			if (tw == null && lastDatasourceValue == null || tw.equals(lastDatasourceValue)) return;
			ITable table = tw != null ? ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(tw.getDataSource()) : null;
			if (table != null)
			{
				if (!wizard.getInput().isEmpty() && !MessageDialog.openConfirm(shell, "Change datasource",
					"Changing the datasource will remove existing columns. Are you sure that you want to change it?"))
				{
					dataSourceViewer.setSelection(new StructuredSelection(lastDatasourceValue));
					return;
				}
				lastDatasourceValue = tw;
				wizard.setTreeInput(new ArrayList<>());

				setTable(table, persistContext);
				IPersist persist = persistContext.getPersist();
				if (persist instanceof WebComponent)
				{
					WebComponent component = (WebComponent)persist;
					String fsProperty = ((FoundsetLinkedConfig)dataproviderProperties.get(0).getConfig()).getForFoundsetName();

					JSONObject value = converter.convertFromChooserValueToJSONValue(selection.getFirstElement(), null);
					component.setProperty(fsProperty, value);
				}
				dataSourceViewer.setValid(true);
			}
			else
			{
				MessageDialog.openError(shell, "Datasource not found", "The datasource '" + tw.getDataSource() + "' was not found.");
				dataSourceViewer.setValid(false);
			}
		}
	}

	private IDataSourceWrapper getTableWrapper()
	{
		IStructuredSelection selection = (IStructuredSelection)dataSourceViewer.getSelection();
		if (selection.getFirstElement() == FormFoundsetEntryContentProvider.FORM_FOUNDSET)
		{
			Form frm = persistContext.getContext() != null ? (Form)persistContext.getContext().getAncestor(IRepository.FORMS) : null;
			String dataSource = frm != null ? frm.getDataSource() : null;
			return dataSource != null ? DataSourceWrapperFactory.getWrapper(dataSource) : null;
		}
		else if (selection.getFirstElement() instanceof RelationsWrapper)
		{
			RelationsWrapper relationsWrapper = (RelationsWrapper)selection.getFirstElement();
			Relation relation = relationsWrapper.relations[relationsWrapper.relations.length - 1];
			String dataSource = relation != null ? relation.getForeignDataSource() : null;
			return dataSource != null ? DataSourceWrapperFactory.getWrapper(dataSource) : null;
		}
		return (IDataSourceWrapper)selection.getFirstElement();
	}

	private void moveDataproviderSelection()
	{
		IDataProvider dataprovider = (IDataProvider)((StructuredSelection)dataproviderTreeViewer.getSelection()).getFirstElement();
		if (dataprovider != null)
		{
			wizard.addNewRow(getDefaultRow(dataprovider.getDataProviderID()));
		}
	}

	private Map<String, Object> getDefaultRow(String val)
	{
		Map<String, Object> row = new HashMap<>();
		for (PropertyDescription pd : dataproviderProperties)
		{
			row.put(pd.getName(), pd.getDefaultValue());
		}
		row.put(dataproviderProperties.get(0).getName(), val);
		return row;
	}

	public void setTable(ITable table, PersistContext context)
	{
		((DataProviderContentProvider)dataproviderTreeViewer.getContentProvider()).setTable(table, context);
		dataproviderTreeViewer.refreshTree();
	}
}
