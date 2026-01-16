/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

import org.eclipse.dltk.javascript.typeinfo.IRSimpleType;
import org.eclipse.dltk.javascript.typeinfo.IRType;
import org.eclipse.dltk.javascript.typeinfo.ITypeSystem;
import org.eclipse.dltk.javascript.typeinfo.MapOnType;
import org.eclipse.dltk.javascript.typeinfo.RSimpleType;
import org.eclipse.dltk.javascript.typeinfo.TypeCompatibility;
import org.eclipse.dltk.javascript.typeinfo.model.Type;

/**
 * @author jcompagner
 *
 */
public class SimpleTypeWithDefault extends RSimpleType implements MapOnType
{
	private final IRSimpleType typeToMap;

	/**
	 * @param typeSystem
	 * @param target
	 * @param typeToMap
	 */
	public SimpleTypeWithDefault(ITypeSystem typeSystem, Type target, IRSimpleType typeToMap)
	{
		super(typeSystem, target);
		this.typeToMap = typeToMap;
	}

	@Override
	public TypeCompatibility canMap(IRType type)
	{
		return type.equals(typeToMap) ? TypeCompatibility.TRUE : TypeCompatibility.FALSE;
	}
}
