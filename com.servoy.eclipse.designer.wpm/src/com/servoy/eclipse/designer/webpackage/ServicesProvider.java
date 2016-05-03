package com.servoy.eclipse.designer.webpackage;

import java.util.HashSet;
import java.util.Set;

import org.apache.tomcat.starter.IServicesProvider;

import com.servoy.eclipse.designer.webpackage.WebPackageManagerResourcesServlet;
import com.servoy.eclipse.designer.webpackage.endpoint.WebPackageManagerEndpoint;


public class ServicesProvider implements IServicesProvider
{
	public void registerServices()
	{
	}

	public Set<Class< ? >> getAnnotatedClasses(String context)
	{
		// wpm stuff are only reported for the root context in developer.
		if ("".equals(context))
		{
			HashSet<Class< ? >> set = new HashSet<Class< ? >>();
			set.add(WebPackageManagerResourcesServlet.class);
			set.add(WebPackageManagerEndpoint.class);
			return set;
		}
		return null;
	}
}
