/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

package com.servoy.eclipse.designer.rfb.startup;

import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.specification.property.IPropertyType;
import org.sablo.websocket.utils.PropertyUtils;

import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.property.ComponentPropertyType;

/**
 * Utility methods that both c.s.e.designer and c.s.e.designer.rfb need
 *
 * @author acostescu
 */
public class RFBDesignerUtils
{

	public static boolean isDroppable(PropertyDescription propertyDescription, Object rootPropConfigObject)
	{
		return isDroppable(propertyDescription, rootPropConfigObject, false);
	}

	public static boolean isDroppable(PropertyDescription propertyDescription, Object rootPropConfigObject, boolean onlyConfigObjects)
	{
		IPropertyType< ? > type = propertyDescription.getType();
		return PropertyUtils.isCustomJSONArrayPropertyType(type)
			? isDroppableElement(((CustomJSONArrayType< ? , ? >)type).getCustomJSONTypeDefinition(), rootPropConfigObject, onlyConfigObjects)
			: isDroppableElement(propertyDescription, rootPropConfigObject, onlyConfigObjects);
	}

	public static boolean isDroppableElement(PropertyDescription propertyDescription, Object rootPropConfigObject, boolean onlyConfigObjects)
	{
		IPropertyType< ? > type = propertyDescription.getType();
		return (!onlyConfigObjects && (propertyDescription instanceof WebObjectSpecification || type instanceof ComponentPropertyType)) ||
			(rootPropConfigObject instanceof JSONObject && Boolean.TRUE.equals(((JSONObject)rootPropConfigObject).opt(FormElement.DROPPABLE)) &&
				PropertyUtils.isCustomJSONObjectProperty(type));
	}

}
