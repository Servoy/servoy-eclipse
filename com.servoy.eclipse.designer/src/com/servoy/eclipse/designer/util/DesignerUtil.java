/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.designer.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.EditPart;

import com.servoy.eclipse.designer.property.IPersistEditPart;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.persistence.IPersist;

/**
 * Utility methods for form designer.
 * 
 * @author rgansevles
 */

public class DesignerUtil
{
	public static List<EditPart> removeChildEditParts(List<EditPart> editParts)
	{
		List<EditPart> newEditParts = new ArrayList<EditPart>(editParts.size());

		for (EditPart editPart : editParts)
		{
			boolean foundParent = false;
			if (editPart instanceof IPersistEditPart)
			{
				IPersistEditPart persistEditpart = (IPersistEditPart)editPart;
				IPersist persist = persistEditpart.getPersist();
				for (EditPart editPart2 : editParts)
				{
					if (editPart2 instanceof IPersistEditPart && ((IPersistEditPart)editPart2).getPersist() == persist.getParent())
					{
						foundParent = true;
						break;
					}
				}
			}
			if (!foundParent) newEditParts.add(editPart);
		}

		return newEditParts;
	}


	/**
	 * @param awtDimension
	 * @return draw2d Dimension
	 */
	public static Dimension convertDimension(java.awt.Dimension awtDimension)
	{
		if (awtDimension == null)
		{
			return null;
		}
		return new Dimension(awtDimension.width, awtDimension.height);
	}

	public static boolean containsInheritedElement(List selectedEditParts)
	{
		if (selectedEditParts != null && !selectedEditParts.isEmpty() && selectedEditParts.get(0) instanceof EditPart)
		{
			for (int i = 0; i < selectedEditParts.size(); i++)
			{
				EditPart object = (EditPart)selectedEditParts.get(i);
				EditPart parent = object.getParent();
				if (parent != null && parent.getModel() instanceof IPersist &&
					ElementUtil.isInheritedFormElement((IPersist)parent.getModel(), object.getModel())) return true;
			}
		}
		return false;
	}

}
