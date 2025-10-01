/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

import java.util.HashSet;
import java.util.Map;

import org.eclipse.dltk.javascript.typeinfo.IJavaScriptLikeObject;
import org.eclipse.dltk.javascript.typeinfo.IRArrayType;
import org.eclipse.dltk.javascript.typeinfo.IRType;
import org.eclipse.dltk.javascript.typeinfo.IRTypeDeclaration;
import org.eclipse.dltk.javascript.typeinfo.ITypeSystem;
import org.eclipse.dltk.javascript.typeinfo.ImmutableType;
import org.eclipse.dltk.javascript.typeinfo.RSimpleType;
import org.eclipse.dltk.javascript.typeinfo.TypeCompatibility;
import org.eclipse.dltk.javascript.typeinfo.model.Type;

/**
 * @author jcompagner
 * @since 2024.06
 *
 */
public class ServoyDynamicArrayRuntimeType extends RSimpleType implements IJavaScriptLikeObject, IRArrayType
{
	private final IRType itemType;

	/**
	 * @param declaration
	 */
	public ServoyDynamicArrayRuntimeType(IRTypeDeclaration declaration, IRType itemType)
	{
		super(declaration);
		this.itemType = itemType;
	}

	public ServoyDynamicArrayRuntimeType(ITypeSystem typeSystem, Type type, IRType itemType)
	{
		super(typeSystem, type);
		this.itemType = itemType;
	}

	@Override
	public IRType getItemType()
	{
		return itemType;
	}

	@Override
	public TypeCompatibility isAssignableFrom(IRType type)
	{
		TypeCompatibility compatible = super.isAssignableFrom(type);
		if (!compatible.ok() && type instanceof ServoyDynamicArrayRuntimeType)
		{
			IRTypeDeclaration other = ((RSimpleType)type).getDeclaration();
			return ServoyDynamicRuntimeType.isAssignableFrom(getDeclaration(), other, new HashSet<IRTypeDeclaration>());
		}
		return compatible;
	}

	@Override
	public IRArrayType makeImmutable(Map<Object, Object> visited)
	{
		IRTypeDeclaration declaration = getDeclaration();
		if (declaration instanceof ImmutableType< ? > || itemType instanceof ImmutableType< ? >)
		{
			IRType type = itemType;
			if (type instanceof ImmutableType< ? > t)
			{
				type = (IRType)t.makeImmutable(visited);
			}
			if (declaration instanceof ImmutableType< ? > t)
			{
				declaration = (IRTypeDeclaration)t.makeImmutable(visited);
			}
			return new ServoyDynamicArrayRuntimeType(declaration, type);
		}
		return this;
	}

}
