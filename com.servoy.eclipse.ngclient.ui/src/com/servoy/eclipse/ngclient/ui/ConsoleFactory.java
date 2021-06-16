package com.servoy.eclipse.ngclient.ui;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;

public class ConsoleFactory implements IConsoleFactory
{
	@Override
	public void openConsole()
	{
		IOConsole console = Activator.getInstance().getConsole();
		IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
		consoleManager.showConsoleView(console);
	}
}
