package com.servoy.eclipse.ui.wizards.extension;

import java.io.File;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.marketplace.InstalledWithPendingExtensionProvider;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.extension.ExtensionUtils;
import com.servoy.j2db.util.Utils;

public class ProcessPendingInstall implements Runnable
{

	public void run()
	{
		// we should already be running in the SWT display thread, but make sure anyway
		UIUtils.runInUI(new Runnable()
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
			}

		}, true);
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