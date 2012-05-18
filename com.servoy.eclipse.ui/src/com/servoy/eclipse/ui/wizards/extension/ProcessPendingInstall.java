package com.servoy.eclipse.ui.wizards.extension;

import java.io.File;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.extension.install.CopyZipEntryImporter;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;

public class ProcessPendingInstall implements Runnable
{

	public void run()
	{
		// we should already be running in the SWT display thread, but make sure anyway
		UIUtils.runInUI(new Runnable()
		{
			public void run()
			{
				File f = new File(new File(new File(ApplicationServerSingleton.get().getServoyApplicationServerDirectory()).getParent(),
					CopyZipEntryImporter.EXPFILES_FOLDER), ActualInstallPage.TO_BE_INSTALLED_FOLDER);
				if (f.exists())
				{
					InstallExtensionWizard installExtensionWizard = new InstallExtensionWizard(true);
					installExtensionWizard.init(PlatformUI.getWorkbench(), null);
					final WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), installExtensionWizard);
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

}