package com.servoy.eclipse.ui.wizards.extension;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.extension.ProcessPendingInstall;
import com.servoy.eclipse.core.util.UIUtils;

public class AfterRestartMessages implements IStartup
{

	public void earlyStartup()
	{
		// show errors/problems if any were encountered at install after restart
		final ProcessPendingInstall needsUI = ProcessPendingInstall.getAndClearUINeedingInstance();
		if (needsUI != null)
		{
			UIUtils.runInUI(new Runnable()
			{
				public void run()
				{
					InstallExtensionWizard installExtensionWizard = new InstallExtensionWizard(needsUI.getErrorAndMessages());
					installExtensionWizard.init(PlatformUI.getWorkbench(), null);
					WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), installExtensionWizard);
					dialog.open();
				}
			}, false);
		}
	}
}
