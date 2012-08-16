package com.servoy.eclipse.debug.script;

import org.eclipse.dltk.javascript.typeinfo.IRSimpleType;
import org.eclipse.dltk.javascript.typeinfo.IRType;
import org.eclipse.dltk.javascript.typeinfo.IRTypeFactory;
import org.eclipse.dltk.javascript.typeinfo.ITypeSystem;
import org.eclipse.dltk.javascript.typeinfo.TypeCompatibility;
import org.eclipse.dltk.javascript.typeinfo.TypeQuery;
import org.eclipse.dltk.javascript.typeinfo.TypeUtil;
import org.eclipse.dltk.javascript.typeinfo.model.JSType;
import org.eclipse.dltk.javascript.typeinfo.model.Type;

public class JavaClassRuntimeTypeFactory implements IRTypeFactory
{
	public static final String JAVA_CLASS = "JAVA_CLASS";


	public IRType create(Type type)
	{
		if (type.getAttribute(JAVA_CLASS) != null)
		{
			return new JavaRuntimeType(type);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.typeinfo.IRTypeFactory#create(org.eclipse.dltk.javascript.typeinfo.ITypeSystem,
	 * org.eclipse.dltk.javascript.typeinfo.model.JSType)
	 */
	public IRType create(ITypeSystem typeSystem, JSType type)
	{
		return null;
	}

	private static class JavaRuntimeType implements IRSimpleType
	{

		private final Type type;

		/**
		 * @param type
		 */
		public JavaRuntimeType(Type type)
		{
			this.type = type;
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
		 * @see org.eclipse.dltk.javascript.typeinfo.JSType2#isAssignableFrom(org.eclipse.dltk.javascript.typeinfo.JSType2)
		 */
		public TypeCompatibility isAssignableFrom(IRType runtimeType)
		{
			if (runtimeType instanceof JavaRuntimeType)
			{
				Class< ? > cls = (Class< ? >)type.getAttribute(JAVA_CLASS);
				Class< ? > other = (Class< ? >)((JavaRuntimeType)runtimeType).type.getAttribute(JAVA_CLASS);
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
			return null;
		}

	}
}
