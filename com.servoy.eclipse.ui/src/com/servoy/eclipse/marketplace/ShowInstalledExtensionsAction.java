package com.servoy.eclipse.marketplace;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * Action that shows the installed extensions dialog (for upgrade/uninstall).
 * @author acostescu
 */
public class ShowInstalledExtensionsAction implements IWorkbenchWindowActionDelegate
{

	private Shell shell;

	public void run(IAction action)
	{
		InstalledExtensionsDialog dialog = InstalledExtensionsDialog.getOrCreateInstance(shell);
		dialog.open();
	}

	public void selectionChanged(IAction action, ISelection selection)
	{
		// not needed
	}

	public void dispose()
	{
		// not needed
	}

	public void init(IWorkbenchWindow window)
	{
		shell = window.getShell();
	}

}
