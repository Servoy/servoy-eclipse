package com.servoy.eclipse.firststeps.ui.actions;

import java.net.URL;

import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.model.util.ServoyLog;

public class OpenURL implements IAction
{
	@Override
	public void run(String arguments)
	{
		if(arguments.length() > 0)
		{
			try
			{
				PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(arguments.substring(1)));
			}
			catch(Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
	}
}
