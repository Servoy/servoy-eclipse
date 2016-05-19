package com.servoy.eclipse.core.nature;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

public class ResourcePropertyTester extends PropertyTester
{

	public final static String HAS_NATURE_BOOLEAN = "hasNature";

	public ResourcePropertyTester()
	{
		// nothing to do here; docs say a public 0 arg constructor is required - don't know if implicit is enough
		System.out.println("a");
	}

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue)
	{
		if (HAS_NATURE_BOOLEAN.equals(property) && receiver instanceof IProject && expectedValue instanceof String)
		{
			try
			{
				IProject proj = (IProject)receiver;
				return proj.isAccessible() && proj.hasNature((String)expectedValue);
			}
			catch (CoreException e)
			{
				return false;
			}
		}


		return false;
	}

}
