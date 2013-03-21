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

import org.eclipse.dltk.javascript.typeinfo.DefaultMetaType;
import org.eclipse.dltk.javascript.typeinfo.IRType;
import org.eclipse.dltk.javascript.typeinfo.IRTypeDeclaration;
import org.eclipse.dltk.javascript.typeinfo.ITypeSystem;
import org.eclipse.dltk.javascript.typeinfo.model.Type;

/**
 * @author jcompagner
 *
 */
final class JavaRuntimeMetaType extends DefaultMetaType
{
	private final ITypeSystem typeSystem;

	public JavaRuntimeMetaType(ITypeSystem typeSystem)
	{
		this.typeSystem = typeSystem;
	}

	@Override
	public IRType toRType(ITypeSystem typeSystem, Type type)
	{
		return new JavaRuntimeType(typeSystem, type);
	}

	public String getId()
	{
		return "JavaType";
	}

	@Override
	public IRType toRType(IRTypeDeclaration declaration)
	{
		return new JavaRuntimeType(declaration);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.typeinfo.DefaultMetaType#getPreferredTypeSystem(org.eclipse.dltk.javascript.typeinfo.model.Type)
	 */
	@Override
	public ITypeSystem getPreferredTypeSystem(Type type)
	{
		return typeSystem;
	}
}