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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Shell;
import org.sablo.specification.PropertyDescription;

import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions.INCLUDE_RELATIONS;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.ITable;

/**
 * @author emera
 */
public class PropertyWizardDialogBuilder
{
	private final Shell shell;
	private final PersistContext persistContext;
	private final FlattenedSolution flattenedSolution;
	private PropertyWizardDialog dialog;
	private Collection<PropertyDescription> wizardProperties;
	private List<Map<String, Object>> input;
	private final PropertyDescription mainProperty;
	private ITable table;


	public PropertyWizardDialogBuilder(Shell shell, PersistContext persistContext, FlattenedSolution flattenedSolution, PropertyDescription property)
	{
		this.shell = shell;
		this.persistContext = persistContext;
		this.flattenedSolution = flattenedSolution;
		this.mainProperty = property;
	}

	public PropertyWizardDialogBuilder withProperties(Collection<PropertyDescription> properties)
	{
		this.wizardProperties = properties;
		return this;
	}

	public PropertyWizardDialogBuilder withInput(List<Map<String, Object>> _input)
	{
		this.input = _input;
		return this;
	}

	public PropertyWizardDialogBuilder withTable(ITable _table)
	{
		this.table = _table;
		return this;
	}

	public int open()
	{
		dialog = new PropertyWizardDialog(shell, persistContext, flattenedSolution,
			table,
			new DataProviderTreeViewer.DataProviderOptions(false, true, true, true, true, true, true, true,
				INCLUDE_RELATIONS.NESTED, true, true, null),
			EditorUtil.getDialogSettings("PropertyWizard"), mainProperty, wizardProperties, input);
		//TODO dataprovider
		return dialog.open();
	}


	public List<Map<String, Object>> getResult()
	{
		return dialog.getResult();
	}
}
