package com.servoy.eclipse.ui.actions;

import java.net.URL;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;

public class OpenAdminPage implements IWorkbenchWindowActionDelegate
{

	public void run(IAction action)
	{
		try
		{
			PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(
				new URL("http://localhost:" + ApplicationServerSingleton.get().getWebServerPort() + "/servoy-admin"));
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
	}

	public void selectionChanged(IAction action, ISelection selection)
	{
	}

	public void dispose()
	{
	}

	public void init(IWorkbenchWindow window)
	{
	}

}
