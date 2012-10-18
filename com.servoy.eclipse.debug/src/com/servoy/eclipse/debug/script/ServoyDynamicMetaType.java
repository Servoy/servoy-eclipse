/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.debug.script;

import org.eclipse.dltk.internal.javascript.ti.TypeSystemImpl;
import org.eclipse.dltk.javascript.typeinfo.DefaultMetaType;
import org.eclipse.dltk.javascript.typeinfo.ITypeSystem;
import org.eclipse.dltk.javascript.typeinfo.model.Type;

/**
 * @author jcompagner
 *
 */
public class ServoyDynamicMetaType extends DefaultMetaType
{
	static TypeSystemImpl SHARED_TYPE_SYSTEM = new TypeSystemImpl();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.typeinfo.MetaType#getId()
	 */
	public String getId()
	{
		return "ServoyDynamicType";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.typeinfo.DefaultMetaType#getPreferredTypeSystem(org.eclipse.dltk.javascript.typeinfo.model.Type)
	 */
	@Override
	public ITypeSystem getPreferredTypeSystem(Type type)
	{
		return SHARED_TYPE_SYSTEM;
	}
}
