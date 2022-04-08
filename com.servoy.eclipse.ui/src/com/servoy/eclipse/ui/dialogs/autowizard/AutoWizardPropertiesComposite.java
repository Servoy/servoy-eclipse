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
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;

import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.util.Pair;

/**
 * @author emera
 */
public class AutoWizardPropertiesComposite
{
	private final AutoWizardConfigurationViewer tableViewer;
	private List<Pair<String, Map<String, Object>>> treeInput = new ArrayList<>();

	private final List<PropertyDescription> dataproviderProperties;
	private final List<PropertyDescription> styleProperties;
	private final PersistContext persistContext;
	private final List<PropertyDescription> i18nProperties;
	private final List<PropertyDescription> stringProperties;
	private final FlattenedSolution flattenedSolution;


	public AutoWizardPropertiesComposite(final Composite parent, PersistContext persistContext, FlattenedSolution flattenedSolution, ITable table,
		List<PropertyDescription> dataproviderProperties,
		List<PropertyDescription> styleProperties, List<PropertyDescription> i18nProperties, List<PropertyDescription> stringProperties,
		List<Map<String, Object>> childrenProperties)
	{
		this.flattenedSolution = flattenedSolution;
		this.dataproviderProperties = dataproviderProperties;
		this.styleProperties = styleProperties;
		this.persistContext = persistContext;
		this.i18nProperties = i18nProperties;
		this.stringProperties = stringProperties;


		tableViewer = createTableViewer(parent, table);
		if (childrenProperties != null) setInputProperties(childrenProperties);
		tableViewer.setInput(treeInput);
	}

	private void setInputProperties(List<Map<String, Object>> childrenProperties)
	{
		treeInput = new ArrayList<>();
		for (Map<String, Object> map : childrenProperties)
		{
			String dpValue = findDPValue(map);
			treeInput.add(new Pair<>(dpValue, map));
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


	private AutoWizardConfigurationViewer createTableViewer(Composite parent, ITable table)
	{
		final Composite container = new Composite(parent, SWT.NONE);
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

		AutoWizardConfigurationViewer viewer = new AutoWizardConfigurationViewer(container, persistContext, flattenedSolution, table, dataproviderProperties,
			styleProperties, i18nProperties, stringProperties, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		return viewer;
	}

	public List<Map<String, Object>> getResult()
	{
		Map<String, String> prefillProperties = new HashMap<>();
		for (PropertyDescription dp : stringProperties)
		{
			Object tag = dp.getTag("wizard");
			if (tag instanceof JSONObject)
			{
				String prefillProperty = ((JSONObject)tag).getString("prefill");
				if (prefillProperty != null)
				{
					prefillProperties.put(prefillProperty, dp.getName());
					continue;
				}
			}
		}
		List<Map<String, Object>> returnValue = treeInput.stream().map(pair -> pair.getRight()).collect(Collectors.toList());
		if (prefillProperties.size() > 0)
		{
			returnValue.forEach(row -> {
				prefillProperties.forEach((key, value) -> {
					row.put(value, row.get(key));
				});
			});
		}
		return returnValue;
	}

	public AutoWizardConfigurationViewer getViewer()
	{
		return tableViewer;
	}

	public void setInput(List<Pair<String, Map<String, Object>>> list)
	{
		treeInput = list;
		getViewer().setInput(list);
		getViewer().refresh();
	}

	public void addNewRow(Pair<String, Map<String, Object>> row)
	{
		treeInput.add(row);
		getViewer().setInput(treeInput);
		getViewer().refresh();
	}

	public List<Pair<String, Map<String, Object>>> getInput()
	{
		return treeInput;
	}
}