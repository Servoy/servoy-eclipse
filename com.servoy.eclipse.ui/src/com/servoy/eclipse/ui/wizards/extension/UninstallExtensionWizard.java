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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.extension.InstalledWithPendingExtensionProvider;
import com.servoy.eclipse.ui.Activator;
import com.servoy.extension.DependencyMetadata;
import com.servoy.extension.ExtensionDependencyDeclaration;
import com.servoy.extension.ExtensionUtils;
import com.servoy.extension.Message;
import com.servoy.extension.VersionStringUtils;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;


/**
 * Wizard used to uninstall extensions.
 * @author acostescu
 */
public class UninstallExtensionWizard extends Wizard implements IImportWizard
{

	protected static final String TITLE = "Extension uninstall"; //$NON-NLS-1$

	protected String idToUninstall;
	protected String versionToUninstall;

	protected InstallExtensionState state = new InstallExtensionState();

	public UninstallExtensionWizard()
	{
		// when started from the "Import" menu item; file based import
	}

	/**
	 * Start the wizard based on an extension id to uninstall.
	 * @param idToUnInstallFromDeveloper the extension to uninstall.
	 */
	public UninstallExtensionWizard(String idToUninstall)
	{
		this.idToUninstall = idToUninstall;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		setWindowTitle(TITLE);
		setDefaultPageImageDescriptor(Activator.loadImageDescriptorFromBundle("extension_x_wizard.png")); //$NON-NLS-1$
		setNeedsProgressMonitor(true);

		state.display = workbench.getDisplay();
	}

	@Override
	public void addPages()
	{
		// first page is uninstall confirmation
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

			state.extensionID = idToUninstall;
			DependencyMetadata[] allV = tmp.getDependencyMetadata(new ExtensionDependencyDeclaration(idToUninstall, VersionStringUtils.UNBOUNDED,
				VersionStringUtils.UNBOUNDED));
			if (allV != null && allV.length == 1)
			{
				state.version = allV[0].version;
				state.extensionProvider = null;
				addPage(new UninstallReviewPage("UniRev", state)); //$NON-NLS-1$
			}
			else
			{
				// should never happen
				Message m1 = new Message("Cannot identify extension to uninstall with id '" + state.extensionID + "'.", Message.ERROR); //$NON-NLS-1$ //$NON-NLS-2$
				Message m2 = (allV != null && allV.length > 1)
					? new Message(
						"Invalid state for '" + state.extensionID + "'. Multiple versions of this extension are marked as installed but this is not supported/allowed!", Message.ERROR) : null; //$NON-NLS-1$ //$NON-NLS-2$
				Message[] msgs = (m2 != null) ? new Message[] { m2, m1 } : new Message[] { m1 };
				addPage(new ShowMessagesPage(
					"Error page", "Cannot uninstall extension", "A problem was encountered when looking for extension to uninstall.", null, msgs, false, null)); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			}
		}
		else
		{
			Message[] msgs = new Message[] { new Message("Cannot access directory '" + extDir.getAbsolutePath() + "'.", Message.ERROR) }; //$NON-NLS-1$ //$NON-NLS-2$
			addPage(new ShowMessagesPage(
				"Error page", "Cannot uninstall extension", "A problem was encountered accessing the extension dir.", null, msgs, false, null)); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
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
		return super.performCancel();
	}

	@Override
	public boolean performFinish()
	{
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
