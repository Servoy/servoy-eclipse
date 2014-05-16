/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.eclipse.warexporter.ui.wizard;

import java.io.File;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.warexporter.export.ExportWarModel;

/**
 * 
 * @author jcompagner
 * @since 6.1
 */
public class FileSelectionPage extends WizardPage implements Listener
{
	/**
	 * 
	 */
	private final ExportWarModel exportModel;
	private Text fileNameText;
	private Button browseButton;
	private Button exportActiveSolution;
	private final IWizardPage nextPage;

	public FileSelectionPage(ExportWarModel exportModel, IWizardPage nextPage)
	{
		super("warfileselection");
		this.exportModel = exportModel;
		this.nextPage = nextPage;
		setTitle("Choose the destination file");
		setDescription("Select the file where you want your solution exported to");
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
		if (exportModel.getFileName() != null) fileNameText.setText(exportModel.getFileName());

		browseButton = new Button(composite, SWT.PUSH);
		browseButton.setText("Browse...");
		browseButton.addListener(SWT.Selection, this);

		exportActiveSolution = new Button(composite, SWT.CHECK);
		exportActiveSolution.setText("Include active solution and modules");
		exportActiveSolution.setSelection(exportModel.isExportActiveSolutionOnly());
		exportActiveSolution.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				exportModel.setExportActiveSolutionOnly(exportActiveSolution.getSelection());
			}
		});

		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.verticalIndent = 5;
		exportActiveSolution.setLayoutData(gd);
		setControl(composite);
	}

	public void handleEvent(Event event)
	{
		if (event.widget == fileNameText)
		{
			String potentialFileName = fileNameText.getText();
			exportModel.setFileName(potentialFileName);
		}
		else if (event.widget == browseButton)
		{
			Shell shell = new Shell();
			GridLayout gridLayout = new GridLayout();
			shell.setLayout(gridLayout);
			FileDialog dlg = new FileDialog(shell, SWT.SAVE);
			if (exportModel.getFileName() != null)
			{
				File f = new File(exportModel.getFileName());
				if (f.isDirectory())
				{
					dlg.setFilterPath(f.getAbsolutePath());
					dlg.setFileName(null);
				}
				else
				{
					dlg.setFilterPath(f.getParent());
					dlg.setFileName(f.getName());
				}
			}
			String[] extensions = { "*.war" };
			dlg.setFilterExtensions(extensions);
			String chosenFileName = dlg.open();
			if (chosenFileName != null)
			{
				exportModel.setFileName(chosenFileName);
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
		boolean messageSet = false;
		if (exportModel.getFileName() == null) return false;
		if (exportActiveSolution.getSelection() && ServoyModelFinder.getServoyModel().getActiveProject() == null)
		{
			setMessage("There is no active solution.", IMessageProvider.WARNING);
			result = false;
			messageSet = true;
		}
		if (fileNameText.getText().length() == 0)
		{
			result = false;
		}
		else
		{
			File f = new File(exportModel.getFileName());
			if (f.exists())
			{
				if (f.isDirectory())
				{
					setMessage("Specified path points to an existing folder.", IMessageProvider.WARNING);
					result = false;
					messageSet = true;
				}
				else
				{
					setMessage("Specified path points to an existing file.", IMessageProvider.INFORMATION);
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
		if (exportModel.getFileName() == null) return null;

		File f = new File(exportModel.getFileName());
		if (f.exists())
		{
			MessageBox msg = new MessageBox(this.getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.CANCEL);
			msg.setText("File already exists");
			msg.setMessage("The file you selected already exists on disk. Do you want to overwrite it?");
			if (msg.open() == SWT.YES)
			{
				return nextPage;
			}
			else
			{
				return null;
			}
		}
		else
		{
			return nextPage;
		}
	}
}