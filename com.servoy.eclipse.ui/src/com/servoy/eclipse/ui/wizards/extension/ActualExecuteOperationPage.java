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

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.core.extension.RestartState;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.extension.Message;
import com.servoy.j2db.util.WrappedObjectReference;

/**
 * This page performs the actual extension management operation, showing progress.
 * @author acostescu
 */
public abstract class ActualExecuteOperationPage extends WizardPage
{

	protected InstallExtensionState state;

	protected boolean operationStarted = false;
	protected Text operationLog;
	protected IWizardPage nextPage = null;

	protected String operationTypeString;

	public ActualExecuteOperationPage(String pageName, InstallExtensionState state, String pageTitle, String pageDescription, String operationTypeString)
	{
		super(pageName);
		this.state = state;

		this.operationTypeString = operationTypeString;
		setTitle(pageTitle);
		setDescription(pageDescription);
		setPageComplete(false);
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
		operationLog = new Text(scroll, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP);
		operationLog.setBackground(state.display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		scroll.setBackground(state.display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		operationLog.setForeground(state.display.getSystemColor(SWT.COLOR_LIST_FOREGROUND));
		scroll.setContent(operationLog);
		operationLog.setText("");

		startOperationIfNecessary();
	}

	@Override
	public Control getControl()
	{
		startOperationIfNecessary();
		return super.getControl();
	}

	/**
	 * Called when the page might get first shown, in order to automatically start the install/uninstall/... process...
	 * But it might get called while the page is visible or when the page is not visible as well so it has to decide what to do and when to start it...
	 */
	protected void startOperationIfNecessary()
	{
		state.display.asyncExec(new Runnable()
		{
			public void run()
			{
				if (!operationStarted)
				{
					if (getContainer() != null && getContainer().getCurrentPage() == ActualExecuteOperationPage.this)
					{
						operationStarted = true;
						boolean error = false;

						try
						{
							IRunnableWithProgress runnable = getPerformOperationRunnableWithProgress();

							getContainer().run(true, true, runnable);

							getContainer().updateButtons();
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
							state.disallowCancel = false;
							ShowMessagesPage errorPage = new ShowMessagesPage(
								"IntErr", operationTypeString + " failed", "Internal error. Logs contain more details.", false, null);
							errorPage.setWizard(getWizard());
							showPageInUIThread(errorPage);
						}
					}
				}
			}

		});
	}

	/**
	 * The runnable that prepares the extension management operation. It should define and use {@link #doOperation(IProgressMonitor, List, RestartState)} after that in order to actually
	 * execute the operation (just to make sure that only the restartState is used). It should call {@link #finalizeOperation(WrappedObjectReference, List)} when done.
	 * @return the runnable.
	 */
	protected abstract IRunnableWithProgress getPerformOperationRunnableWithProgress();

	protected void showPageInUIThread(final IWizardPage page)
	{
		state.display.asyncExec(new Runnable()
		{
			public void run()
			{
				getContainer().showPage(page);
			}
		});
	}

	protected void finalizeOperation(WrappedObjectReference<String> error, List<Message> allMessages)
	{
		if (state.extensionProvider != null) allMessages.addAll(Arrays.asList(state.extensionProvider.getMessages()));
		if (state.installedExtensionsProvider != null) allMessages.addAll(Arrays.asList(state.installedExtensionsProvider.getMessages()));
		Message messages[] = allMessages.size() > 0 ? allMessages.toArray(new Message[allMessages.size()]) : null;

		if (error.o != null)
		{
			state.disallowCancel = false;
			ShowMessagesPage errorPage = new ShowMessagesPage("ActErr", operationTypeString + " failed", error.o, null, messages, false, null);
			errorPage.setWizard(getWizard());
			showPageInUIThread(errorPage);
		}
		else if (state.mustRestart) // this will be false when running after restart
		{
			state.disallowCancel = true;
			state.canFinish = true;
			RestartPage restartPage = new RestartPage(state, messages, operationTypeString);
			restartPage.setWizard(getWizard());
			showPageInUIThread(restartPage); // runningWithUI == true for sure here
		}
		else
		{
			appendTextToLog("Done.");
			if (messages != null)
			{
				nextPage = new ShowMessagesPage(
					"ActErr", operationTypeString + " finished", "However, some items may need your attention.", null, messages, false, null);
				nextPage.setWizard(getWizard());
				// state.canFinish is not set to true here; see getNextPage()
			}
			else
			{
				state.canFinish = true;
			}
		}
	}

	protected void appendTextToLog(final String toAppend)
	{
		state.display.asyncExec(new Runnable()
		{
			public void run()
			{
				if (!operationLog.isDisposed()) // can be already disposed when on restart there are not messages to show and the wizard closes itself
				{
					operationLog.setText(operationLog.getText() + toAppend + System.getProperty("line.separator"));
					operationLog.setSize(operationLog.computeSize(operationLog.getParent().getSize().x, SWT.DEFAULT));
				}
			}
		});
	}

	/**
	 * Does the actual install. This assumes that everything is in place for default copy & other needed operations.
	 * It hides the state member on purpose; 'state' member should not be used in this method; only use the parameter. (the actual operation must be doable after restart as well)
	 * @param allMessages any info/warning/error messages that are generated by this step.
	 */
	protected abstract void doOperation(IProgressMonitor monitor, List<Message> allMessages, @SuppressWarnings("hiding")
	RestartState state);

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
