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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.util.DataSourceWrapperFactory;
import com.servoy.eclipse.model.util.IDataSourceWrapper;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderContentProvider;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions;
import com.servoy.eclipse.ui.labelproviders.DataProviderLabelProvider;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.types.FoundsetDesignToChooserConverter;
import com.servoy.eclipse.ui.property.types.FoundsetPropertyEditor;
import com.servoy.eclipse.ui.views.TreeSelectViewer;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedConfig;
import com.servoy.j2db.util.Pair;

/**
 * @author emera
 */
public class DataproviderComposite extends Composite
{
	private final DataProviderTreeViewer dataproviderTreeViewer;
	private ListViewer stylePropertiesViewer;
	private final WizardConfigurationViewer tableViewer;
	private List<Pair<String, Map<String, Object>>> input = new ArrayList<>();
	private final IDialogSettings settings;
	private final List<PropertyDescription> dataproviderProperties;
	private final List<PropertyDescription> styleProperties;
	private TreeSelectViewer dataSourceViewer;
	private final PersistContext persistContext;
	private final FoundsetDesignToChooserConverter converter;
	private IDataSourceWrapper lastDatasourceValue;


	public DataproviderComposite(final Composite parent, PersistContext persistContext, FlattenedSolution flattenedSolution, ITable table,
		DataProviderOptions dataproviderOptions, final IDialogSettings settings, List<PropertyDescription> dataproviderProperties,
		List<PropertyDescription> styleProperties, List<Map<String, Object>> childrenProperties)
	{
		super(parent, SWT.None);
		this.settings = settings;
		this.dataproviderProperties = dataproviderProperties;
		this.styleProperties = styleProperties;
		this.persistContext = persistContext;
		converter = new FoundsetDesignToChooserConverter(flattenedSolution);

		this.setLayout(new FillLayout());
		SashForm form = new SashForm(this, SWT.HORIZONTAL);
		SashForm form2 = new SashForm(form, SWT.VERTICAL);

		if (dataproviderProperties.size() > 0)
			dataproviderTreeViewer = createDataproviderTree(form2, persistContext, flattenedSolution, table, dataproviderOptions);
		else dataproviderTreeViewer = null;

		if (styleProperties.size() > 0) // should really be alway 1 size..
		{
			Object tag = styleProperties.get(0).getTag("wizard");
			if (tag instanceof JSONArray)
			{
				GridLayout layout = new GridLayout(1, false);
				layout.marginHeight = 0;
				layout.marginWidth = 0;
				layout.marginRight = 5;
				Composite listParent = new Composite(form2, SWT.NONE);
				listParent.setLayout(layout);

				GridData gridData = new GridData();
				gridData.verticalAlignment = GridData.FILL;
				gridData.horizontalSpan = 1;
				gridData.verticalSpan = 1;
				gridData.grabExcessHorizontalSpace = true;
				gridData.grabExcessVerticalSpace = true;
				gridData.horizontalAlignment = GridData.FILL;
				gridData.minimumWidth = 300;
				gridData.heightHint = 250;

				stylePropertiesViewer = new ListViewer(listParent);
				stylePropertiesViewer.getControl().setLayoutData(gridData);
				stylePropertiesViewer.setContentProvider(JSONContentProvider.INSTANCE);
				stylePropertiesViewer.setLabelProvider(new LabelProvider()
				{
					@Override
					public String getText(Object element)
					{
						if (element instanceof JSONObject)
						{
							return ((JSONObject)element).getString("name");
						}
						return "";
					}
				});
				stylePropertiesViewer.setInput(tag);

				stylePropertiesViewer.addOpenListener(event -> moveStyleSelection());
				stylePropertiesViewer.addSelectionChangedListener(event -> moveStyleSelection());

			}
		}
		else stylePropertiesViewer = null;

		if (dataproviderTreeViewer != null && stylePropertiesViewer != null)
			form2.setWeights(70, 30);

		tableViewer = createTableViewer(form);
		if (childrenProperties != null) setInputProperties(childrenProperties);
		tableViewer.setInput(input);
	}

	private void setInputProperties(List<Map<String, Object>> childrenProperties)
	{
		input = new ArrayList<>();
		for (Map<String, Object> map : childrenProperties)
		{
			String dpValue = findDPValue(map);
			input.add(new Pair<>(dpValue, map));
		}
	}

