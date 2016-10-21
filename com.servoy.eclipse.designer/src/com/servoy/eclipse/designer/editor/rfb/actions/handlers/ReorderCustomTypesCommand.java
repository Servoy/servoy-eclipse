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

package com.servoy.eclipse.designer.editor.rfb.actions.handlers;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.gef.commands.Command;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.ICustomType;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.j2db.persistence.IBasicWebObject;
import com.servoy.j2db.persistence.IChildWebObject;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.persistence.WebCustomType;

/**
 * @author gganea
 *
 */
public class ReorderCustomTypesCommand extends Command
{

	public ReorderCustomTypesCommand(WebCustomType inPlaceOf, WebCustomType dropped)
	{
		super("Reorder Parent");
		this.inPlaceOf = inPlaceOf;
		this.dropped = dropped;
	}

	private final WebCustomType inPlaceOf;
	private final WebCustomType dropped;

	private IChildWebObject[] oldValue;

	@Override
	public void execute()
	{
		IBasicWebObject parent = inPlaceOf.getParent();
		reorder(parent);
	}


	private WebCustomType reorder(IBasicWebObject parentBean)
	{
		int index = inPlaceOf.getIndex();
		String propertyName = inPlaceOf.getJsonKey();
		WebObjectSpecification spec = WebComponentSpecProvider.getInstance().getSpecProviderState().getWebComponentSpecification(parentBean.getTypeName());
		boolean isArray = spec.isArrayReturnType(propertyName);
		PropertyDescription targetPD = spec.getProperty(propertyName);
		IChildWebObject[] arrayValue = null;
		if (isArray)
		{
			targetPD = ((ICustomType< ? >)targetPD.getType()).getCustomJSONTypeDefinition();
			if (parentBean instanceof WebComponent)
			{
				arrayValue = (IChildWebObject[])((WebComponent)parentBean).getProperty(propertyName);
			}
			if (index == -1) index = arrayValue != null ? arrayValue.length : 0;

			oldValue = arrayValue;
			if (parentBean instanceof WebComponent)
			{
				if (index > -1)
				{
					ArrayList<IChildWebObject> arrayList = new ArrayList<IChildWebObject>();
					arrayList.addAll(Arrays.asList(arrayValue));
					arrayList.remove(dropped);
					arrayList.add(index, dropped);
					arrayValue = arrayList.toArray(new IChildWebObject[arrayList.size()]);
				}
				//don't forget to set the correct indexes.
				for (int i = 0; i < arrayValue.length; i++)
				{
					arrayValue[i].setIndex(i);
				}
				((WebComponent)parentBean).setProperty(propertyName, arrayValue);
			}
			parentBean.updateJSON();
			return dropped;
		}
		return null;
	}

	@Override
	public void undo()
	{
		ArrayList<IPersist> changes = new ArrayList<IPersist>();
		changes.add(dropped.getParent());
		dropped.getParent().setProperty(dropped.getJsonKey(), oldValue);
		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, changes);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.gef.commands.Command#redo()
	 */
	@Override
	public void redo()
	{
		ArrayList<IPersist> changes = new ArrayList<IPersist>();
		changes.add(dropped.getParent());
		super.redo();
		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, changes);
	}
}
