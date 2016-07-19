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

package com.servoy.eclipse.designer.editor.rfb.menu;

import java.util.Iterator;

import org.eclipse.core.expressions.PropertyTester;

import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;

/**
 * @author gboros
 *
 */
public class IsMovablePropertyTester extends PropertyTester
{
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue)
	{
		IPersist persist = null;
		if (receiver instanceof PersistContext)
		{
			persist = ((PersistContext)receiver).getPersist();
		}
		else if (receiver instanceof IPersist)
		{
			persist = (IPersist)receiver;
		}

		if (persist != null)
		{
			Form form = (Form)persist.getAncestor(IRepository.FORMS);
			if (form != null && form.isResponsiveLayout())
			{
				ISupportChilds persistParent = persist.getParent();
				if (persistParent != null)
				{
					Iterator<IPersist> childIte = persistParent.getAllObjects();
					childIte.next();
					return childIte.hasNext();
				}
			}
		}

		return false;
	}

}
