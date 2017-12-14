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
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
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
			final IWorkbenchWizard wizard = wizardClass.newInstance();

			IStructuredSelection selection = StructuredSelection.EMPTY;
			ISelection windowSelection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
			if (windowSelection instanceof IStructuredSelection)
			{
				selection = (IStructuredSelection)windowSelection;
			}
			wizard.init(PlatformUI.getWorkbench(), selection);

			// Instantiates the wizard container with the wizard and opens it
			WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), wizard)
			{
				private Button restoreDefault = null;

				@Override
				protected IDialogSettings getDialogBoundsSettings()
				{
					return EditorUtil.getDialogSettings(wizardClass.getSimpleName());
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
					super.createButtonsForButtonBar(parent);
				}

				@Override
				protected void finishPressed()
				{
					if (restoreDefault != null) restoreDefault.setEnabled(false);
					super.finishPressed();
					if (restoreDefault != null && getReturnCode() != OK) restoreDefault.setEnabled(true);
				}

			};
			if (wizard instanceof IPageChangedListener)
			{
				dialog.addPageChangedListener((IPageChangedListener)wizard);
			}
			dialog.create();
			dialog.open();
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
}