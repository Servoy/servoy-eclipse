/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.eclipse.core.resource;

import java.util.ArrayList;

import org.sablo.specification.PropertyDescription;

import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.design.DesignNGClient;
import com.servoy.j2db.server.ngclient.property.ValueListConfig;
import com.servoy.j2db.server.ngclient.property.types.ValueListPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ValueListTypeSabloValue;

/**
 * @author gganea
 *
 */
public class DesignerValueListPropertyType extends ValueListPropertyType
{

	public static final DesignerValueListPropertyType DESIGNER_INSTANCE = new DesignerValueListPropertyType();

	/**
	*
	*/
	protected DesignerValueListPropertyType()
	{
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.ngclient.property.types.ValueListPropertyType#toSabloComponentValue(java.lang.Object,
	 * org.sablo.specification.PropertyDescription, com.servoy.j2db.server.ngclient.INGFormElement, com.servoy.j2db.server.ngclient.WebFormComponent,
	 * com.servoy.j2db.server.ngclient.DataAdapterList)
	 */
	@Override
	public ValueListTypeSabloValue toSabloComponentValue(final Object formElementValue, final PropertyDescription pd, final INGFormElement formElement,
		final WebFormComponent component, DataAdapterList dataAdapterList)
	{
		if (dataAdapterList.getApplication() instanceof DesignNGClient)
		{
			ValueList val = null;
			IValueList valueList = null;
			ValueListConfig config = (ValueListConfig)pd.getConfig();
			String dataproviderID = (pd.getConfig() != null ? (String)formElement.getPropertyValue(config.getFor()) : null);

			valueList = getIValueList(formElementValue, pd, formElement, component, dataAdapterList, val, valueList, config, dataproviderID);

			return valueList != null ? new ValueListTypeSabloValue(valueList, dataAdapterList, config, dataproviderID, pd)
			{
				@Override
				protected java.util.List<java.util.Map<String, Object>> getJavaValueForJSON()
				{
					if (dataAdapterList.getApplication() instanceof DesignNGClient && !((DesignNGClient)dataAdapterList.getApplication()).getShowData())
					{
						return new ArrayList<>();
					}
					else
					{
						setValueList(getIValueList(formElementValue, pd, formElement, component, dataAdapterList, null, valueList, config, dataproviderID));
						return super.getJavaValueForJSON();
					}
				}
			} : null;
		}
		else
		{
			return super.toSabloComponentValue(formElementValue, pd, formElement, component, dataAdapterList);
		}
	}

	@Override
	protected IValueList getRealValueList(INGApplication application, ValueList val, ComponentFormat fieldFormat, String dataproviderID)
	{
		return application instanceof DesignNGClient ? com.servoy.j2db.component.ComponentFactory.getRealValueList(application, val, true, fieldFormat.dpType,
			fieldFormat.parsedFormat, dataproviderID, true) : super.getRealValueList(application, val, fieldFormat, dataproviderID);
	}
}
