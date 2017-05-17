package com.servoy.eclipse.firststeps;

import java.util.HashSet;
import java.util.Set;

import org.apache.tomcat.starter.IServicesProvider;

public class ServicesProvider implements IServicesProvider
{
	public void registerServices()
	{
	}

	public Set<Class< ? >> getAnnotatedClasses(String context)
	{
		if ("".equals(context))
		{
			HashSet<Class< ? >> set = new HashSet<Class< ? >>();
			set.add(ResourcesServlet.class);
			return set;
		}
		return null;
	}
}
