/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.DialogTray;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.util.EditorUtil.SaveDirtyEditorsOutputEnum;
import com.servoy.eclipse.ui.views.TutorialView;
import com.servoy.eclipse.ui.wizards.ICopyWarToCommandLineWizard;
import com.servoy.eclipse.ui.wizards.IRestoreDefaultWizard;

/**
 * Action for opening a wizard.
 *
 * @author acostescu
 */
public class OpenWizardAction extends Action
{

	private final Class< ? extends IWorkbenchWizard> wizardClass;

	/**
	 * Creates a new "open wizard" action.
	 *
	 * @param wizardClass the class of the wizard that this action will open.
	 * @param image the image descriptor for this action.
	 * @param text the text to be used as text & tool tip for this action.
	 */
	public OpenWizardAction(Class< ? extends IWorkbenchWizard> wizardClass, ImageDescriptor image, String text)
	{
		this.wizardClass = wizardClass;

		setImageDescriptor(image);
		if (text != null)
		{
			setText(text);
			setToolTipText(text);
		}
	}

	@Override
	public void run()
	{
		try
		{
			if (SaveDirtyEditorsOutputEnum.CANCELED == EditorUtil.saveDirtyEditors(UIUtils.getActiveShell(), true)) return;
			final IWorkbenchWizard wizard = wizardClass.newInstance();

			IStructuredSelection selection = StructuredSelection.EMPTY;
			ISelection windowSelection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
			if (windowSelection instanceof IStructuredSelection)
			{
				selection = (IStructuredSelection)windowSelection;
			}
			wizard.init(PlatformUI.getWorkbench(), selection);
			initWizard(wizard);
			// Instantiates the wizard container with the wizard and opens it
			WizardDialog dialog = new WizardDialog(UIUtils.getActiveShell(), wizard)
			{
				private Button restoreDefault = null;
				private Button copyWarToCmd = null;

				@Override
				protected IDialogSettings getDialogBoundsSettings()
				{
					return EditorUtil.getDialogSettings(wizardClass.getSimpleName());
				}

				@Override
				public void setShellStyle(int newShellStyle)
				{
					if (TutorialView.isTutorialViewOpen())
					{
						super.setShellStyle(SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE | SWT.RESIZE);
					}
					else
					{
						super.setShellStyle(SWT.CLOSE | SWT.PRIMARY_MODAL | SWT.BORDER | SWT.TITLE | SWT.RESIZE);
					}
				}

				@Override
				public boolean close()
				{
					boolean trayOpen = getTray() != null;
					if (trayOpen)
					{
						closeTray();
					}
					getDialogBoundsSettings().put("tray_open", trayOpen);
					wizard.dispose();
					return super.close();
				}

				@Override
				public void create()
				{
					super.create();

					String trayOpen = getDialogBoundsSettings().get("tray_open");
					if ((trayOpen == null || Boolean.valueOf(trayOpen).booleanValue()))
					{
						getShell().getDisplay().asyncExec(new Runnable()
						{
							@Override
							public void run()
							{
								getCurrentPage().performHelp();
							}
						});
					}
				}

				@Override
				public void openTray(DialogTray tray) throws IllegalStateException, UnsupportedOperationException
				{
					super.openTray(tray);
					Rectangle bounds = getShell().getBounds();
					bounds.width = bounds.width + 150;
					getShell().setBounds(bounds);
				}

				@Override
				protected void createButtonsForButtonBar(Composite parent)
				{
					if (wizard instanceof IRestoreDefaultWizard)
					{
						restoreDefault = createButton(parent, 1, "Restore Defaults", false);
						restoreDefault.addSelectionListener(new SelectionListener()
						{

							@Override
							public void widgetSelected(SelectionEvent e)
							{
								((IRestoreDefaultWizard)wizard).restoreDefaults();
							}

							@Override
							public void widgetDefaultSelected(SelectionEvent e)
							{
							}
						});
					}
					if (wizard instanceof ICopyWarToCommandLineWizard)
					{
						copyWarToCmd = createButton(parent, 2, "Command line equiv.", false);
						copyWarToCmd.setEnabled(false);
						copyWarToCmd.addSelectionListener(new SelectionListener()
						{

							@Override
							public void widgetSelected(SelectionEvent e)
							{
								String extraInfoForClient = ((ICopyWarToCommandLineWizard)wizard).copyWarToCommandLine();
								MessageBox box = new MessageBox(parent.getShell(), SWT.OK);
								box.setText("War Export - cmd. line args");
								box.setMessage("Command for command line war exporter (equivalent to this wizard war export) was copied to clipboard." +
									(extraInfoForClient != null ? "\n\n" + extraInfoForClient : ""));
								box.open();
							}

							@Override
							public void widgetDefaultSelected(SelectionEvent e)
							{
							}
						});
					}
					super.createButtonsForButtonBar(parent);
				}

				@Override
				protected void finishPressed()
				{
					if (restoreDefault != null) restoreDefault.setEnabled(false);
					if (getTray() != null)
					{
						closeTray();
					}
					super.finishPressed();
					if (restoreDefault != null && getReturnCode() != OK) restoreDefault.setEnabled(true);
				}

				@Override
				protected void nextPressed()
				{
					super.nextPressed();
					if (getTray() != null)
					{
						getCurrentPage().performHelp();
					}
				}

				@Override
				protected void backPressed()
				{
					super.backPressed();
					if (getTray() != null)
					{
						getCurrentPage().performHelp();
					}
				}

				@Override
				public void updateButtons()
				{
					super.updateButtons();
					Button fns = this.getButton(IDialogConstants.FINISH_ID);
					if (fns != null && copyWarToCmd != null) copyWarToCmd.setEnabled(fns.isEnabled());
				}

			};
			if (wizard instanceof IPageChangedListener)
			{
				dialog.addPageChangedListener((IPageChangedListener)wizard);
			}
			dialog.create();
			dialog.open();
			handleWizardReturnValue(wizard);
			wizard.dispose();

		}
		catch (InstantiationException e)
		{
			ServoyLog.logError(e);
		}
		catch (IllegalAccessException e)
		{
			ServoyLog.logError(e);
		}
	}

	public void handleWizardReturnValue(IWorkbenchWizard wizard)
	{

	}

	public void initWizard(IWorkbenchWizard wizard)
	{

	}
}