	private String findDPValue(Map<String, Object> map)
	{
		for (PropertyDescription dp : dataproviderProperties)
		{
			if (map.get(dp.getName()) != null) return map.get(dp.getName()).toString();
		}
		return null;
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

		final WizardConfigurationViewer viewer = new WizardConfigurationViewer(container, dataproviderProperties, styleProperties,
			SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		return viewer;
	}

	private DataProviderTreeViewer createDataproviderTree(SashForm form, PersistContext persistContext, FlattenedSolution flattenedSolution, ITable table,
		DataProviderOptions dataproviderOptions)
	{

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

		dataSourceViewer = new TreeSelectViewer(datasourceComposite, SWT.NONE)
		{
			@Override
			protected IStructuredSelection openDialogBox(Control control)
			{
				return super.openDialogBox(control);
			}
		};

		dataSourceViewer.setContentProvider(FoundsetPropertyEditor.getFoundsetContentProvider(persistContext));
		dataSourceViewer.setLabelProvider(FoundsetPropertyEditor.getFoundsetLabelProvider(null, converter));
		dataSourceViewer.setTextLabelProvider(FoundsetPropertyEditor.getFoundsetLabelProvider(persistContext.getContext(), converter));
		dataSourceViewer.addSelectionChangedListener(new ISelectionChangedListener()
		{
			public void selectionChanged(SelectionChangedEvent event)
			{
				handleDataSourceSelected();
			}
		});
		ITable formTable = ((Form)persistContext.getContext()).getDataSource() != null
			? ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(((Form)persistContext.getContext()).getDataSource()) : null;
		dataSourceViewer.setInput(FoundsetPropertyEditor.getFoundsetInputOptions(formTable, null, false));
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
		DataProviderTreeViewer treeviewer = new DataProviderTreeViewer(parent, DataProviderLabelProvider.INSTANCE_HIDEPREFIX, // label provider will be overwritten when superform is known
			new DataProviderContentProvider(persistContext, flattenedSolution, table), dataproviderOptions, true, true,
			TreePatternFilter.getSavedFilterMode(settings, TreePatternFilter.FILTER_LEAFS), SWT.MULTI);
		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 1;
		gridData.verticalSpan = 1;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.minimumWidth = 400;
		gridData.heightHint = 450;

		treeviewer.setLayoutData(gridData);
		treeviewer.addSelectionChangedListener(event -> moveDataproviderSelection());
		treeviewer.addOpenListener(event -> moveDataproviderSelection());

		treeviewer.getViewer().getTree().setToolTipText("Select the dataprovders for which you want to place fields");


		return treeviewer;
	}

	protected void handleDataSourceSelected()
	{
		if (dataproviderTreeViewer == null) return;
		IStructuredSelection selection = (IStructuredSelection)dataSourceViewer.getSelection();
		if (!selection.isEmpty())
		{
			IDataSourceWrapper tw = getTableWrapper();
			if (tw == null && lastDatasourceValue == null || tw.equals(lastDatasourceValue)) return;
			ITable table = tw != null ? ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(tw.getDataSource()) : null;
			if (table != null)
			{
				if (!input.isEmpty() && !MessageDialog.openConfirm(getShell(), "Change datasource",
					"Changing the datasource will remove existing columns. Are you sure that you want to change it?"))
				{
					dataSourceViewer.setSelection(new StructuredSelection(lastDatasourceValue));
					return;
				}
				lastDatasourceValue = tw;
				input = new ArrayList<>();
				tableViewer.setInput(input);
				tableViewer.refresh();
				setTable(table, persistContext);
				IPersist persist = persistContext.getPersist();
				if (persist instanceof WebComponent)
				{
					WebComponent component = (WebComponent)persist;
					String fsProperty = ((FoundsetLinkedConfig)dataproviderProperties.get(0).getConfig()).getForFoundsetName();

					JSONObject value = converter.convertFromChooserValueToJSONValue(tw, null);
					component.setProperty(fsProperty, value);
				}
			}
			else
			{
				MessageDialog.openError(getShell(), "Datasource not found", "The datasource '" + tw.getDataSource() + "' was not found.");
			}
		}
	}

	private IDataSourceWrapper getTableWrapper()
	{
		IStructuredSelection selection = (IStructuredSelection)dataSourceViewer.getSelection();
		if (selection.getFirstElement() == FormFoundsetEntryContentProvider.FORM_FOUNDSET)
		{
			String dataSource = ((Form)persistContext.getContext()).getDataSource();
			return dataSource != null ? DataSourceWrapperFactory.getWrapper(dataSource) : null;
		}
		return (IDataSourceWrapper)selection.getFirstElement();
	}

	private void moveDataproviderSelection()
	{
		IDataProvider dataprovider = (IDataProvider)((StructuredSelection)dataproviderTreeViewer.getSelection()).getFirstElement();
		if (dataprovider != null)
		{
			input.add(new Pair<String, Map<String, Object>>(dataprovider.getDataProviderID(), getDefaultRow(dataprovider.getDataProviderID())));
			tableViewer.setInput(input);
			tableViewer.refresh();
		}
	}

	private void moveStyleSelection()
	{
		Object style = ((StructuredSelection)stylePropertiesViewer.getSelection()).getFirstElement();
		if (style instanceof JSONObject)
		{
			String name = ((JSONObject)style).getString("name");
			String styleClass = ((JSONObject)style).getString("cls");
			Map<String, Object> map = new HashMap<>();
			PropertyDescription propertyDescription = styleProperties.get(0);
			map.put(propertyDescription.getName(), styleClass);
			input.add(new Pair<String, Map<String, Object>>(name, map));
			tableViewer.setInput(input);
			tableViewer.refresh();
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

	public List<Map<String, Object>> getResult()
	{
		return input.stream().map(pair -> pair.getRight()).collect(Collectors.toList());
	}

	private static class JSONContentProvider implements IStructuredContentProvider
	{
		private static final IStructuredContentProvider INSTANCE = new JSONContentProvider();

		@Override
		public Object[] getElements(Object inputElement)
		{
			if (inputElement instanceof JSONArray)
			{
				JSONArray array = (JSONArray)inputElement;
				JSONObject[] objects = new JSONObject[array.length()];
				for (int i = 0; i < objects.length; i++)
				{
					objects[i] = array.getJSONObject(i);
				}
				return objects;
			}
			return null;
		}

	}
}