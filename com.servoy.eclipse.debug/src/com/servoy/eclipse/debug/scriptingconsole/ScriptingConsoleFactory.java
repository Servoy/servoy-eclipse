package com.servoy.eclipse.debug.scriptingconsole;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.debug.ui.display.IEvaluateConsole;
import org.eclipse.dltk.debug.ui.display.IEvaluateConsoleFactory;
import org.eclipse.jface.resource.ImageDescriptor;

import com.servoy.eclipse.debug.Activator;

public class ScriptingConsoleFactory implements IEvaluateConsoleFactory
{

	private ScriptConsole scriptConsole;

	public ScriptingConsoleFactory()
	{
	}

	public boolean isEnabled()
	{
		return true;
	}

	public IEvaluateConsole create()
	{
		if (scriptConsole == null) scriptConsole = new ScriptConsole();
		return scriptConsole;
	}

	public String getLabel()
	{
		return "Live Scripting Console";
	}

	public ImageDescriptor getImageDescriptor()
	{
		return ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path("/icons/launch.gif"), null));
	}

}
