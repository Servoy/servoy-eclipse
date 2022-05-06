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
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.json.JSONObject;

import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.ITable;

/**
 * @author emera
 */
public class AutoWizardPropertiesComposite
{
	private final AutoWizardConfigurationViewer tableViewer;
	private List<Map<String, Object>> treeInput = new ArrayList<>();

	private final PersistContext persistContext;
	private final FlattenedSolution flattenedSolution;
	private final PropertyWizardDialogConfigurator propertiesConfigurator;


	public AutoWizardPropertiesComposite(final Composite parent, PersistContext persistContext, FlattenedSolution flattenedSolution,
		PropertyWizardDialogConfigurator configurator)
	{
		this.flattenedSolution = flattenedSolution;
		this.persistContext = persistContext;
		this.propertiesConfigurator = configurator;

		tableViewer = createTableViewer(parent, configurator.getTable());
		if (configurator.getInput() != null) setInputProperties(configurator.getInput());
		tableViewer.setInput(treeInput);
	}

	private void setInputProperties(List<Map<String, Object>> childrenProperties)
	{
		treeInput = childrenProperties;
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

		AutoWizardConfigurationViewer viewer = new AutoWizardConfigurationViewer(container, persistContext, flattenedSolution, table,
			propertiesConfigurator,
			SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION, propertiesConfigurator.getAutoPropertyName());
		return viewer;
	}
	
	public List<Map<String, Object>> getResult()
	{
		List<Map<String, Object>> returnValue = treeInput;
		if (propertiesConfigurator.getPrefillProperties().size() > 0)
		{
			returnValue.forEach(row -> {
				propertiesConfigurator.getPrefillProperties().forEach(pd -> {
					row.put(pd.getName(), row.get((((JSONObject)pd.getTag("wizard")).get("prefill"))));
				});
			});
		}
		return returnValue;
	}

	public AutoWizardConfigurationViewer getViewer()
	{
		return tableViewer;
	}

	public void setInput(List<Map<String, Object>> list)
	{
		treeInput = list;
		getViewer().setInput(list);
		getViewer().refresh();
	}

	public void addNewRow(Map<String, Object> row)
	{
		treeInput.add(row);
		getViewer().setInput(treeInput);
		getViewer().refresh();
	}

	public List<Map<String, Object>> getInput()
	{
		return treeInput;
	}
}