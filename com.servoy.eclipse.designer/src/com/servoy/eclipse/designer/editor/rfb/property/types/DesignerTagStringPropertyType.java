/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.eclipse.designer.editor.rfb.property.types;

import org.json.JSONException;
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IPropertyType;
import org.sablo.websocket.utils.DataConversion;

import com.servoy.eclipse.core.Activator;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.property.types.TagStringPropertyType;

/**
 * @author jcompagner
 *
 */
public class DesignerTagStringPropertyType extends TagStringPropertyType
{
	public static final IPropertyType< ? > DESIGNER_INSTANCE = new DesignerTagStringPropertyType();

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.property.types.TagStringPropertyType#toTemplateJSONValue(org.json.JSONWriter, java.lang.String, java.lang.String,
	 * org.sablo.specification.PropertyDescription, org.sablo.websocket.utils.DataConversion, com.servoy.j2db.server.ngclient.FormElementContext)
	 */
	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, String formElementValue, PropertyDescription pd,
		DataConversion browserConversionMarkers, FormElementContext formElementContext) throws JSONException
	{
		// special case for in the designer, value in template will let this true.
		if (formElementValue.startsWith("i18n:") && formElementContext.getFormElement().getDesignId() != null)
		{
			return super.toTemplateJSONValue(writer, key, Activator.getDefault().getDesignClient().getI18NMessage(formElementValue), pd,
				browserConversionMarkers, formElementContext);
		}
		return super.toTemplateJSONValue(writer, key, formElementValue, pd, browserConversionMarkers, formElementContext);
	}

	@Override
	public boolean valueInTemplate(String formElementVal, PropertyDescription pd, FormElementContext formElementContext)
	{
		return formElementContext.getFormElement().getDesignId() != null ? true : super.valueInTemplate(formElementVal, pd, formElementContext);
	}

}
