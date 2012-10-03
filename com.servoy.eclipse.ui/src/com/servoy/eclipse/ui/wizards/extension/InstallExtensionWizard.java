/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */

package com.servoy.eclipse.ui.wizards.extension;

import java.io.File;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.extension.InstalledWithPendingExtensionProvider;
import com.servoy.eclipse.ui.Activator;
import com.servoy.extension.ExtensionUtils;
import com.servoy.extension.MarketPlaceExtensionProvider;
import com.servoy.extension.Message;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.util.Pair;


/**
 * Wizard used to install extensions either from local .exp files or from the Marketplace 
 * @author acostescu
 */
public class InstallExtensionWizard extends Wizard implements IImportWizard
{

	protected static final String WIZARD_SETTINGS_SECTION = "ExtensionInstallWizard"; //$NON-NLS-1$
	protected static final String TITLE = "Extension install"; //$NON-NLS-1$

	private Pair<String, Message[]> errorAndMsgs;

	protected String idToInstallFromMP;
	protected String versionToInstallFromMP;

	protected InstallExtensionWizardOptions dialogOptions;
	protected InstallExtensionState state = new InstallExtensionState();

	public InstallExtensionWizard()
	{
		// when started from the "Import" menu item; file based import
	}

	/**
	 * Start the wizard based on an extension id received from Servoy Marketplace.
	 * @param idToInstallFromMP if non-null, this will be a Marketplace install. Null will just show the normal import wizard.
	 * @param versionToInstallFromMP if started from the installed extensions dialog in order to update, we already know the version to install.
	 */
	public InstallExtensionWizard(String idToInstallFromMP, String versionToInstallFromMP)
	{
		this.idToInstallFromMP = idToInstallFromMP;
		this.versionToInstallFromMP = versionToInstallFromMP;
	}

	/**
	 * Start the wizard only for showing an error/warning/info page (for example needed after a restart install was run).
	 * @param errorAndMsgs an error message (can be null) and an array of messages to show to the user.
	 */
	public InstallExtensionWizard(Pair<String, Message[]> errorAndMsgs)
	{
		this.errorAndMsgs = errorAndMsgs;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		setWindowTitle(errorAndMsgs == null ? TITLE : "Extension install/uninstall"); //$NON-NLS-1$
		setDefaultPageImageDescriptor(idToInstallFromMP != null
			? Activator.loadImageDescriptorFromBundle("marketplace_wizard.png") : Activator.loadImageDescriptorFromBundle("extension_wizard.png")); //$NON-NLS-1$ //$NON-NLS-2$
		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection(WIZARD_SETTINGS_SECTION);
		if (section == null)
		{
			section = workbenchSettings.addNewSection(WIZARD_SETTINGS_SECTION);
		}
		setDialogSettings(section);
		setNeedsProgressMonitor(true);
		dialogOptions = new InstallExtensionWizardOptions(getDialogSettings());

		state.display = workbench.getDisplay();
	}

	@Override
	public void addPages()
	{
		// ApplicationServerSingleton.get() should never be called in this wizard elsewhere at restart, otherwise restart-install will not work correctly
		if (errorAndMsgs != null)
		{
			state.canFinish = true;
			state.disallowCancel = true; // install/uninstall is over, it can't be cancelled
			addPage(new ShowMessagesPage(
				"Error page", "Extension install/uninstall", errorAndMsgs.getLeft() != null ? errorAndMsgs.getLeft() : "Some items may need your attention.", null, errorAndMsgs.getRight(), false, null)); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		}
		else
		{
			// first page is either the install from MP page, or install from file

			if (state.installDir == null)
			{
				String appServerDir = ApplicationServerSingleton.get().getServoyApplicationServerDirectory();
				if (appServerDir != null)
				{
					state.installDir = new File(appServerDir).getParentFile();
				}
			}

			File extDir = new File(state.installDir, ExtensionUtils.EXPFILES_FOLDER);
			if (!extDir.exists()) extDir.mkdir();
			if (extDir.exists() && extDir.canRead() && extDir.isDirectory())
			{
				InstalledWithPendingExtensionProvider tmp = new InstalledWithPendingExtensionProvider(extDir, state);
				state.mustRestart = (tmp.getFolderCount() > 1); // check if we already have pending install operations
				state.installedExtensionsProvider = tmp;

				if (idToInstallFromMP != null)
				{
					MarketPlaceExtensionProvider marketplaceProvider = null;

					state.extensionID = idToInstallFromMP;
					// only show first page if there is more then one version available for this extension in the MP
					marketplaceProvider = new MarketPlaceExtensionProvider(state.installDir);
					if (versionToInstallFromMP == null)
					{
						String[] versions = marketplaceProvider.getAvailableVersions(idToInstallFromMP);
						if (versions == null || versions.length == 0)
						{
							// this shouldn't happen in normal circumstances; maybe a network error caused this...
							Message[] msgs = marketplaceProvider.getMessages();
							if (msgs.length == 0)
							{
								msgs = new Message[] { new Message("Unknown error", Message.ERROR) }; //$NON-NLS-1$
							}
							addPage(new ShowMessagesPage(
								"Error page", "Cannot install extension", "A problem was encountered during available versions lookup.", null, msgs, false, null)); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
						}
						else
						{
							state.extensionProvider = marketplaceProvider;
							if (versions.length == 1)
							{
								state.version = versions[0];
								addPage(new DependencyResolvingPage("DepResolver", state, dialogOptions, false)); //$NON-NLS-1$
							}
							else
							{
								// show a page that allows the user to choose a version and auto-selects the most appropriate one (highest compatible)
								addPage(new ChooseMPExtensionVersion("MPver", state, dialogOptions, marketplaceProvider, versions)); //$NON-NLS-1$
							}
						}
					}
					else
					{
						state.extensionProvider = marketplaceProvider;
						state.version = versionToInstallFromMP;
						addPage(new DependencyResolvingPage("DepResolver", state, dialogOptions, false)); //$NON-NLS-1$
					}
				}
				else
				{
					addPage(new ChooseEXPFilePage("EXPchooser", state, dialogOptions)); //$NON-NLS-1$
				}
			}
			else
			{
				Message[] msgs = new Message[] { new Message("Cannot access directory '" + extDir.getAbsolutePath() + "'.", Message.ERROR) }; //$NON-NLS-1$ //$NON-NLS-2$
				addPage(new ShowMessagesPage(
					"Error page", "Cannot install extension", "A problem was encountered accessing the extension dir.", null, msgs, false, null)); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			}
		}

		// all other pages are not added here because they vary depending on what's going on;
		// the next/previous page implementation for these dynamic pages is contained in the pages, they don't use the wizard's list of pages
	}

	@Override
	public boolean needsPreviousAndNextButtons()
	{
		return true;
	}

	@Override
	public boolean performCancel()
	{
		if (state.disallowCancel) return false;
		if (dialogOptions != null) dialogOptions.saveOptions(getDialogSettings());
		return super.performCancel();
	}

	@Override
	public boolean performFinish()
	{
		if (dialogOptions != null) dialogOptions.saveOptions(getDialogSettings());
		if (state.mustRestart)
		{
			disposeInternal();
			PlatformUI.getWorkbench().restart();
			return false; // false because otherwise wizard dialog will try to close when it's already closed and disposed (because of above call) => stack traces
		}
		return true;
	}

	@Override
	public boolean canFinish()
	{
		return state != null ? state.canFinish : false;
	}

	@Override
	public void dispose()
	{
		super.dispose();
		disposeInternal();
	}

	private void disposeInternal()
	{
		if (state != null) state.dispose();
	}

}
