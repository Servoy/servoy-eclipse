package com.servoy.eclipse.debug.scriptingconsole;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.debug.ui.display.IEvaluateConsole;
import org.eclipse.dltk.debug.ui.display.IEvaluateConsoleFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

import com.servoy.eclipse.debug.Activator;

public class ScriptingConsoleFactory implements IEvaluateConsoleFactory
{

	public boolean isEnabled()
	{
		return true;
	}

	public IEvaluateConsole create()
	{
		ScriptConsole scriptConsole = new ScriptConsole()
		{
			/*
			 * (non-Javadoc)
			 * 
			 * @see com.servoy.eclipse.debug.scriptingconsole.ScriptConsole#dispose()
			 */
			@Override
			public void dispose()
			{
				XMLMemento xmlMemento = XMLMemento.createWriteRoot(ScriptingConsole.SCRIPTINGCONSOLE_SETTING);
				saveState(xmlMemento);
				StringWriter writer = new StringWriter();
				try
				{
					xmlMemento.save(writer);
					Activator.getDefault().getDialogSettings().put(ScriptingConsole.SCRIPTINGCONSOLE_SETTING, writer.getBuffer().toString());
				}
				catch (IOException e)
				{
					// don't do anything. Simply don't store the settings
				}
				super.dispose();
			}
		};

		String persistedMemento = Activator.getDefault().getDialogSettings().get(ScriptingConsole.SCRIPTINGCONSOLE_SETTING);
		if (persistedMemento != null)
		{
			try
			{
				XMLMemento memento = XMLMemento.createReadRoot(new StringReader(persistedMemento));
				scriptConsole.restoreState(memento);
			}
			catch (WorkbenchException e)
			{
				// don't do anything. Simply don't restore the settings
			}
		}
		return scriptConsole;
	}

	public String getLabel()
	{
		return "Command Console";
	}

	public ImageDescriptor getImageDescriptor()
	{
		return ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path("/icons/commandconsole.gif"), null));
	}

}
