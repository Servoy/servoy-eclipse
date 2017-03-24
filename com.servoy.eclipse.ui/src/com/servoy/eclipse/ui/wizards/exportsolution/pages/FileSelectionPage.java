/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

package com.servoy.eclipse.ui.wizards.exportsolution.pages;

import java.io.File;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.BuilderUtils;
import com.servoy.eclipse.ui.wizards.ExportSolutionWizard;

/**
 * @author gboros
 *
 */
public class FileSelectionPage extends WizardPage implements Listener
{
	private final ExportSolutionWizard exportSolutionWizard;
	private Text fileNameText;
	private Button browseButton;
	private int projectProblemsType;

	public FileSelectionPage(ExportSolutionWizard exportSolutionWizard)
	{
		super("page1");
		setTitle("Choose the destination file");
		setDescription("Select the file where you want your solution exported to");
		this.exportSolutionWizard = exportSolutionWizard;
		projectProblemsType = BuilderUtils.getMarkers(
			new String[] { ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getProject().getName() });
		if (projectProblemsType == BuilderUtils.HAS_ERROR_MARKERS)
		{
			if (exportSolutionWizard.getModulesSelectionPage().hasDBDownErrors())
			{
				projectProblemsType = BuilderUtils.HAS_WARNING_MARKERS;
				setMessage(ModulesSelectionPage.DB_DOWN_WARNING, IMessageProvider.WARNING);
			}
			else setErrorMessage("Errors in the solution will prevent it from functioning well. Please solve errors (problems view) first.");
		}
		else if (projectProblemsType == BuilderUtils.HAS_WARNING_MARKERS)
		{
			setMessage("Warnings in the solution may prevent it from functioning well. You may want to solve warnings (problems view) first.",
				IMessageProvider.WARNING);
		}
	}

	public void createControl(Composite parent)
	{
		GridLayout gridLayout = new GridLayout(2, false);
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(gridLayout);

		fileNameText = new Text(composite, SWT.BORDER);
		fileNameText.addListener(SWT.KeyUp, this);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		fileNameText.setLayoutData(gd);
		if (exportSolutionWizard.getModel().getFileName() != null) fileNameText.setText(exportSolutionWizard.getModel().getFileName());

		browseButton = new Button(composite, SWT.PUSH);
		browseButton.setText("Browse...");
		browseButton.addListener(SWT.Selection, this);

		setControl(composite);
	}

	public void handleEvent(Event event)
	{
		if (event.widget == fileNameText)
		{
			String potentialFileName = fileNameText.getText();
			exportSolutionWizard.getModel().setFileName(potentialFileName);
		}
		else if (event.widget == browseButton)
		{
			Shell shell = new Shell();
			GridLayout gridLayout = new GridLayout();
			shell.setLayout(gridLayout);
			FileDialog dlg = new FileDialog(shell, SWT.SAVE);
			if (exportSolutionWizard.getModel().getFileName() != null)
			{
				File f = new File(exportSolutionWizard.getModel().getFileName());
				if (f.isDirectory())
				{
					dlg.setFilterPath(f.getAbsolutePath());
					dlg.setFileName(null);
				}
				else
				{
					dlg.setFilterPath(f.getParent());
					dlg.setFileName(f.getName());
					String[] extensions = { "*.servoy" };
					dlg.setFilterExtensions(extensions);
				}
			}
			String chosenFileName = dlg.open();
			if (chosenFileName != null)
			{
				exportSolutionWizard.getModel().setFileName(chosenFileName);
				fileNameText.setText(chosenFileName);
			}
		}
		getWizard().getContainer().updateButtons();
		getWizard().getContainer().updateMessage();
	}

	@Override
	public boolean canFlipToNextPage()
	{
		boolean result = true;
		if (projectProblemsType == BuilderUtils.HAS_ERROR_MARKERS && !exportSolutionWizard.hasActiveSolutionDbDownErrors()) return false;

		boolean messageSet = (projectProblemsType == BuilderUtils.HAS_WARNING_MARKERS);
		if (exportSolutionWizard.getModel().getFileName() == null) return false;
		if (fileNameText.getText().length() == 0)
		{
			result = false;
		}
		else
		{
			File f = new File(exportSolutionWizard.getModel().getFileName());
			if (f.exists())
			{
				if (f.isDirectory())
				{
					if (!messageSet) setMessage("Specified path points to an existing folder.", IMessageProvider.WARNING);
					result = false;
					messageSet = true;
				}
				else
				{
					if (!messageSet) setMessage("Specified path points to an existing file.", IMessageProvider.INFORMATION);
					messageSet = true;
				}
			}
		}
		if (!messageSet)
		{
			setMessage(null);
		}
		return result;
	}

	@Override
	public IWizardPage getNextPage()
	{
		if (exportSolutionWizard.getModel().getFileName() == null) return null;

		File f = new File(exportSolutionWizard.getModel().getFileName());
		if (f.exists())
		{
			if (!f.getAbsolutePath().equals(exportSolutionWizard.getModel().getUserAcknowledgedFileToOverwrite()))
			{
				MessageBox msg = new MessageBox(this.getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL);
				msg.setText("File already exists");
				msg.setMessage("The file you selected already exists on disk. Do you want to overwrite it?");
				if (msg.open() == SWT.YES)
				{
					exportSolutionWizard.getModel().setUserAcknowledgedFileToOverwrite(f.getAbsolutePath());
					return exportSolutionWizard.getExportOptionsPage();
				}
				else
				{
					return null;
				}
			}
			// User already acknowledged overwriting this file, and the file name was not
			// changed in the meantime.
			else
			{
				return exportSolutionWizard.getExportOptionsPage();
			}
		}
		else
		{
			return exportSolutionWizard.getExportOptionsPage();
		}
	}

}