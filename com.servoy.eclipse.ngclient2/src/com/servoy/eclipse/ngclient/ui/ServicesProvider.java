package com.servoy.eclipse.ngclient.ui;

import java.util.HashSet;
import java.util.Set;

import org.apache.tomcat.starter.IServicesProvider;

public class ServicesProvider implements IServicesProvider
{
	@Override
	public void registerServices()
	{
	}

	@Override
	public Set<Class< ? >> getAnnotatedClasses(String context)
	{
		// ng client stuff are only reported for the root context in developer.
		if ("".equals(context))
		{
			HashSet<Class< ? >> set = new HashSet<Class< ? >>();
			set.add(IndexPageFilter.class);
			return set;
		}
		return null;
	}

}
