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

package com.servoy.eclipse.ui.dialogs.autowizard.nattable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.sablo.specification.PropertyDescription;

import com.servoy.eclipse.ui.dialogs.autowizard.PropertyWizardDialogConfigurator;
import com.servoy.j2db.util.Utils;

/**
 * @author emera
 */
public class AutoWizardTableColumnPropertyAccessor implements IColumnPropertyAccessor<Map<String, Object>>
{
	private final List<String> propertyNames;
	private final PropertyWizardDialogConfigurator propertiesConfig;
	private final List<PropertyDescription> dataproviderProperties;

	public AutoWizardTableColumnPropertyAccessor(PropertyWizardDialogConfigurator config)
	{
		this.propertiesConfig = config;
		this.propertyNames = propertiesConfig.getOrderedProperties().stream().filter(pd -> !propertiesConfig.getPrefillProperties().contains(pd))
			.map(pd -> pd.getName()).collect(Collectors.toList());
		if (propertiesConfig.getDataproviderProperties().size() > 0) propertyNames.add(0, propertiesConfig.getAutoPropertyName());
		dataproviderProperties = propertiesConfig.getDataproviderProperties();
	}

	@Override
	public Object getDataValue(Map<String, Object> row, int columnIndex)
	{
		if (columnIndex == propertyNames.size()) return ""; // the delete column
		if (propertiesConfig.getDataproviderProperties().size() > 0)
		{
			if (columnIndex == 0)
			{
				for (PropertyDescription pd : propertiesConfig.getDataproviderProperties())
				{
					if (row.get(pd.getName()) != null) return row.get(pd.getName());
				}
				return "";
			}
		}
		String propertyName = propertyNames.get(columnIndex);
		return row.get(propertyName);
	}

	@Override
	public void setDataValue(Map<String, Object> rowObject, int columnIndex, Object newValue)
	{
		if (columnIndex == propertyNames.size()) return; // the delete column
		Optional<PropertyDescription> dp = searchDataproviderProperty(getColumnProperty(columnIndex));
		if (dp.isPresent())
		{
			PropertyDescription dpProp = dp.get();
			Optional<PropertyDescription> prevValue = propertiesConfig.getDataproviderProperties().stream().filter(pd -> rowObject.get(pd.getName()) != null)
				.findAny();
			if (!prevValue.isPresent()) return; //ignore, for this column no dataprovider was set
			Object dpValue = rowObject.get(prevValue.get().getName());
			if (Utils.getAsBoolean(newValue))
			{
				for (PropertyDescription pd : dataproviderProperties)
				{
					if (!pd.getName().equals(dpProp.getName()))
					{
						rowObject.put(pd.getName(), pd.getDefaultValue());
					}
					else
					{
						rowObject.put(dpProp.getName(), dpValue);
					}
				}
			}
		}
		else
		{
			rowObject.put(getColumnProperty(columnIndex), newValue);
		}
	}

	private Optional<PropertyDescription> searchDataproviderProperty(String property)
	{
		return propertiesConfig.getDataproviderProperties().stream().filter(pd -> pd.getName().equals(property)).findAny();
	}

	@Override
	public int getColumnCount()
	{
		return propertyNames.size() + 1;
	}

	@Override
	public String getColumnProperty(int columnIndex)
	{
		if (columnIndex == propertyNames.size()) return ""; // the delete column
		return propertyNames.get(columnIndex);
	}

	@Override
	public int getColumnIndex(String propertyName)
	{
		return propertyNames.contains(propertyName) ? propertyNames.indexOf(propertyName) : propertyNames.size();
	}
}
