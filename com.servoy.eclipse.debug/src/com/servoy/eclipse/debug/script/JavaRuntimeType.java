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

import org.eclipse.dltk.javascript.typeinfo.IRSimpleType;
import org.eclipse.dltk.javascript.typeinfo.IRType;
import org.eclipse.dltk.javascript.typeinfo.IRTypeDeclaration;
import org.eclipse.dltk.javascript.typeinfo.IRTypeTransformer;
import org.eclipse.dltk.javascript.typeinfo.ITypeSystem;
import org.eclipse.dltk.javascript.typeinfo.RSimpleType;
import org.eclipse.dltk.javascript.typeinfo.TypeCompatibility;
import org.eclipse.dltk.javascript.typeinfo.TypeQuery;
import org.eclipse.dltk.javascript.typeinfo.TypeUtil;
import org.eclipse.dltk.javascript.typeinfo.model.Type;

@SuppressWarnings("nls")
class JavaRuntimeType extends RSimpleType
{
	public static final String JAVA_CLASS = "JAVA_CLASS";

	/**
	 * @param typeSystem 
	 * @param type
	 */
	public JavaRuntimeType(ITypeSystem typeSystem, Type type)
	{
		super(typeSystem, type);
	}

	/**
	 * @param declaration
	 */
	public JavaRuntimeType(IRTypeDeclaration declaration)
	{
		super(declaration);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.typeinfo.IRType#isJavaScriptObject()
	 */
	@Override
	public boolean isJavaScriptObject()
	{
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.typeinfo.JSType2#isAssignableFrom(org.eclipse.dltk.javascript.typeinfo.JSType2)
	 */
	@Override
	public TypeCompatibility isAssignableFrom(IRType runtimeType)
	{
		if (runtimeType instanceof JavaRuntimeType)
		{
			Class< ? > cls = (Class< ? >)getTarget().getAttribute(JavaRuntimeType.JAVA_CLASS);
			Class< ? > other = (Class< ? >)((JavaRuntimeType)runtimeType).getTarget().getAttribute(JavaRuntimeType.JAVA_CLASS);
			return cls.isAssignableFrom(other) ? TypeCompatibility.TRUE : TypeCompatibility.FALSE;
		}
		if (runtimeType instanceof IRSimpleType)
		{
			Type src = ((IRSimpleType)runtimeType).getTarget();
			final String localName = TypeUtil.getName(getTarget());
			for (Type t : new TypeQuery(src).getHierarchy())
			{
				if (localName.equals(TypeUtil.getName(t))) return TypeCompatibility.TRUE;
			}
		}
		return TypeCompatibility.FALSE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj.getClass() == JavaRuntimeType.class)
		{
			return ((JavaRuntimeType)obj).getTarget().equals(getTarget());
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.typeinfo.IRType#transform(org.eclipse.dltk.javascript.typeinfo.IRTypeTransformer)
	 */
	@Override
	public IRType transform(IRTypeTransformer function)
	{
		final IRTypeDeclaration value = function.transform(getDeclaration());
		if (value != getDeclaration())
		{
			return new JavaRuntimeType(value);
		}
		else
		{
			return this;
		}
	}
}