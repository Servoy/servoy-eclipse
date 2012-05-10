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
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.marketplace.ContentInstaller;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.extension.DependencyMetadata;
import com.servoy.extension.ExtensionDependencyDeclaration;
import com.servoy.extension.Message;
import com.servoy.extension.dependency.ExtensionNode;
import com.servoy.extension.dependency.InstallStep;
import com.servoy.extension.dependency.MaxVersionLibChooser;
import com.servoy.extension.install.CopyZipEntryImporter;
import com.servoy.extension.install.LibActivationHandler;
import com.servoy.extension.install.LibChoiceHandler;
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

	protected final static String TO_BE_INSTALLED_FOLDER = ".pending"; //$NON-NLS-1$

	protected InstallExtensionState state;

	protected boolean installStarted = false;
	protected Text installLog;
	protected IWizardPage nextPage = null;

	public ActualInstallPage(String pageName, InstallExtensionState state)
	{
		super(pageName);
		this.state = state;

		setTitle("Installing extension"); //$NON-NLS-1$
		setDescription("The extension is being installed. Please wait."); //$NON-NLS-1$
		setPageComplete(false);
	}

	public void createControl(Composite parent)
	{
		initializeDialogUnits(parent);

		ScrolledComposite scroll = new ScrolledComposite(parent, SWT.V_SCROLL);
		scroll.setAlwaysShowScrollBars(false);
		scroll.setExpandHorizontal(true);
		scroll.setMinWidth(10);
		installLog = new Text(scroll, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP);
		installLog.setText(""); //$NON-NLS-1$
		installLog.setBackground(state.display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		installLog.setForeground(state.display.getSystemColor(SWT.COLOR_LIST_FOREGROUND));
		scroll.setContent(installLog);

		scroll.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));

		setControl(scroll);
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
		state.display.asyncExec(new Runnable()
		{
			public void run()
			{
				if (!installStarted)
				{
					if (getContainer().getCurrentPage() == ActualInstallPage.this)
					{
						installStarted = true;
						boolean error = false;
						try
						{
							getContainer().run(true, true, new InstallRunnableWithProgress());
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
						if (error)
						{
							ShowMessagesPage errorPage = new ShowMessagesPage(
								"IntErr", "Install failed", "Internal error. Logs contain more details.", false, null); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
							errorPage.setWizard(getWizard());
							getContainer().showPage(errorPage);
						}
					}
				}
			}
		});
	}

	protected class InstallRunnableWithProgress implements IRunnableWithProgress
	{

		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
		{
			String[] error = new String[1];
			List<Message> allMessages = new ArrayList<Message>();
			state.mustRestart = false;
			state.extensionProvider.clearMessages();
			state.installedExtensionsProvider.clearMessages();

			String tmp = "getting extension package" + (state.chosenPath.extensionPath.length > 1 ? "s" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
			monitor.beginTask("Install progress - " + tmp, state.chosenPath.extensionPath.length * 10 - 5 + 5); // download (6) & install (4) combo - download main extension that already happened + check restart flag //$NON-NLS-1$
			appendTextToLog(tmp + "...\n"); //$NON-NLS-1$

			File destinationDir = new File(new File(state.installDir, CopyZipEntryImporter.EXPFILES_FOLDER), TO_BE_INSTALLED_FOLDER);
			try
			{
				// download & copy .exp files to .pending folder
				if (destinationDir.exists()) FileUtils.deleteQuietly(destinationDir);
				destinationDir.mkdirs();
				if (destinationDir.exists() && destinationDir.list().length == 0)
				{
					File f;
					boolean first = true; // don't count first extension getEXPFile as work (it was probably already downloaded)
					for (ExtensionNode n : state.chosenPath.extensionPath)
					{
						f = state.extensionProvider.getEXPFile(n.id, n.version);
						String name = state.extensionProvider.getDependencyMetadata(new ExtensionDependencyDeclaration(n.id, n.version, n.version))[0].extensionName;
						monitor.subTask("'" + name + "' ..."); //$NON-NLS-1$ //$NON-NLS-2$
						if (f == null)
						{
							// error, install failed, cannot get an .exp file
							error[0] = "Cannot get extension package '" + name + "'."; //$NON-NLS-1$ //$NON-NLS-2$
							break;
						}
						else
						{
							if (!first) monitor.worked(5);
							first = false;
							// copy it to '.pending' folder
							InputStream is = null;
							OutputStream os = null;
							try
							{
								is = new BufferedInputStream(new FileInputStream(f));
								os = new BufferedOutputStream(new FileOutputStream(new File(destinationDir, f.getName())));
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
							appendTextToLog("     [x] " + name); //$NON-NLS-1$
							monitor.worked(1);
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
						monitor.setTaskName("Install progress - verifying the need for restart"); //$NON-NLS-1$
						for (ExtensionNode n : state.chosenPath.extensionPath)
						{
							if (n.resolveType != ExtensionNode.SIMPLE_DEPENDENCY_RESOLVE)
							{
								// check the uninstall version's flag
								EXPParser parser = state.getOrCreateParser(state.installedExtensionsProvider.getEXPFile(n.id, n.installedVersion));
								ExtensionConfiguration parsed = parser.parseWholeXML();
								Message[] problems = parser.getMessages();
								parser.clearMessages();
								if (problems != null) allMessages.addAll(Arrays.asList(problems));
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
							EXPParser parser = state.getOrCreateParser(state.extensionProvider.getEXPFile(n.id, n.version));
							parser.clearMessages();
							ExtensionConfiguration parsed = parser.parseWholeXML();
							Message[] problems = parser.getMessages();
							if (problems != null) allMessages.addAll(Arrays.asList(problems));
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
						monitor.worked(5);

						if (error[0] == null && monitor.isCanceled())
						{
							error[0] = "Canceled by user."; //$NON-NLS-1$
						}

						if (!state.mustRestart && error[0] == null)
						{
							// start installing!
							doInstall(new SubProgressMonitor(monitor, state.chosenPath.extensionPath.length * 4), error, allMessages);
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
				if (!state.mustRestart) FileUtils.deleteQuietly(destinationDir);
			}

			monitor.done();

			Message[] problems = state.extensionProvider.getMessages();
			if (problems != null) allMessages.addAll(Arrays.asList(problems));
			problems = state.installedExtensionsProvider.getMessages();
			if (problems != null) allMessages.addAll(Arrays.asList(problems));
			Message messages[] = allMessages.size() > 0 ? allMessages.toArray(new Message[allMessages.size()]) : null;
			if (error[0] != null)
			{
				ShowMessagesPage errorPage = new ShowMessagesPage("InstErr", "Install failed", error[0], null, messages, false, null); //$NON-NLS-1$//$NON-NLS-2$
				errorPage.setWizard(getWizard());
				getContainer().showPage(errorPage);
			}
			else if (state.mustRestart)
			{
				// TODO persist the state for after restart
				state.canFinish = true;
				ShowMessagesPage restartPage = new ShowMessagesPage(
					"InstRst", "Restart", "Servoy Developer needs to restart\nin order to complete the install process.", null, messages, false, null); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				restartPage.setWizard(getWizard());
				getContainer().showPage(restartPage);
			}
			else
			{
				state.canFinish = true;
				if (messages != null)
				{
					ShowMessagesPage someProblems = new ShowMessagesPage(
						"InstErr", "Install finished", "The extension was installed, but some problems were encountered.", null, messages, false, null); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
					someProblems.setWizard(getWizard());
					getContainer().showPage(someProblems);
				}
				else
				{
					nextPage = new ShowMessagesPage("InstErr", "Install finished", "The extension was successfully installed.", false, null); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
					nextPage.setWizard(getWizard());
					state.display.asyncExec(new Runnable()
					{
						public void run()
						{
							getContainer().updateButtons();
						}
					});
				}
			}
		}
	}

	protected void appendTextToLog(final String toAppend)
	{
		state.display.asyncExec(new Runnable()
		{
			public void run()
			{
				installLog.setText(installLog.getText() + toAppend + '\n');
			}
		});
	}

	/**
	 * Does the actual install. This assumes that everything is in place for default copy & other needed operations.
	 * @param error error[0] can be set in case of fatal install failure.
	 * @param allMessages any info/warning/error messages that are generated by this step.
	 */
	protected void doInstall(IProgressMonitor monitor, String[] error, List<Message> allMessages)
	{
		// this can no longer be cancelled
		state.disallowCancel = true;

		String tmp = "installing extension" + (state.chosenPath.extensionPath.length > 1 ? "s" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
		monitor.beginTask("Install progress - " + tmp, state.chosenPath.installSequence.length + 1); //$NON-NLS-1$
		appendTextToLog(tmp + "..."); //$NON-NLS-1$

		for (InstallStep step : state.chosenPath.installSequence)
		{
			DependencyMetadata dmd = state.extensionProvider.getDependencyMetadata(new ExtensionDependencyDeclaration(step.extension.id,
				step.extension.version, step.extension.version))[0];
			if (step.type == InstallStep.INSTALL)
			{
				monitor.subTask("'" + dmd.extensionName + "' [INSTALL]..."); //$NON-NLS-1$//$NON-NLS-2$
				File f = state.extensionProvider.getEXPFile(step.extension.id, step.extension.version);

				// default install
				CopyZipEntryImporter defaultInstaller = new CopyZipEntryImporter(f, state.installDir, step.extension.id);
				defaultInstaller.importFile();
				Message[] problems = defaultInstaller.getMessages();
				if (problems != null)
				{
					allMessages.addAll(Arrays.asList(problems));
				}

				// developer specific install
				EXPParser parser = state.getOrCreateParser(f);
				ExtensionConfiguration whole = parser.parseWholeXML();
				problems = parser.getMessages();
				parser.clearMessages();
				if (problems != null) allMessages.addAll(Arrays.asList(problems));
				if (whole.getContent() != null)
				{
					final ContentInstaller developerSpecific = new ContentInstaller(f, whole.getContent());
					UIUtils.runInUI(new Runnable()
					{
						public void run()
						{
							developerSpecific.installAll();
						}
					}, true);
				}

				appendTextToLog("     [INSTALLED] " + dmd.extensionName); //$NON-NLS-1$
				monitor.worked(1);
			}
			else if (step.type == InstallStep.UNINSTALL)
			{
				monitor.subTask("'" + dmd.extensionName + "' [UNINSTALL]..."); //$NON-NLS-1$//$NON-NLS-2$
				// TODO uninstall
				appendTextToLog("     [REMOVED] " + dmd.extensionName); //$NON-NLS-1$
				monitor.worked(1);
			}
			else
			{
				// should never happen; if it does it's an implementation error
				allMessages.add(new Message("Internal error [uist]...", Message.ERROR)); //$NON-NLS-1$
				ServoyLog.logError("Unknown install step type...", null); //$NON-NLS-1$
			}
		}

		monitor.subTask("handling library dependencies..."); //$NON-NLS-1$

		LibChoiceHandler libHandler = new LibChoiceHandler(state.installedExtensionsProvider, state.extensionProvider, state);
		LibActivationHandler activator = new LibActivationHandler(state.installDir);
		libHandler.handleChoices(state.chosenPath.libChoices, new MaxVersionLibChooser(), activator);
		Message[] problems = libHandler.getMessages();
		if (problems != null) allMessages.addAll(Arrays.asList(problems));
		problems = activator.getMessages();
		if (problems != null) allMessages.addAll(Arrays.asList(problems));

		monitor.worked(1);
		monitor.done();
	}

	@Override
	public IWizardPage getPreviousPage()
	{
		return null;
	}

	@Override
	public IWizardPage getNextPage()
	{
		return nextPage;
	}

}
