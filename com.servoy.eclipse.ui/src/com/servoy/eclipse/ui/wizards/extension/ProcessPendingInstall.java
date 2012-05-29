package com.servoy.eclipse.ui.wizards.extension;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.marketplace.InstalledWithPendingExtensionProvider;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.wizards.extension.ActualInstallPage.ContinueInstallRunnableWithProgress;
import com.servoy.extension.ExtensionUtils;
import com.servoy.j2db.util.Utils;

public class ProcessPendingInstall implements Runnable
{

	public void run()
	{
		File installDir = getInstallDir();
		File f = new File(new File(installDir, ExtensionUtils.EXPFILES_FOLDER), InstalledWithPendingExtensionProvider.PENDING_FOLDER);

		if (f.exists())
		{
			InstallExtensionWizard installExtensionWizard = new InstallExtensionWizard(true, installDir);
			installExtensionWizard.init(PlatformUI.getWorkbench(), null);
			final WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), installExtensionWizard);
			// we should already be running in the SWT display thread; if not, don't try to block and show UI right now (would lead to potential deadlocks)
			if (Display.getCurrent() != null)
			{
				// async exec would not work here; see StartupAsyncUIRunner comments
				Display.getCurrent().timerExec(1, new Runnable()
				{
					public void run()
					{
						dialog.getShell().forceActive();
					}
				});
				dialog.open();
			}
			else
			{
				boolean error = false;

				// just run it without UI for now
				installExtensionWizard.addPages();
				IWizardPage[] pages = installExtensionWizard.getPages();
				if (pages != null && pages.length == 1 && pages[0] instanceof ActualInstallPage)
				{
					ActualInstallPage installPage = (ActualInstallPage)pages[0];
					ContinueInstallRunnableWithProgress runner = installPage.new ContinueInstallRunnableWithProgress(false);
					try
					{
						runner.run(null);
					}
					catch (InvocationTargetException e)
					{
						ServoyLog.logError(e);
						error = true;
					}
					catch (InterruptedException e)
					{
						ServoyLog.logError(e);
						error = true;
					}
					if (installPage.getNextPage() != null)
					{
						// so the user should be aware of some things that happened (but we can show those later)
						UIUtils.runInUI(new Runnable()
						{
							public void run()
							{
								dialog.open();
							}
						}, false);
					}
				}
				else
				{
					ServoyLog.logError("Internal error: Cannot continue with pending install: [" + (pages != null ? pages.length : -1) + "]", null); //$NON-NLS-1$ //$NON-NLS-2$
					error = true;
				}

				if (error)
				{
					UIUtils.runInUI(new Runnable()
					{
						public void run()
						{
							MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
								"Extension install failed", "Internal error. Check logs for more details."); //$NON-NLS-1$//$NON-NLS-2$
						}
					}, false);
				}
			}
		}
	}

	protected File getInstallDir()
	{
		// we are not using ApplicationServerSingleton.get().getServoyApplicationServerDirectory() cause that would init the app. server and load all plugins, beans...
		// which negates the purpose of this restart-install
		File f = null;
		String location = System.getProperty("servoy.application_server.dir"); //$NON-NLS-1$
		if (location == null)
		{
			location = System.getProperty("eclipse.home.location"); //$NON-NLS-1$
			if (location != null && location.startsWith("file:")) //$NON-NLS-1$
			{
				location = location.substring(5) + "../"; //$NON-NLS-1$
				if (location != null && Utils.getPlatform() == Utils.PLATFORM_WINDOWS)
				{
					if (location.startsWith("/")) location = location.substring(1); //$NON-NLS-1$
					location = location.replaceAll("/", "\\\\"); //$NON-NLS-1$//$NON-NLS-2$
				}
				f = new File(location);
			}
		}
		else
		{
			f = new File(location + "../"); //$NON-NLS-1$
		}

		if (f != null && f.exists()) return f;

		// should never happen
		RuntimeException ex = new RuntimeException("eclipseLocation='" + location + '\''); //$NON-NLS-1$
		ServoyLog.logError("Could not determine servoy base location", ex); //$NON-NLS-1$ 
		MessageDialog.openError(null, "Install", "A pending installation has failed: base location unknown."); //$NON-NLS-1$//$NON-NLS-2$
		throw ex;
	}

}