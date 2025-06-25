/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.eclipse.designer.editor.rfb.actions.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.FixedComboBoxCellEditor;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
public class ConversionComponentDialog extends Dialog
{
	public static final int CI_OLD_PROPERTY_NAME = 0;
	public static final int CI_PROPERTY_VALUE = 1;
	public static final int CI_NEW_PROPERTY_NAME = 2;

	private final PersistContext value;
	private TableViewer propertiesTableViewer;
	private final List<Pair<String, String>> selectedSpecProperties = new ArrayList<Pair<String, String>>();
	private String newSpecName;

	public ConversionComponentDialog(Shell shell, PersistContext value)
	{
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
		this.value = value;
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		Composite composite = (Composite)super.createDialogArea(parent);
		String initialSpecName = FormTemplateGenerator.getComponentTypeName((IFormElement)value.getPersist());
		getShell().setText(
			"Convert Component - '" + ((IFormElement)value.getPersist()).getName() + "' - " +
				initialSpecName);

		Label label = new Label(composite, SWT.NONE);
		label.setText("New Component Type");
		Combo specsCombo = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
		List<String> specs = new ArrayList<String>();
		WebObjectSpecification[] webComponentSpecifications = WebComponentSpecProvider.getSpecProviderState().getAllWebObjectSpecifications();
		for (WebObjectSpecification webComponentSpec : webComponentSpecifications)
		{
			if (webComponentSpec.isDeprecated()) continue;
			if (!webComponentSpec.getPackageName().equals("servoydefault") && !initialSpecName.equals(webComponentSpec.getName()))
			{
				specs.add(webComponentSpec.getName());
			}
		}
		specs.sort(String::compareTo);
		specsCombo.setItems(specs.toArray(new String[0]));
		specsCombo.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				String selectedSpec = specsCombo.getText();
				if ("-none-".equals(selectedSpec))
				{
					selectedSpecProperties.clear();
					newSpecName = null;
				}
				else
				{
					WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(selectedSpec);
					if (spec != null)
					{
						newSpecName = spec.getName();
						List<String> newProperties = getPropertiesForSpec(spec.getName());
						for (Pair<String, String> pair : selectedSpecProperties)
						{
							if (newProperties.contains(pair.getLeft()))
							{
								pair.setRight(pair.getLeft());
							}
						}

					}
				}
				propertiesTableViewer.refresh();
			}
		});
		Composite tableContainer = new Composite(composite, SWT.NONE);

		propertiesTableViewer = new TableViewer(tableContainer, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		Table propertiesTable = propertiesTableViewer.getTable();
		propertiesTable.setLinesVisible(true);
		propertiesTable.setHeaderVisible(true);

		TableColumn propertyNameColumn = new TableColumn(propertiesTable, SWT.LEFT, CI_OLD_PROPERTY_NAME);
		propertyNameColumn.setText("Property Name");


		TableColumn propertyValueColumn = new TableColumn(propertiesTable, SWT.LEFT, CI_PROPERTY_VALUE);
		propertyValueColumn.setText("Property Value");

		TableColumn propertyMappingColumn = new TableColumn(propertiesTable, SWT.LEFT, CI_NEW_PROPERTY_NAME);
		propertyMappingColumn.setText("New Component Property Mapping");
		TableViewerColumn propertyMappingViewerColumn = new TableViewerColumn(propertiesTableViewer, propertyMappingColumn);
		propertyMappingViewerColumn.setEditingSupport(new EditingSupport(propertiesTableViewer)
		{

			@Override
			protected CellEditor getCellEditor(Object element)
			{
				List<String> properties = getPropertiesForSpec(newSpecName);
				properties.add(0, "-none-");
				FixedComboBoxCellEditor editor = new FixedComboBoxCellEditor(propertiesTable, properties.toArray(new String[0]),
					SWT.BORDER | SWT.READ_ONLY);
				((CCombo)editor.getControl()).select(properties.indexOf(((Pair<String, String>)element).getRight()));
				return editor;
			}

			@Override
			protected boolean canEdit(Object element)
			{
				return newSpecName != null;
			}

			@Override
			protected Object getValue(Object element)
			{
				String propertyName = ((Pair<String, String>)element).getRight();
				if (propertyName != null && propertyName.length() > 0)
				{
					int index = getPropertiesForSpec(newSpecName).indexOf(propertyName);
					if (index >= 0)
					{
						return Integer.valueOf(index);
					}
				}
				return Integer.valueOf(0);
			}

			@Override
			protected void setValue(Object element, Object value)
			{
				int index = Utils.getAsInteger(value, false);
				List<String> properties = getPropertiesForSpec(newSpecName);
				properties.add(0, "-none-");
				String selectedPropertyName = null;
				if (index >= 0 && index < properties.size())
				{
					selectedPropertyName = properties.get(index);
					if ("-none-".equals(selectedPropertyName))
					{
						selectedPropertyName = null;
					}
				}
				((Pair<String, String>)element).setRight(selectedPropertyName);
				getViewer().update(element, null);
			}

		});

		TableColumnLayout layout = new TableColumnLayout();
		tableContainer.setLayout(layout);
		layout.setColumnData(propertyNameColumn, new ColumnWeightData(1, 200, true));
		layout.setColumnData(propertyValueColumn, new ColumnWeightData(1, 200, true));
		layout.setColumnData(propertyMappingColumn, new ColumnWeightData(1, 200, true));

		propertiesTableViewer.setLabelProvider(new ITableLabelProvider()
		{
			public Image getColumnImage(Object element, int columnIndex)
			{
				return null;
			}

			public String getColumnText(Object element, int columnIndex)
			{
				if (columnIndex == CI_OLD_PROPERTY_NAME) return ((Pair<String, String>)element).getLeft();
				if (columnIndex == CI_NEW_PROPERTY_NAME) return ((Pair<String, String>)element).getRight();
				if (columnIndex == CI_PROPERTY_VALUE)
				{
					String propertyName = ((Pair<String, String>)element).getLeft();
					Object propertyValue = ((BaseComponent)value.getPersist()).getProperty(propertyName);
					UUID uuid = Utils.getAsUUID(propertyValue, false);
					if (uuid != null)
					{
						IPersist persist = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().searchPersist(uuid);
						if (persist instanceof ISupportName nameSupport)
						{
							return nameSupport.getName();
						}
					}
					return propertyValue != null ? propertyValue.toString() : "-none-";
				}
				return null;
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
		propertiesTableViewer.setContentProvider(new ArrayContentProvider());
		getPropertiesForSpec(initialSpecName).forEach(propertyName -> {
			selectedSpecProperties.add(new Pair<>(propertyName, null));
		});
		propertiesTableViewer.setInput(selectedSpecProperties);

		return composite;
	}

	/**
	 * @return
	 */
	public String getSpecName()
	{
		return newSpecName;
	}

	public List<Pair<String, String>> getValue()
	{
		return selectedSpecProperties;
	}

	private List<String> getPropertiesForSpec(String specName)
	{
		List<String> properties = new ArrayList<>();
		WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(specName);
		if (spec != null)
		{
			spec.getProperties().forEach((name, property) -> {
				if (!property.isDeprecated())
				{
					properties.add(name);
				}
			});
			spec.getHandlers().forEach((name, method) -> {
				if (!method.isDeprecated())
				{
					properties.add(name);
				}
			});
			properties.sort(String::compareTo);
		}
		return properties;
	}
}
