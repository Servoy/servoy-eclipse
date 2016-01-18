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

package com.servoy.eclipse.designer.editor.rfb.actions.handlers;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.gef.commands.Command;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.j2db.persistence.IChildWebObject;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.WebCustomType;

/**
 * @author gganea@servoy.com
 *
 */
public class MoveCustomTypeCommand extends Command
{


	private final WebCustomType source;
	private final Object value;
	private IChildWebObject[] oldValue;

	/**
	 * @param label
	 */
	public MoveCustomTypeCommand(WebCustomType source, Object value)
	{
		super("Move web custom type");
		this.source = source;
		this.value = value;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.gef.commands.Command#execute()
	 */
	@Override
	public void execute()
	{
		LocationCache.getINSTANCE().putLocation(source.getParent().getUUID() + source.getJsonKey(), source.getUUID().toString(), (Point)value);
		reSortCustomTypes();
	}

	/**
	 * Resort the array of webcustomtypes that the given custom type is part of.
	 * @param parent
	 * @param source
	 */
	private void reSortCustomTypes()
	{
		String jsonKey = source.getJsonKey();
		IChildWebObject[] property = (IChildWebObject[])source.getParent().getProperty(jsonKey);


		ArrayList<IChildWebObject> oldValueAsList = new ArrayList<>();
		Collections.addAll(oldValueAsList, property);
		this.oldValue = oldValueAsList.toArray(new IChildWebObject[oldValueAsList.size()]); // to really save the old order, we need to create a new array

		List<IChildWebObject> asList = Arrays.asList(property);//sorting the list means sorting the array in place, no need to set it back again
		Collections.sort(asList, new Comparator<IChildWebObject>()
		{
			@Override
			public int compare(IChildWebObject o1, IChildWebObject o2)
			{
				String o1key = o1.getUUID().toString();
				String parentKey = o1.getParent().getUUID() + o1.getJsonKey();
				String o2key = o2.getUUID().toString();
				Point o1Location = LocationCache.getINSTANCE().getLocation(parentKey, o1key);
				Point o2Location = LocationCache.getINSTANCE().getLocation(parentKey, o2key);
				if (o1Location != null && o2Location != null) return PositionComparator.comparePoint(true, o1Location, o2Location);
				return 0;
			}
		});

		setIndexes(property);
		source.getParent().setProperty(jsonKey, asList.toArray());
	}

	/** Sets the correct index in the array for each IChildWebObject
	 * @param newValue2
	 */
	private void setIndexes(IChildWebObject[] array)
	{
		for (int i = 0; i < array.length; i++)
		{
			array[i].setIndex(i);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	@Override
	public void undo()
	{
		setIndexes(oldValue);
		source.getParent().setProperty(source.getJsonKey(), oldValue);
		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, source.getParent(), true);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.gef.commands.Command#redo()
	 */
	@Override
	public void redo()
	{
		super.redo();
		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, source.getParent(), true);
	}
}
