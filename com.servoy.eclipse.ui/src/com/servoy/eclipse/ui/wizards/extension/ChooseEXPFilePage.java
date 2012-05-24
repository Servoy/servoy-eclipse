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

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.extension.ComposedExtensionProvider;
import com.servoy.extension.DependencyMetadata;
import com.servoy.extension.FileBasedExtensionProvider;
import com.servoy.extension.MarketPlaceExtensionProvider;
import com.servoy.extension.Message;
import com.servoy.extension.parser.EXPParser;

/**
 * Page for choosing a local .exp file to install. It also allows the user to choose searching for dependencies in parent folder and in the marketplace.
 * @author acostescu
 */
public class ChooseEXPFilePage extends WizardPage
{

	protected InstallExtensionState state;
	protected InstallExtensionWizardOptions dialogOptions;
	protected Button useMarketplace;
	protected Button useFolder;

	/**
	 * Creates a new page for choosing file based import sources.
	 * @param pageName see super.
	 * @param state the state of the install extension process. Will be filled with info retrieved from this page in order for the wizard to go forward.
	 * @param dialogOptions initial check-box states are held in this object; they will be updated.
	 */
	public ChooseEXPFilePage(String pageName, InstallExtensionState state, InstallExtensionWizardOptions dialogOptions)
	{
		super(pageName);
		this.state = state;
		this.dialogOptions = dialogOptions;

		setTitle("Please select an extension package"); //$NON-NLS-1$
		setDescription("An extension will be installed from the selected .exp file."); //$NON-NLS-1$
	}

	public void createControl(Composite parent)
	{
		initializeDialogUnits(parent);

		Composite topLevel = new Composite(parent, SWT.NONE);
		topLevel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
		setControl(topLevel);

		final Text filePath = new Text(topLevel, SWT.BORDER);
		filePath.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				state.expFile = filePath.getText();
				getContainer().updateButtons(); // this will validate as well
			}

		});

		Button browseButton = new Button(topLevel, SWT.NONE);
		browseButton.setText("Browse"); //$NON-NLS-1$
		browseButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				FileDialog dlg = new FileDialog(getShell(), SWT.NONE);
				dlg.setFilterExtensions(new String[] { "*.exp", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
				if (dialogOptions.fileImportParent != null) dlg.setFilterPath(dialogOptions.fileImportParent);
				if (dlg.open() != null)
				{
					dialogOptions.fileImportParent = dlg.getFilterPath();
					filePath.setText(dlg.getFilterPath() + File.separator + dlg.getFileName());
				}
			}
		});

		useFolder = new Button(topLevel, SWT.CHECK);
		useFolder.setText("search for dependencies in containing folder"); //$NON-NLS-1$
		useFolder.setSelection(dialogOptions.useLocalFolderForDependencies);
		useFolder.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				dialogOptions.useLocalFolderForDependencies = useFolder.getSelection();
			}
		});

		useMarketplace = new Button(topLevel, SWT.CHECK);
		useMarketplace.setText("search for dependencies in Servoy Marketplace"); //$NON-NLS-1$
		useMarketplace.setSelection(dialogOptions.useMarketplaceForDependencies);
		useMarketplace.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				dialogOptions.useMarketplaceForDependencies = useMarketplace.getSelection();
			}
		});

		// layout the page
		FormLayout formLayout = new FormLayout();
		formLayout.spacing = 5;
		formLayout.marginWidth = 10;
		formLayout.marginHeight = 10;
		topLevel.setLayout(formLayout);

		FormData formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.top = new FormAttachment(browseButton, 0, SWT.CENTER);
		formData.right = new FormAttachment(100, -100);
		filePath.setLayoutData(formData);

		formData = new FormData();
		formData.left = new FormAttachment(filePath, 0);
		formData.top = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		browseButton.setLayoutData(formData);

		formData = new FormData();
		formData.left = new FormAttachment(0, 10);
		formData.top = new FormAttachment(filePath, 0);
		formData.right = new FormAttachment(100, 0);
		useFolder.setLayoutData(formData);

		formData = new FormData();
		formData.left = new FormAttachment(0, 10);
		formData.top = new FormAttachment(useFolder, 0);
		formData.right = new FormAttachment(100, 0);
		useMarketplace.setLayoutData(formData);
	}

	private void validate()
	{
		String error = null;
		if (state.expFile != null && state.expFile.length() > 0)
		{
			File f = new File(state.expFile);
			if (!f.isAbsolute())
			{
				error = "Absolute path required."; //$NON-NLS-1$
			}
			else if (!(f.exists() && f.isFile()))
			{
				error = "You must select a valid .exp file."; //$NON-NLS-1$
			}
			else
			{
				EXPParser parser = state.getOrCreateParser(f);
				DependencyMetadata dependencyMetadata = parser.parseDependencyInfo();

				if (dependencyMetadata == null)
				{
					Message[] warn = parser.getMessages();
					error = "Cannot parse selected .exp file." + System.getProperty("line.separator") + (warn.length > 0 ? shortenIfNeeded(warn[0].message) : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				else
				{
					state.extensionID = dependencyMetadata.id;
					state.version = dependencyMetadata.version;
					dialogOptions.fileImportParent = f.getParent();
				}
			}
		}

		setErrorMessage(error);
	}

	@SuppressWarnings("nls")
	private String shortenIfNeeded(String message)
	{
		String shortened = message.replace("\n", " ").replace("\r", ""); // flatten to 1 line
		if (shortened.length() > 71) shortened = "(...) " + shortened.substring(shortened.length() - 71); // keep last part

		if (message.length() > shortened.length()) ServoyLog.logInfo("Shortened error message when validating .exp file: " + message);

		return shortened;
	}

	@Override
	public boolean canFlipToNextPage()
	{
		validate();
		if (getErrorMessage() == null && state.expFile != null && state.expFile.trim().length() > 0)
		{
			return true;
		}

		return false;
	}

	@Override
	public IWizardPage getNextPage()
	{
		File f = new File(state.expFile);

		FileBasedExtensionProvider fbep;
		if (useFolder.getSelection() == true)
		{
			fbep = new FileBasedExtensionProvider(f.getParentFile(), true, state);
		}
		else
		{
			fbep = new FileBasedExtensionProvider(f, false, state);
		}

		if (useMarketplace.getSelection() == true)
		{
			state.extensionProvider = new ComposedExtensionProvider(fbep, new MarketPlaceExtensionProvider(state.installDir));
		}
		else
		{
			state.extensionProvider = fbep;
		}

		// second page is always the dependency resolving page; when we reach it, first pages should have already set the extensionID/version pair in 'state'
		DependencyResolvingPage next = new DependencyResolvingPage("DepResolver", state, dialogOptions, true); //$NON-NLS-1$
		next.setWizard(getWizard());
		return next;
	}

}
