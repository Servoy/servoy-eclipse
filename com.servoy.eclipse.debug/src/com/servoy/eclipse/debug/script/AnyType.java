package com.servoy.eclipse.debug.script;

import org.eclipse.dltk.javascript.typeinfo.IRType;
import org.eclipse.dltk.javascript.typeinfo.IRTypeTransformer;
import org.eclipse.dltk.javascript.typeinfo.ITypeSystem;
import org.eclipse.dltk.javascript.typeinfo.TypeCompatibility;

public class AnyType implements IRType
{
	public String getName()
	{
		return "Object";
	}

	public TypeCompatibility isAssignableFrom(IRType type)
	{
		return TypeCompatibility.TRUE;
	}

	public ITypeSystem activeTypeSystem()
	{
		return null;
	}

	public boolean isExtensible()
	{
		return true;
	}

	public boolean isJavaScriptObject()
	{
		return false;
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
	 * @see org.eclipse.dltk.javascript.typeinfo.IRType#isSynthetic()
	 */
	public boolean isSynthetic()
	{
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.typeinfo.IRType#normalize()
	 */
	public IRType normalize()
	{
		return this;
	}
}
