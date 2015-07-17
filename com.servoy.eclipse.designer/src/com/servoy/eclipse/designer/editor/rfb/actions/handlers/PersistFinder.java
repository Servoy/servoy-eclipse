/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * @author user
 *
 */
public class PersistFinder
{

	static
	{
		INSTANCE = new PersistFinder();
	}
	public static PersistFinder INSTANCE;
	private final AtomicInteger id = new AtomicInteger();

	private PersistFinder()
	{
	}

	public IPersist searchForPersist(BaseVisualFormEditor editorPart, String uuid)
	{
		if (uuid == null) return null;
//		if (!uuid.contains("_"))
//		{
		return ModelUtils.getEditingFlattenedSolution(editorPart.getForm()).searchPersist(UUID.fromString(uuid));
//		}
//		else
//		{
//			// THIS 'else' code is only for deprecated usage of WebComponents represented as Bean persist (instead of new WebComponent persist)
//			// developer will not generate this type of persist now - it is just for solutions already built on 8.0 alphas and some of the betas
//			// that relied on Bean.getBeanXML to store the WebComponent's design json

//          // this will not work unless WebCustomType equals and getUUIDAsString are re-instated as well I think
//
//			String searchFor = uuid;
//			String[] split = searchFor.split("_");
//			if (split.length != 3) return null;
//			String parentUUID = split[0];
//			String fieldName = split[1];
//			String typeName = split[2];
//			int index = -1;
//			IWebComponent parentBean = (IWebComponent)ModelUtils.getEditingFlattenedSolution(editorPart.getForm()).searchPersist(UUID.fromString(parentUUID));
//
//			if (fieldName.indexOf('.') > 0)
//			{
//				index = extractIndex(fieldName);
//				fieldName = fieldName.substring(0, fieldName.indexOf('.'));
//			}
//
//			// only Bean instances should produce this kind of uuids
////			if (parentBean instanceof Bean)
////			{
//			Bean bean = (Bean)parentBean;
//			PropertyDescription pd = bean.getPropertyDescription().getProperty(fieldName);
//			if (index != -1) pd = ((CustomJSONPropertyType< ? >)pd.getType()).getCustomJSONTypeDefinition(); // element type of array
//			WebCustomType customType = new WebCustomType(bean, pd, fieldName, index, false); // because we create a new persist here editing it will not work; at this time a warning message is already printed in the developer log that the bean should be re-added to form for that (to be created as WebComponent persist instead)
//			String customPropName = "customProp_" + id.incrementAndGet();
//			while (!checkName(editorPart, customPropName))
//			{
//				customPropName = "customProp_" + id.incrementAndGet();
//			}
//			customType.setName(customPropName);
//			customType.setTypeName(typeName);
//			return customType;
////			}
//		}
	}

	private int extractIndex(String dropTargetFieldName)
	{
		int index = -1;
		if (dropTargetFieldName.indexOf('.') > 0)
		{
			index = Integer.parseInt(dropTargetFieldName.substring(dropTargetFieldName.indexOf('.') + 1, dropTargetFieldName.length()));
		}
		return index;
	}

	public boolean checkName(BaseVisualFormEditor editorPart, String compName)
	{
		Iterator<IFormElement> fields = editorPart.getForm().getFlattenedObjects(null).iterator();
		for (IFormElement element : Utils.iterate(fields))
		{
			if (compName.equals(element.getName()))
			{
				return false;
			}
		}
		return true;
	}


}
