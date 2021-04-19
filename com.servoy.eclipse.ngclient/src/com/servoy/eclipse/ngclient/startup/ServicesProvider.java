package com.servoy.eclipse.ngclient.startup;

import java.util.HashSet;
import java.util.Set;

import org.apache.tomcat.starter.ClassServiceFactory;
import org.apache.tomcat.starter.IServicesProvider;
import org.osgi.framework.BundleContext;
import org.sablo.filter.SeparateSessionFilter;

import com.servoy.eclipse.ngclient.startup.resourceprovider.DeveloperMediaResourcesServlet;
import com.servoy.eclipse.ngclient.startup.resourceprovider.ResourceProvider;
import com.servoy.j2db.server.main.Activator;
import com.servoy.j2db.server.ngclient.ClientFunctionsServlet;
import com.servoy.j2db.server.ngclient.ComponentsModuleGenerator;
import com.servoy.j2db.server.ngclient.NGClientEntryFilter;
import com.servoy.j2db.server.ngclient.SelectNGSolutionFilter;
import com.servoy.j2db.server.ngclient.endpoint.NGClientEndpoint;
import com.servoy.j2db.server.ngclient.endpoint.RecordingEndpoint;


public class ServicesProvider implements IServicesProvider
{
	@Override
	public void registerServices()
	{
		BundleContext context = Activator.getContext();
		context.registerService(SeparateSessionFilter.class.getName(), new ClassServiceFactory(SeparateSessionFilter.class), null);
	}

	@Override
	public Set<Class< ? >> getAnnotatedClasses(String context)
	{
		// ng client stuff are only reported for the root context in developer.
		if ("".equals(context))
		{
			HashSet<Class< ? >> set = new HashSet<Class< ? >>();
			set.add(DeveloperMediaResourcesServlet.class);
			set.add(ComponentsModuleGenerator.class);
			set.add(NGClientEntryFilter.class);
			set.add(NGClientEndpoint.class);
			set.add(RecordingEndpoint.class);
			set.add(ResourceProvider.class);
			set.add(SelectNGSolutionFilter.class);
			set.add(ClientFunctionsServlet.class);
			return set;
		}
		return null;
	}
}
