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
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.core.util.UIUtils.StartupAsyncUIRunner;
import com.servoy.eclipse.marketplace.ContentInstaller;
import com.servoy.eclipse.marketplace.InstalledWithPendingExtensionProvider;
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

/**
 * This page gets all needed .exp files and starts installing. If a restart is needed, then it will go to the restart page
 * and the install process will continue on startup.
 * @author acostescu
 */
public class ActualInstallPage extends WizardPage
{

	protected InstallExtensionState state;

	protected boolean installStarted = false;
	protected Text installLog;
	protected IWizardPage nextPage = null;

	protected boolean afterRestart;

	protected StartupAsyncUIRunner asyncUIRunner;
	protected boolean automaticClose = false; // cannot try to close the wizard automatically while a ui progress job is running

	public ActualInstallPage(String pageName, InstallExtensionState state, boolean afterRestart)
	{
		super(pageName);
		this.state = state;
		this.afterRestart = afterRestart;

		setTitle("Installing extension"); //$NON-NLS-1$
		setDescription("The extension is being installed. Please wait."); //$NON-NLS-1$
		setPageComplete(false);

		asyncUIRunner = new StartupAsyncUIRunner(state.display);
	}

	public void createControl(Composite parent)
	{
		initializeDialogUnits(parent);

		final Composite topLevel = new Composite(parent, SWT.NONE);
		topLevel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
		GridLayout gl = new GridLayout(1, false);
		gl.marginWidth = 10;
		gl.marginHeight = 10;
		topLevel.setLayout(gl);
		setControl(topLevel);

		ScrolledComposite scroll = new ScrolledComposite(topLevel, SWT.V_SCROLL | SWT.BORDER);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scroll.setAlwaysShowScrollBars(false);
		scroll.setExpandHorizontal(true);
		scroll.setMinWidth(10);
		installLog = new Text(scroll, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP);
		installLog.setBackground(state.display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		scroll.setBackground(state.display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		installLog.setForeground(state.display.getSystemColor(SWT.COLOR_LIST_FOREGROUND));
		scroll.setContent(installLog);
		installLog.setText(""); //$NON-NLS-1$

		startInstallProcessIfNecessary();
	}

	@Override
	public Control getControl()
	{
		startInstallProcessIfNecessary();
		return super.getControl();
	}

	/**
	 * Called when the page might get first shown, in order to automatically start the install process...
	 * But it might get called while the page is visible or when the page is not visible as well so it has to decide what to do and when to start it...
	 */
	protected void startInstallProcessIfNecessary()
	{
		asyncUIRunner.asyncExec(new Runnable()
		{
			public void run()
			{
				if (!installStarted)
				{
					if (getContainer() != null && getContainer().getCurrentPage() == ActualInstallPage.this)
					{
						installStarted = true;
						boolean error = false;
						try
						{
							IRunnableWithProgress runnable = (afterRestart ? new ContinueInstallRunnableWithProgress() : new InstallRunnableWithProgress());

//							if (afterRestart) ModalContext.setAllowReadAndDispatch(true); // if workbench startup is not complete, fork will have no effect (this flag would make if have effect); but:

							// the trouble at restart is that even if we fork we run into trouble (exceptions) because
							// class loaders will try to activate the bundle and because we are doing it on another thread then the main (which is activating it
							// right now - that triggers the wizard) it will just wait, time-out and err. so for the moment, we won't fork at startup...
							getContainer().run(!afterRestart, !afterRestart, runnable);

//							if (afterRestart) ModalContext.setAllowReadAndDispatch(false); // see comment above

							getContainer().updateButtons();
							if (automaticClose) ((WizardDialog)getContainer()).close();
						}
						catch (InvocationTargetException e)
						{
							error = true;
							ServoyLog.logError(e);
						}
						catch (InterruptedException e)
						{
							error = true;
							ServoyLog.logError(e);
						}
//						finally
//						{
//							if (afterRestart) ModalContext.setAllowReadAndDispatch(false); // see comment above
//						}
						if (error)
						{
							state.disallowCancel = false;
							ShowMessagesPage errorPage = new ShowMessagesPage(
								"IntErr", "Install failed", "Internal error. Logs contain more details.", false, null); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
							errorPage.setWizard(getWizard());
							showPageInUIThread(errorPage);
						}
					}
				}
			}

		});
	}

	protected void showPageInUIThread(final ShowMessagesPage page)
	{
		asyncUIRunner.asyncExec(new Runnable()
		{
			public void run()
			{
				getContainer().showPage(page);
			}
		});
	}

	protected class InstallRunnableWithProgress implements IRunnableWithProgress
	{

		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
		{
			String[] error = new String[1];
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
						processUIEventsIfNeeded();

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
									processUIEventsIfNeeded();
								}

								public void worked(int worked)
								{
									m.worked(worked);
									processUIEventsIfNeeded();
								}

								public void setStatusMessage(String message)
								{
									m.subTask(subTaskPrefix + " - " + message); //$NON-NLS-1$
									processUIEventsIfNeeded();
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
								error[0] = "Cannot get extension package '" + name + "'."; //$NON-NLS-1$ //$NON-NLS-2$
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
								error[0] = "Cannot copy extension package '" + name + "'.\nCheck logs for more details."; //$NON-NLS-1$ //$NON-NLS-2$
							}
							finally
							{
								Utils.closeInputStream(is);
								Utils.closeOutputStream(os);
							}
							monitor.worked(10);
							appendTextToLog("           [" + '\u2022' + "] " + name); //$NON-NLS-1$ //$NON-NLS-2$
						}
						if (monitor.isCanceled())
						{
							error[0] = "Canceled by user."; //$NON-NLS-1$
							break;
						}
					}

					if (error[0] == null)
					{
						// check to see if restart is needed...
						monitor.setTaskName("Verifying the need for restart"); //$NON-NLS-1$
						monitor.subTask(""); //$NON-NLS-1$
						processUIEventsIfNeeded();
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
										error[0] = "Cannot parse package.xml of extension '" + state.installedExtensionsProvider.getDependencyMetadata(new ExtensionDependencyDeclaration(n.id, n.installedVersion, n.installedVersion))[0].extensionName + "'."; //$NON-NLS-1$ //$NON-NLS-2$
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
									error[0] = "Cannot parse package.xml of extension '" + state.extensionProvider.getDependencyMetadata(new ExtensionDependencyDeclaration(n.id, n.version, n.version))[0].extensionName + "'."; //$NON-NLS-1$ //$NON-NLS-2$
									break;
								}
							}
						}
						monitor.worked(50);
						processUIEventsIfNeeded();

						if (error[0] == null && monitor.isCanceled())
						{
							error[0] = "Canceled by user."; //$NON-NLS-1$
						}

						if (!state.mustRestart && error[0] == null)
						{
							// start installing!
							monitor.setTaskName("Install progress"); //$NON-NLS-1$
							appendTextToLog(""); // new line //$NON-NLS-1$
							appendTextToLog("Installing extension" + (state.chosenPath.extensionPath.length > 1 ? "s..." : "...")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
							doInstall(new SubProgressMonitor(monitor, state.chosenPath.extensionPath.length * 40), allMessages, state);
							appendTextToLog(""); // new line //$NON-NLS-1$ 
						}
						else if (error[0] == null)
						{
							state.disallowCancel = true; // because a restart is needed
						}
					}
				}
				else
				{
					// unable to set-up the .pending install dir; install failed
					error[0] = "Problem accessing an internal directory."; //$NON-NLS-1$
				}
			}
			finally
			{
				if (!state.mustRestart) FileUtils.deleteQuietly(pendingDir);
			}

			monitor.done();

			if (error[0] == null && state.mustRestart)
			{
				error[0] = state.storeToPending(pendingDir);
			}
			finalizeInstall(error, allMessages);
		}
	}

	protected class ContinueInstallRunnableWithProgress implements IRunnableWithProgress
	{

		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
		{
			String[] error = new String[1];
			List<Message> allMessages = new ArrayList<Message>();

			if (state.installedExtensionsProvider != null)
			{
				File extDir = new File(state.installDir, ExtensionUtils.EXPFILES_FOLDER);
				try
				{
					File[] pendingDirs = InstalledWithPendingExtensionProvider.getPendingDirsAscending(extDir);
					String tmp = "Preparing pending install"; //$NON-NLS-1$ 
					monitor.beginTask(tmp, 100 * pendingDirs.length); // 60% already done before restart, restore state (4) and install (36)
					monitor.worked(60 * pendingDirs.length); // stuff already done before restart
					processUIEventsIfNeeded();

					for (int i = 0; i < pendingDirs.length && error[0] == null; i++) // ascending
					{
						monitor.setTaskName(tmp);
						appendTextToLog(tmp + "..."); //$NON-NLS-1$ 
						error[0] = state.recreateFromPending(pendingDirs[i]);

						monitor.worked(4);
						processUIEventsIfNeeded();

						if (error[0] == null)
						{
							// start installing!
							monitor.setTaskName("Install progress"); //$NON-NLS-1$
							appendTextToLog("Installing " + (i + 1) + " of " + pendingDirs.length + "..."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
							doInstall(new SubProgressMonitor(monitor, 36), allMessages, state);
							appendTextToLog(""); // new line //$NON-NLS-1$ 
						}
					}
				}
				finally
				{
					FileUtils.deleteQuietly(new File(extDir, InstalledWithPendingExtensionProvider.PENDING_FOLDER));
				}
			}
			else
			{
				// should never happen
				error[0] = "Cannot access installed extensions."; //$NON-NLS-1$
			}

			monitor.done();

			finalizeInstall(error, allMessages);
		}
	}

	protected void finalizeInstall(String[] error, List<Message> allMessages)
	{
		if (state.extensionProvider != null) allMessages.addAll(Arrays.asList(state.extensionProvider.getMessages()));
		if (state.installedExtensionsProvider != null) allMessages.addAll(Arrays.asList(state.installedExtensionsProvider.getMessages()));
		Message messages[] = allMessages.size() > 0 ? allMessages.toArray(new Message[allMessages.size()]) : null;

		if (error[0] != null)
		{
			state.disallowCancel = false;
			ShowMessagesPage errorPage = new ShowMessagesPage("InstErr", "Install failed", error[0], null, messages, false, null); //$NON-NLS-1$//$NON-NLS-2$
			errorPage.setWizard(getWizard());
			showPageInUIThread(errorPage);
		}
		else if (state.mustRestart) // this will be false when running after restart
		{
			state.disallowCancel = true;
			state.canFinish = true;
			RestartPage restartPage = new RestartPage(state, messages);
			restartPage.setWizard(getWizard());
			showPageInUIThread(restartPage);
		}
		else
		{
			appendTextToLog("Done."); //$NON-NLS-1$
			if (messages != null)
			{
				nextPage = new ShowMessagesPage("InstErr", "Install finished", "Some items require your attention.", null, messages, false, null); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				nextPage.setWizard(getWizard());
			}
			else
			{
				state.canFinish = true;
				if (afterRestart && getContainer() instanceof WizardDialog)
				{
					// just try to close the wizard if there are no messages to continue with developer startup
					state.disallowCancel = false; // so that we can close
					automaticClose = true;
				}
			}
		}
	}

	protected void appendTextToLog(final String toAppend)
	{
		asyncUIRunner.asyncExec(new Runnable()
		{
			public void run()
			{
				if (!installLog.isDisposed()) // can be already disposed when on restart there are not messages to show and the wizard closes itself
				{
					installLog.setText(installLog.getText() + toAppend + System.getProperty("line.separator")); //$NON-NLS-1$
					installLog.setSize(installLog.computeSize(installLog.getParent().getSize().x, SWT.DEFAULT));
				}
			}
		});
		processUIEventsIfNeeded();
	}

	protected void processUIEventsIfNeeded()
	{
		if (state.display.getThread() == Thread.currentThread())
		{
			while (state.display.readAndDispatch())
			{
				// continue dispatching until nothing more is scheduled
			}
		}
	}

	/**
	 * Does the actual install. This assumes that everything is in place for default copy & other needed operations.
	 * @param allMessages any info/warning/error messages that are generated by this step.
	 */
	protected void doInstall(IProgressMonitor monitor, List<Message> allMessages, @SuppressWarnings("hiding")
	RestartState state) // this hides the member on purpose; 'state' member should not be used in this method
	{
		// this can no longer be cancelled
		state.disallowCancel = true;

		monitor.beginTask("Installing extension" + (state.chosenPath.extensionPath.length > 1 ? "s" : ""), state.chosenPath.installSequence.length + 1);//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
		processUIEventsIfNeeded();

		for (InstallStep step : state.chosenPath.installSequence)
		{
			DependencyMetadata dmd = state.extensionProvider.getDependencyMetadata(new ExtensionDependencyDeclaration(step.extension.id,
				step.extension.version, step.extension.version))[0];
			if (step.type == InstallStep.INSTALL)
			{
				monitor.subTask("installing '" + dmd.extensionName + "'..."); //$NON-NLS-1$//$NON-NLS-2$
				processUIEventsIfNeeded();
				File f = state.extensionProvider.getEXPFile(step.extension.id, step.extension.version, null);

				// default install
				CopyZipEntryImporter defaultInstaller = new CopyZipEntryImporter(f, state.installDir, step.extension.id, step.extension.version);
				defaultInstaller.handleFile();
				allMessages.addAll(Arrays.asList(defaultInstaller.getMessages()));

				// developer specific install
				EXPParser parser = state.getOrCreateParser(f);
				ExtensionConfiguration whole = parser.parseWholeXML();
				allMessages.addAll(Arrays.asList(parser.getMessages()));
				parser.clearMessages();
				if (whole.getContent() != null)
				{
					final ContentInstaller developerSpecific = new ContentInstaller(f, whole.getContent(), state.installDir);
					Runnable r = new Runnable()
					{
						public void run()
						{
							developerSpecific.installAll();
						}
					};
					if (afterRestart)
					{
						// cannot run sync and wait, because at restart, the developer specific install might need ServoyModel, which
						// is not available at that time (during preInitialize that executes before ServoyModel is created)
						allMessages.add(new Message("Developer specific install tasks posponed until developer is fully started.", Message.INFO)); //$NON-NLS-1$
						state.display.asyncExec(r);
					}
					else
					{
						UIUtils.runInUI(r, true); // wait for it
					}
				}

				monitor.worked(1);
				appendTextToLog("           [+] " + dmd.extensionName); //$NON-NLS-1$
			}
			else if (step.type == InstallStep.UNINSTALL)
			{
				monitor.subTask("uninstalling '" + dmd.extensionName + "'..."); //$NON-NLS-1$//$NON-NLS-2$
				processUIEventsIfNeeded();

				File f = state.installedExtensionsProvider.getEXPFile(step.extension.id, step.extension.installedVersion, null);
				UninstallZipEntries uninstaller = new UninstallZipEntries(f, state.installDir, step.extension.id, step.extension.installedVersion);
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
		processUIEventsIfNeeded();

		if (state.chosenPath.libChoices != null)
		{
			LibChoiceHandler libHandler = new LibChoiceHandler(state.installedExtensionsProvider, state.extensionProvider, state);
			LibActivationHandler activator = new LibActivationHandler(state.installDir);
			libHandler.handleChoices(state.chosenPath.libChoices, new MaxVersionLibChooser(), activator);
			allMessages.addAll(Arrays.asList(libHandler.getMessages()));
			allMessages.addAll(Arrays.asList(activator.getMessages()));
		}

		monitor.worked(1);
		monitor.done();
	}

	@Override
	public IWizardPage getPreviousPage()
	{
		return null;
	}

	@Override
	public boolean canFlipToNextPage()
	{
		return nextPage != null;
	}

	@Override
	public IWizardPage getNextPage()
	{
		if (nextPage != null) state.canFinish = true;
		return nextPage;
	}

}
