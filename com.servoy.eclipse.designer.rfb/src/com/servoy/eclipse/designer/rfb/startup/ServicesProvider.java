package com.servoy.eclipse.designer.rfb.startup;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.annotation.WebServlet;

import org.apache.tomcat.starter.IServicesProvider;

import com.servoy.eclipse.designer.rfb.editor.FormEditorEndpoint;

@WebServlet("/rfb/*")
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
			set.add(FormEditorEndpoint.class);
			return set;
		}
		return null;
	}
}
