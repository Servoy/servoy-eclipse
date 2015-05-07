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
import com.servoy.j2db.persistence.IWebComponent;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.persistence.WebCustomType;
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

		String searchFor = uuid;
		if (searchFor.contains("_"))
		{
			String[] split = searchFor.split("_");
			if (split.length != 3) return null;
			String parentUUID = split[0];
			String fieldName = split[1];
			String typeName = split[2];
			int index = -1;
			IWebComponent parentBean = (IWebComponent)ModelUtils.getEditingFlattenedSolution(editorPart.getForm()).searchPersist(UUID.fromString(parentUUID));

			if (fieldName.indexOf('[') > 0)
			{
				index = extractIndex(fieldName);
				fieldName = fieldName.substring(0, fieldName.indexOf('['));
			}

			if (parentBean instanceof WebComponent)
			{
				Object propertyValue = ((WebComponent)parentBean).getProperty(fieldName);
				return index == -1 ? (WebCustomType)propertyValue : ((WebCustomType[])propertyValue)[index];
			}
			else
			{
				WebCustomType bean = new WebCustomType(parentBean, fieldName, typeName, index, false);
				String compName = "bean_" + id.incrementAndGet();
				while (!checkName(editorPart, compName))
				{
					compName = "bean_" + id.incrementAndGet();
				}
				bean.setName(compName);
				bean.setTypeName(typeName);
				return bean;
			}
		}
		return ModelUtils.getEditingFlattenedSolution(editorPart.getForm()).searchPersist(UUID.fromString(searchFor));
	}

	/**
	 * @param dropTargetFieldName
	 * @return
	 */
	private int extractIndex(String dropTargetFieldName)
	{
		int index = -1;
		if (dropTargetFieldName.indexOf('[') > 0)
		{
			index = Integer.parseInt(dropTargetFieldName.substring(dropTargetFieldName.indexOf('[') + 1, dropTargetFieldName.indexOf(']')));
		}
		return index;
	}

	/**
	 * @param compName
	 */
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
