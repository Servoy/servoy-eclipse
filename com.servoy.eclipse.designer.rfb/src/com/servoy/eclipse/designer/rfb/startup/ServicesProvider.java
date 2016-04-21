package com.servoy.eclipse.designer.rfb.startup;

import java.util.HashSet;
import java.util.Set;

import org.apache.tomcat.starter.IServicesProvider;

import com.servoy.eclipse.designer.rfb.endpoint.EditorContentEndpoint;
import com.servoy.eclipse.designer.rfb.endpoint.EditorEndpoint;
import com.servoy.eclipse.designer.webpackage.WebPackageManagerResourcesServlet;
import com.servoy.eclipse.designer.webpackage.endpoint.WebPackageManagerEndpoint;


public class ServicesProvider implements IServicesProvider
{
	public void registerServices()
	{
	}

	public Set<Class< ? >> getAnnotatedClasses(String context)
	{
		// rfb stuff are only reported for the root context in developer.
		if ("".equals(context))
		{
			HashSet<Class< ? >> set = new HashSet<Class< ? >>();
			set.add(ResourcesServlet.class);
			set.add(EditorContentFilter.class);
			set.add(EditorEndpoint.class);
			set.add(EditorContentEndpoint.class);
			set.add(DesignerFilter.class);
			set.add(WebPackageManagerResourcesServlet.class);
			set.add(WebPackageManagerEndpoint.class);
			return set;
		}
		return null;
	}
}
