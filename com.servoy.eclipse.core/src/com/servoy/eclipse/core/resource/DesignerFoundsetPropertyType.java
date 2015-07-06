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

import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;

import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.design.DesignNGClient;
import com.servoy.j2db.server.ngclient.property.FoundsetPropertyType;
import com.servoy.j2db.server.ngclient.property.FoundsetTypeSabloValue;

/**
 * @author lvostinar
 *
 */
public class DesignerFoundsetPropertyType extends FoundsetPropertyType
{
	public static final DesignerFoundsetPropertyType DESIGNER_INSTANCE = new DesignerFoundsetPropertyType(null);

	public DesignerFoundsetPropertyType(PropertyDescription definition)
	{
		super(definition);
	}

	@Override
	public FoundsetTypeSabloValue toSabloComponentValue(JSONObject formElementValue, PropertyDescription pd, INGFormElement formElement,
		WebFormComponent component, final DataAdapterList dal)
	{
		return new FoundsetTypeSabloValue(formElementValue, pd.getName(), dal, dal.getApplication())
		{
			@Override
			public void updateFoundset(IFoundSetInternal newFoundset)
			{
				if (dal.getApplication() instanceof DesignNGClient && !((DesignNGClient)dal.getApplication()).getShowData())
				{
					super.updateFoundset((IFoundSetInternal)null);
					return;
				}
				super.updateFoundset(newFoundset);
			}

			@Override
			public IFoundSetInternal getFoundset()
			{
				if (getFormUI().getDataConverterContext().getApplication() instanceof DesignNGClient &&
					!((DesignNGClient)getFormUI().getDataConverterContext().getApplication()).getShowData())
				{
					return null;
				}
				return super.getFoundset();
			}
		};
	}
}
