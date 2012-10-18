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
import org.eclipse.dltk.javascript.typeinfo.IRSimpleType;
import org.eclipse.dltk.javascript.typeinfo.IRType;
import org.eclipse.dltk.javascript.typeinfo.IRTypeDeclaration;
import org.eclipse.dltk.javascript.typeinfo.IRTypeTransformer;
import org.eclipse.dltk.javascript.typeinfo.ITypeSystem;
import org.eclipse.dltk.javascript.typeinfo.MetaType;
import org.eclipse.dltk.javascript.typeinfo.TypeCompatibility;
import org.eclipse.dltk.javascript.typeinfo.TypeQuery;
import org.eclipse.dltk.javascript.typeinfo.TypeUtil;
import org.eclipse.dltk.javascript.typeinfo.model.Type;

@SuppressWarnings("nls")
class JavaRuntimeType implements IRSimpleType
{
	public static final String JAVA_CLASS = "JAVA_CLASS";

	public static final MetaType JAVA_META_TYPE = new DefaultMetaType()
	{
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
		public IRType toRType(ITypeSystem typeSystem, IRTypeDeclaration declaration)
		{
			return new JavaRuntimeType(typeSystem, declaration);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.dltk.javascript.typeinfo.DefaultMetaType#getPreferredTypeSystem(org.eclipse.dltk.javascript.typeinfo.model.Type)
		 */
		@Override
		public ITypeSystem getPreferredTypeSystem(Type type)
		{
			// TODO Auto-generated method stub
			return super.getPreferredTypeSystem(type);
		}
	};

	private final Type type;

	private final ITypeSystem typeSystem;

	private final IRTypeDeclaration typeDeclaration;

	/**
	 * @param typeSystem 
	 * @param type
	 */
	public JavaRuntimeType(ITypeSystem typeSystem, Type type)
	{
		this.typeSystem = typeSystem;
		this.type = type;
		this.typeDeclaration = convert(type);
	}

	/**
	 * @param typeSystem2
	 * @param declaration
	 */
	public JavaRuntimeType(ITypeSystem typeSystem, IRTypeDeclaration declaration)
	{
		this.typeSystem = typeSystem;
		this.type = declaration.getSource();
		this.typeDeclaration = declaration;

	}

	protected final IRTypeDeclaration convert(Type type)
	{
		return typeSystem != null ? typeSystem.convert(type) : null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.typeinfo.model.JSType#getName()
	 */
	public String getName()
	{
		return type.getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.typeinfo.IRType#isExtensible()
	 */
	public boolean isExtensible()
	{
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.typeinfo.IRType#isJavaScriptObject()
	 */
	public boolean isJavaScriptObject()
	{
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.typeinfo.JSType2#isAssignableFrom(org.eclipse.dltk.javascript.typeinfo.JSType2)
	 */
	public TypeCompatibility isAssignableFrom(IRType runtimeType)
	{
		if (runtimeType instanceof JavaRuntimeType)
		{
			Class< ? > cls = (Class< ? >)type.getAttribute(JavaRuntimeType.JAVA_CLASS);
			Class< ? > other = (Class< ? >)((JavaRuntimeType)runtimeType).type.getAttribute(JavaRuntimeType.JAVA_CLASS);
			return cls.isAssignableFrom(other) ? TypeCompatibility.TRUE : TypeCompatibility.FALSE;
		}
		if (runtimeType instanceof IRSimpleType)
		{
			Type src = ((IRSimpleType)runtimeType).getTarget();
			final String localName = TypeUtil.getName(type);
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
	 * @see org.eclipse.dltk.javascript.typeinfo.model.ClassType#getTarget()
	 */
	public Type getTarget()
	{
		return type;
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
			return ((JavaRuntimeType)obj).type.equals(type);
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		return type.hashCode();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.typeinfo.IRType#activeTypeSystem()
	 */
	public ITypeSystem activeTypeSystem()
	{
		return typeSystem;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.typeinfo.IRType#transform(org.eclipse.dltk.javascript.typeinfo.IRTypeTransformer)
	 */
	public IRType transform(IRTypeTransformer function)
	{
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.typeinfo.IRSimpleType#getDeclaration()
	 */
	public IRTypeDeclaration getDeclaration()
	{
		return typeDeclaration;
	}

}