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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;

import com.servoy.eclipse.core.extension.InstalledWithPendingExtensionProvider;
import com.servoy.eclipse.core.extension.RestartState;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.marketplace.ContentInstaller;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.extension.DependencyMetadata;
import com.servoy.extension.ExtensionDependencyDeclaration;
import com.servoy.extension.ExtensionUtils;
import com.servoy.extension.IProgress;
import com.servoy.extension.Message;
import com.servoy.extension.dependency.ExtensionNode;
import com.servoy.extension.dependency.InstallStep;
import com.servoy.extension.dependency.MaxVersionLibChooser;
import com.servoy.extension.install.CopyZipEntryImporter;
import com.servoy.extension.install.LibActivationHandler;
import com.servoy.extension.install.LibChoiceHandler;
import com.servoy.extension.install.UninstallZipEntries;
import com.servoy.extension.parser.EXPParser;
import com.servoy.extension.parser.ExtensionConfiguration;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.WrappedObjectReference;

/**
 * This page gets all needed .exp files and starts installing. If a restart is needed, then it will go to the restart page
 * and the install process will continue on startup.
 * @author acostescu
 */
public class ActualInstallPage extends ActualExecuteOperationPage
{

	public ActualInstallPage(String pageName, InstallExtensionState state)
	{
		super(pageName, state, "Installing extension", "The extension is being installed. Please wait.", "Install"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Override
	protected IRunnableWithProgress getPerformOperationRunnableWithProgress()
	{
		return new InstallRunnableWithProgress();
	}

	protected class InstallRunnableWithProgress implements IRunnableWithProgress
	{

		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
		{
			WrappedObjectReference<String> error = new WrappedObjectReference<String>(null);
			List<Message> allMessages = new ArrayList<Message>();
			state.extensionProvider.clearMessages();
			state.installedExtensionsProvider.clearMessages();

			String tmp = "Getting extension package" + (state.chosenPath.extensionPath.length > 1 ? "s" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
			monitor.beginTask(tmp, state.chosenPath.extensionPath.length * 100 - 50 + 50); // download (60) & install (40) combo - download main extension that already happened + check restart flag 
			appendTextToLog(tmp + "..."); //$NON-NLS-1$

			File pendingDir = InstalledWithPendingExtensionProvider.getNextFreePendingDir(new File(state.installDir, ExtensionUtils.EXPFILES_FOLDER));
			try
			{
				// download & copy .exp files to .pending folder
				pendingDir.mkdirs();
				if (pendingDir.exists() && pendingDir.list().length == 0)
				{
					File f;
					boolean first = true; // don't count first extension getEXPFile as work (it was probably already downloaded)
					for (ExtensionNode n : state.chosenPath.extensionPath)
					{
						String name = state.extensionProvider.getDependencyMetadata(new ExtensionDependencyDeclaration(n.id, n.version, n.version))[0].extensionName;
						final String subTaskPrefix = "'" + name + "'"; //$NON-NLS-1$//$NON-NLS-2$
						monitor.subTask(subTaskPrefix + "..."); //$NON-NLS-1$ 

						IProgress progress = null;
						final SubProgressMonitor m;
						if (!first)
						{
							m = new SubProgressMonitor(monitor, 50);
							progress = new IProgress()
							{
								public void start(int totalWork)
								{
									m.beginTask("", totalWork); //$NON-NLS-1$
								}

								public void worked(int worked)
								{
									m.worked(worked);
								}

								public void setStatusMessage(String message)
								{
									m.subTask(subTaskPrefix + " - " + message); //$NON-NLS-1$
								}

								public boolean shouldCancelOperation()
								{
									return m.isCanceled();
								}
							};
						}
						else
						{
							m = null;
						}

						f = state.extensionProvider.getEXPFile(n.id, n.version, progress); // download

						if (!first) m.done();

						if (f == null)
						{
							// error, install failed, cannot get an .exp file
							if (!monitor.isCanceled())
							{
								error.o = "Cannot get extension package '" + name + "'."; //$NON-NLS-1$ //$NON-NLS-2$
								break;
							}
						}
						else
						{
							first = false;
							// copy it to '.pending' folder
							InputStream is = null;
							OutputStream os = null;
							try
							{
								is = new BufferedInputStream(new FileInputStream(f));
								os = new BufferedOutputStream(new FileOutputStream(new File(pendingDir, f.getName())));
								Utils.streamCopy(is, os);
							}
							catch (IOException e)
							{
								ServoyLog.logError(e);
								error.o = "Cannot copy extension package '" + name + "'.\nCheck logs for more details."; //$NON-NLS-1$ //$NON-NLS-2$
							}
							finally
							{
								Utils.closeInputStream(is);
								Utils.closeOutputStream(os);
							}
							monitor.worked(10);
							appendTextToLog("           [" + '\u2022' + "] " + name); // that is a bullet char //$NON-NLS-1$ //$NON-NLS-2$
						}
						if (monitor.isCanceled())
						{
							error.o = "Canceled by user."; //$NON-NLS-1$
							break;
						}
					}

					if (error.o == null)
					{
						// check to see if restart is needed...
						monitor.setTaskName("Verifying the need for restart"); //$NON-NLS-1$
						monitor.subTask(""); //$NON-NLS-1$
						if (!state.mustRestart) // can be already set to true if there are already pending installs to be performed (from other install operations)
						{
							for (ExtensionNode n : state.chosenPath.extensionPath)
							{
								if (n.resolveType != ExtensionNode.SIMPLE_DEPENDENCY_RESOLVE)
								{
									// check the uninstall version's flag
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
								EXPParser parser = state.getOrCreateParser(state.extensionProvider.getEXPFile(n.id, n.version, null));
								parser.clearMessages();
								ExtensionConfiguration parsed = parser.parseWholeXML();
								allMessages.addAll(Arrays.asList(parser.getMessages()));
								if (parsed != null)
								{
									if (parsed.requiresRestart)
									{
										state.mustRestart = true;
										break;
									}
								}
								else
								{
									error.o = "Cannot parse package.xml of extension '" + state.extensionProvider.getDependencyMetadata(new ExtensionDependencyDeclaration(n.id, n.version, n.version))[0].extensionName + "'."; //$NON-NLS-1$ //$NON-NLS-2$
									break;
								}
							}
						}
						monitor.worked(50);

						if (error.o == null && monitor.isCanceled())
						{
							error.o = "Canceled by user."; //$NON-NLS-1$
						}

						if (error.o == null)
						{
							monitor.setTaskName("Install progress"); //$NON-NLS-1$
							appendTextToLog(""); // new line //$NON-NLS-1$
							appendTextToLog("Installing extension" + (state.chosenPath.extensionPath.length > 1 ? "s..." : "...")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
							if (!state.mustRestart)
							{
								// start installing what's needed!
								doOperation(new SubProgressMonitor(monitor, state.chosenPath.extensionPath.length * 40), allMessages, state);
							}
							else
							{
								// only install developer specific content
								state.disallowCancel = true; // because a restart is needed

								// we will install solutions/styles/other developer specific content cause that is quite independent (doesn't need restart);
								// and if we would want to do this after restart, it would open up wizards/dialogs out of the blue (would look strange) 
								doDeveloperSpecificInstallOnly(new SubProgressMonitor(monitor, state.chosenPath.extensionPath.length * 40), allMessages, state);
							}
							appendTextToLog(""); // new line //$NON-NLS-1$ 
						}
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
	 * Does the actual install. This assumes that everything is in place for default copy & other needed operations.
	 * @param allMessages any info/warning/error messages that are generated by this step.
	 */
	@Override
	protected void doOperation(IProgressMonitor monitor, List<Message> allMessages, @SuppressWarnings("hiding")
	RestartState state) // this hides the member on purpose; 'state' member should not be used in this method
	{
		// this can no longer be cancelled
		state.disallowCancel = true;

		monitor.beginTask("Installing extension" + (state.chosenPath.extensionPath.length > 1 ? "s" : ""), state.chosenPath.installSequence.length + 1);//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 

		LibChoiceHandler libHandler = null;
		if (state.chosenPath.libChoices != null)
		{
			libHandler = new LibChoiceHandler(state.installedExtensionsProvider, state.extensionProvider, state);
			libHandler.prepareChoices(state.chosenPath.libChoices, new MaxVersionLibChooser());
		}

		for (InstallStep step : state.chosenPath.installSequence)
		{
			DependencyMetadata dmd = state.extensionProvider.getDependencyMetadata(new ExtensionDependencyDeclaration(step.extension.id,
				step.extension.version, step.extension.version))[0];
			if (step.type == InstallStep.INSTALL)
			{
				monitor.subTask("installing '" + dmd.extensionName + "'..."); //$NON-NLS-1$//$NON-NLS-2$
				File f = state.extensionProvider.getEXPFile(step.extension.id, step.extension.version, null);

				EXPParser parser = state.getOrCreateParser(f);
				ExtensionConfiguration whole = parser.parseWholeXML();

				// default install
				CopyZipEntryImporter defaultInstaller = new CopyZipEntryImporter(f, state.installDir, step.extension.id, step.extension.version, whole);
				defaultInstaller.handleFile();
				allMessages.addAll(Arrays.asList(defaultInstaller.getMessages()));

				// developer specific install
				allMessages.addAll(Arrays.asList(parser.getMessages()));
				parser.clearMessages();
				if (whole.getContent() != null)
				{
					final ContentInstaller developerSpecific = new ContentInstaller(f, whole.getContent(), state.installDir, true);
					Runnable r = new Runnable()
					{
						public void run()
						{
							developerSpecific.installAll();
						}
					};
					UIUtils.runInUI(r, true); // wait for it
				}

				monitor.worked(1);
				appendTextToLog("           [+] " + dmd.extensionName); //$NON-NLS-1$
			}
			else if (step.type == InstallStep.UNINSTALL)
			{
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

	/**
	 * Starts only the developer-specific install (import solutions/styles/SVN repo/...).
	 * @param allMessages any info/warning/error messages that are generated by this step.
	 */
	protected void doDeveloperSpecificInstallOnly(IProgressMonitor monitor, List<Message> allMessages, @SuppressWarnings("hiding")
	RestartState state) // this hides the member on purpose; 'state' member should not be used in this method
	{
		// this can no longer be cancelled
		state.disallowCancel = true;

		monitor.beginTask("Installing extension" + (state.chosenPath.extensionPath.length > 1 ? "s" : ""), state.chosenPath.installSequence.length);//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 

		for (InstallStep step : state.chosenPath.installSequence)
		{
			DependencyMetadata dmd = state.extensionProvider.getDependencyMetadata(new ExtensionDependencyDeclaration(step.extension.id,
				step.extension.version, step.extension.version))[0];
			if (step.type == InstallStep.INSTALL)
			{
				monitor.subTask("installing '" + dmd.extensionName + "'..."); //$NON-NLS-1$//$NON-NLS-2$
				File f = state.extensionProvider.getEXPFile(step.extension.id, step.extension.version, null);

				// developer specific install
				EXPParser parser = state.getOrCreateParser(f);
				ExtensionConfiguration whole = parser.parseWholeXML();
				allMessages.addAll(Arrays.asList(parser.getMessages()));
				parser.clearMessages();
				if (whole.getContent() != null)
				{
					final ContentInstaller developerSpecific = new ContentInstaller(f, whole.getContent(), state.installDir, false);
					Runnable r = new Runnable()
					{
						public void run()
						{
							developerSpecific.installAll();
						}
					};
					UIUtils.runInUI(r, true); // wait for it
				}

				monitor.worked(1);
				appendTextToLog("           [+] " + dmd.extensionName); //$NON-NLS-1$
			}
			else if (step.type == InstallStep.UNINSTALL)
			{
				// nothing to do here yet... for specific developer contents
			}
			else
			{
				// should never happen; if it does it's an implementation error
				allMessages.add(new Message("Internal error [uist]...", Message.ERROR)); //$NON-NLS-1$
				ServoyLog.logError("Unknown install step type...", null); //$NON-NLS-1$
			}
		}

		monitor.done();
	}

}
