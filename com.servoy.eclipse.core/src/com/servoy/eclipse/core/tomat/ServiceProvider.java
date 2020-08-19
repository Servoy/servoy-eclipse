package com.servoy.eclipse.core.tomat;

import java.util.HashSet;
import java.util.Set;

import org.apache.tomcat.starter.IServicesProvider;

public class ServiceProvider implements IServicesProvider
{
	public Set<Class< ? >> getAnnotatedClasses(String context)
	{
		if ("".equals(context))
		{
			HashSet<Class< ? >> set = new HashSet<Class< ? >>();
			set.add(TomcatTesterServlet.class);
			return set;
		}
		return null;
	}
}
