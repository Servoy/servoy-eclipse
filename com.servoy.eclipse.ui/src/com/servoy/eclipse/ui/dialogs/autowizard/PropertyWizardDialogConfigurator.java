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
import java.util.stream.Collectors;

import org.eclipse.swt.widgets.Shell;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.types.StyleClassPropertyType;

import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions.INCLUDE_RELATIONS;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedPropertyType;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.server.ngclient.property.types.FormPropertyType;
import com.servoy.j2db.server.ngclient.property.types.RelationPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ServoyStringPropertyType;
import com.servoy.j2db.server.ngclient.property.types.TagStringPropertyType;

/**
 * @author emera
 */
public class PropertyWizardDialogConfigurator
{
	private final Shell shell;
	private final PersistContext persistContext;
	private final FlattenedSolution flattenedSolution;
	private PropertyWizardDialog dialog;
	private Collection<PropertyDescription> wizardProperties;
	private List<Map<String, Object>> input;
	private final PropertyDescription mainProperty;
	private ITable table;
	private List<PropertyDescription> dataproviderProperties;
	private List<PropertyDescription> styleProperties;
	private List<PropertyDescription> i18nProperties;
	private List<PropertyDescription> stringProperties;
	private List<PropertyDescription> formProperties;
	private List<PropertyDescription> relationProperties;
	private List<PropertyDescription> orderedProperties;
	private List<PropertyDescription> prefillProperties;


	public PropertyWizardDialogConfigurator(Shell shell, PersistContext persistContext, FlattenedSolution flattenedSolution, PropertyDescription property)
	{
		this.shell = shell;
		this.persistContext = persistContext;
		this.flattenedSolution = flattenedSolution;
		this.mainProperty = property;
	}

	public PropertyWizardDialogConfigurator withProperties(Collection<PropertyDescription> properties)
	{
		this.wizardProperties = properties;
		dataproviderProperties = wizardProperties.stream()
			.filter(prop -> FoundsetLinkedPropertyType.class.isAssignableFrom(prop.getType().getClass()) ||
				DataproviderPropertyType.class.isAssignableFrom(prop.getType().getClass()))
			.sorted((desc1, desc2) -> getOrder(desc1) - getOrder(desc2))
			.collect(Collectors.toList());
		// should be only 1 (or only 1 with values)
		styleProperties = filterProperties(StyleClassPropertyType.class);
		i18nProperties = filterProperties(TagStringPropertyType.class);
		stringProperties = filterProperties(ServoyStringPropertyType.class);
		formProperties = filterProperties(FormPropertyType.class);
		relationProperties = filterProperties(RelationPropertyType.class);
		orderedProperties = properties.stream().sorted((desc1, desc2) -> getOrder(desc1) - getOrder(desc2)).collect(Collectors.toList());
		prefillProperties = wizardProperties.stream()
			.filter(prop -> prop.getTag("wizard") instanceof JSONObject && ((JSONObject)prop.getTag("wizard")).optString("prefill", null) != null)
			.collect(Collectors.toList());
		return this;
	}

	private int getOrder(PropertyDescription desc1)
	{
		try
		{
			if (desc1.getTag("wizard") instanceof JSONObject)
			{
				JSONObject w = (JSONObject)desc1.getTag("wizard");
				return Integer.parseInt(w.optString("order", null));
			}
			if (desc1.getTag("wizard") instanceof JSONArray)
			{
				return Integer.MAX_VALUE;
			}
			return Integer.parseInt((String)desc1.getTag("wizard"));
		}
		catch (NumberFormatException ex)
		{
			return Integer.MAX_VALUE;
		}
	}

	public PropertyWizardDialogConfigurator withInput(List<Map<String, Object>> _input)
	{
		this.input = _input;
		return this;
	}

	public PropertyWizardDialogConfigurator withTable(ITable _table)
	{
		this.table = _table;
		return this;
	}

	public int open()
	{
		dialog = new PropertyWizardDialog(shell, persistContext, flattenedSolution,
			table,
			EditorUtil.getDialogSettings("PropertyWizard" + mainProperty.getName()), mainProperty, this);
		return dialog.open();
	}


	public List<Map<String, Object>> getResult()
	{
		return dialog.getResult();
	}

	private List<PropertyDescription> filterProperties(Class< ? > cls)
	{
		return wizardProperties.stream()
			.filter(prop -> cls.isAssignableFrom(prop.getType().getClass()))
			.collect(Collectors.toList());
	}

	public DataProviderOptions getDataproviderOptions()
	{
		return new DataProviderTreeViewer.DataProviderOptions(false, true, true, true, true, true, true, true,
			INCLUDE_RELATIONS.NESTED, true, true, null);
	}

	public List<PropertyDescription> getStyleProperties()
	{
		return styleProperties;
	}

	public List<PropertyDescription> getDataproviderProperties()
	{
		return dataproviderProperties;
	}

	public List<PropertyDescription> getI18nProperties()
	{
		return i18nProperties;
	}

	public List<PropertyDescription> getStringProperties()
	{
		return stringProperties;
	}

	public List<PropertyDescription> getFormProperties()
	{
		return formProperties;
	}

	public List<PropertyDescription> getRelationProperties()
	{
		return relationProperties;
	}

	public List<Map<String, Object>> getInput()
	{
		return input;
	}

	public ITable getTable()
	{
		return table;
	}

	public String getAutoPropertyName()
	{
		return mainProperty.getName();
	}

	public List<PropertyDescription> getOrderedProperties()
	{
		return orderedProperties;
	}

	public List<PropertyDescription> getPrefillProperties()
	{
		return prefillProperties;
	}
}
