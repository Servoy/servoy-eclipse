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

import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebComponentSpecification;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.rfb.GhostBean;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.j2db.persistence.Bean;
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

		String searchFor = uuid;
		if (searchFor.contains("_"))
		{
			String[] split = searchFor.split("_");
			if (split.length != 3) return null;
			String parentUUID = split[0];
			String fieldName = split[1];
			String typeName = split[2];
			int index = -1;
			Bean parentBean = (Bean)ModelUtils.getEditingFlattenedSolution(editorPart.getForm()).searchPersist(UUID.fromString(parentUUID));
			WebComponentSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(parentBean.getBeanClassName());
			if (fieldName.indexOf('[') > 0)
			{
				index = extractIndex(fieldName);
				fieldName = fieldName.substring(0, fieldName.indexOf('['));
			}
			boolean arrayReturnType = spec.isArrayReturnType(fieldName);

			Bean bean = new GhostBean(parentBean, fieldName, typeName, index, arrayReturnType, false);
			String compName = "bean_" + id.incrementAndGet();
			while (!checkName(editorPart, compName))
			{
				compName = "bean_" + id.incrementAndGet();
			}
			bean.setName(compName);
			bean.setBeanClassName(typeName);
			return bean;
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
		Iterator<IFormElement> fields = editorPart.getForm().getFormElementsSortedByFormIndex();
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
