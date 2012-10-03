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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import com.servoy.eclipse.core.extension.InstalledWithPendingExtensionProvider;
import com.servoy.eclipse.core.extension.RestartState;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.extension.DependencyMetadata;
import com.servoy.extension.ExtensionDependencyDeclaration;
import com.servoy.extension.ExtensionUtils;
import com.servoy.extension.Message;
import com.servoy.extension.dependency.ExtensionNode;
import com.servoy.extension.dependency.InstallStep;
import com.servoy.extension.dependency.MaxVersionLibChooser;
import com.servoy.extension.install.LibActivationHandler;
import com.servoy.extension.install.LibChoiceHandler;
import com.servoy.extension.install.UninstallZipEntries;
import com.servoy.extension.parser.EXPParser;
import com.servoy.extension.parser.ExtensionConfiguration;
import com.servoy.j2db.util.WrappedObjectReference;

/**
 * This page prepares and executes an uninstall. If a restart is needed, then it will go to the restart page
 * and the uninstall process will continue on startup.
 * @author acostescu
 */
public class ActualUninstallPage extends ActualExecuteOperationPage
{

	public ActualUninstallPage(String pageName, InstallExtensionState state)
	{
		super(pageName, state, "Uninstalling extension", "The extension is being uninstalled. Please wait.", "Uninstall"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Override
	protected IRunnableWithProgress getPerformOperationRunnableWithProgress()
	{
		return new UninstallRunnableWithProgress();
	}

	protected class UninstallRunnableWithProgress implements IRunnableWithProgress
	{

		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
		{
			WrappedObjectReference<String> error = new WrappedObjectReference<String>(null);
			List<Message> allMessages = new ArrayList<Message>();
			state.installedExtensionsProvider.clearMessages();

			String tmp = "Preparing to uninstall"; //$NON-NLS-1$
			monitor.beginTask(tmp, state.chosenPath.installSequence.length * 100 + 50); // uninstall (100 * #) + restart search 30 & serialize 20 
			appendTextToLog(tmp + "..."); //$NON-NLS-1$

			File pendingDir = InstalledWithPendingExtensionProvider.getNextFreePendingDir(new File(state.installDir, ExtensionUtils.EXPFILES_FOLDER));
			try
			{
				// prepare this folder for serializing the RestartState if needed
				pendingDir.mkdirs();
				if (pendingDir.exists() && pendingDir.list().length == 0)
				{
					// check to see if restart is needed...
					monitor.setTaskName("Verifying the need for restart"); //$NON-NLS-1$
					monitor.subTask(""); //$NON-NLS-1$
					if (!state.mustRestart) // can be already set to true if there are already pending installs to be performed (from other install operations)
					{
						for (InstallStep step : state.chosenPath.installSequence)
						{
							// check the uninstall version's flag
							ExtensionNode n = step.extension;
							EXPParser parser = state.getOrCreateParser(state.installedExtensionsProvider.getEXPFile(n.id, n.installedVersion, null));
							ExtensionConfiguration parsed = parser.parseWholeXML();
							allMessages.addAll(Arrays.asList(parser.getMessages()));
							parser.clearMessages();
							if (parsed != null)
							{
								if (parsed.requiresRestart) // installed extensions should already be valid (parse result != null)
								{
									state.mustRestart = true;
									break;
								}
							}
							else
							{
								error.o = "Cannot parse package.xml of extension '" + state.installedExtensionsProvider.getDependencyMetadata(new ExtensionDependencyDeclaration(n.id, n.installedVersion, n.installedVersion))[0].extensionName + "'."; //$NON-NLS-1$ //$NON-NLS-2$
								break;
							}
						}
					}
					monitor.worked(30);

					if (error.o == null && monitor.isCanceled())
					{
						error.o = "Canceled by user."; //$NON-NLS-1$
					}

					if (error.o == null)
					{
						monitor.setTaskName("Uninstall progress"); //$NON-NLS-1$
						appendTextToLog(""); // new line //$NON-NLS-1$
						appendTextToLog("Uninstalling extension" + (state.chosenPath.installSequence.length > 1 ? "s..." : "...")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
						if (!state.mustRestart)
						{
							// start uninstalling what's needed!
							doOperation(new SubProgressMonitor(monitor, state.chosenPath.installSequence.length * 100), allMessages, state);
						}
						else
						{
							// only install developer specific content
							state.disallowCancel = true; // because a restart is needed
						}
						appendTextToLog(""); // new line //$NON-NLS-1$ 
					}
				}
				else
				{
					// unable to set-up the .pending install dir; install failed
					error.o = "Problem accessing an internal directory."; //$NON-NLS-1$
				}
			}
			finally
			{
				if (!state.mustRestart) FileUtils.deleteQuietly(pendingDir);
			}

			monitor.done();

			if (error.o == null && state.mustRestart)
			{
				error.o = state.storeToPending(pendingDir);
			}
			finalizeOperation(error, allMessages);
		}
	}

	/**
	 * Does the actual uninstall.
	 * @param allMessages any info/warning/error messages that are generated by this step.
	 */
	@Override
	protected void doOperation(IProgressMonitor monitor, List<Message> allMessages, @SuppressWarnings("hiding")
	RestartState state) // this hides the member on purpose; 'state' member should not be used in this method
	{
		// this can no longer be cancelled
		state.disallowCancel = true;

		monitor.beginTask("Uninstalling extension" + (state.chosenPath.installSequence.length > 1 ? "s" : ""), state.chosenPath.installSequence.length + 1);//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 

		LibChoiceHandler libHandler = null;
		if (state.chosenPath.libChoices != null)
		{
			libHandler = new LibChoiceHandler(state.installedExtensionsProvider, null, state);
			libHandler.prepareChoices(state.chosenPath.libChoices, new MaxVersionLibChooser());
		}

		for (InstallStep step : state.chosenPath.installSequence)
		{
			if (step.type == InstallStep.UNINSTALL)
			{
				DependencyMetadata dmd = state.installedExtensionsProvider.getDependencyMetadata(new ExtensionDependencyDeclaration(step.extension.id,
					step.extension.installedVersion, step.extension.installedVersion))[0];
				monitor.subTask("uninstalling '" + dmd.extensionName + "'..."); //$NON-NLS-1$//$NON-NLS-2$

				File f = state.installedExtensionsProvider.getEXPFile(step.extension.id, step.extension.installedVersion, null);

				EXPParser parser = state.getOrCreateParser(f);
				ExtensionConfiguration whole = parser.parseWholeXML();

				UninstallZipEntries uninstaller = new UninstallZipEntries(f, state.installDir, step.extension.id, step.extension.installedVersion, whole);
				uninstaller.handleFile();

				allMessages.addAll(Arrays.asList(uninstaller.getMessages()));

				monitor.worked(1);
				appendTextToLog("           [-] " + dmd.extensionName); //$NON-NLS-1$
			}
			else
			{
				// should never happen; if it does it's an implementation error
				allMessages.add(new Message("Internal error [uist]...", Message.ERROR)); //$NON-NLS-1$
				ServoyLog.logError("Unknown install step type...", null); //$NON-NLS-1$
			}
		}

		monitor.subTask("handling library dependencies..."); //$NON-NLS-1$

		if (libHandler != null)
		{
			LibActivationHandler activator = new LibActivationHandler(state.installDir);
			libHandler.handlePreparedChoices(activator);
			allMessages.addAll(Arrays.asList(libHandler.getMessages()));
			allMessages.addAll(Arrays.asList(activator.getMessages()));
		}

		monitor.worked(1);
		monitor.done();
	}

}
