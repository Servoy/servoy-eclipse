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

package com.servoy.eclipse.mobileexporter.ui.wizard;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.mobileexporter.export.MobileExporter;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.FlatTreeContentProvider;
import com.servoy.eclipse.ui.labelproviders.SupportNameLabelProvider;
import com.servoy.eclipse.ui.views.TreeSelectViewer;
import com.servoy.eclipse.ui.wizards.FinishPage;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.SolutionMetaData;

/**
 * @author lvostinar
 *
 */
public class WarExportPage extends WizardPage
{
	public static String OUTPUT_PATH_KEY = "initialOutputPath";
	public static String SERVER_URL_KEY = "serverURL";

	private Text outputText;
	private Button outputBrowseButton;
	private Text serverURL;
	private TreeSelectViewer solutionSelectViewer;
	private Button exportAsWar;
	private Text phoneGapUsername;
	private Text phoneGapPassword;
	private final FinishPage finishPage;
	private final PhoneGapApplicationPage pgAppPage;

	public WarExportPage(String pageName, String title, ImageDescriptor titleImage, FinishPage finishPage, PhoneGapApplicationPage pgAppPage)
	{
		super(pageName, title, titleImage);
		this.finishPage = finishPage;
		this.pgAppPage = pgAppPage;
	}

	public void createControl(Composite parent)
	{
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);

		Label outputLabel = new Label(container, SWT.NONE);
		outputLabel.setText("Output directory");

		outputText = new Text(container, SWT.BORDER);
		outputBrowseButton = new Button(container, SWT.NONE);
		outputBrowseButton.setText("Browse");

		Label serverURLLabel = new Label(container, SWT.NONE);
		serverURLLabel.setText("Application Server URL");

		serverURL = new Text(container, SWT.BORDER);
		serverURL.setToolTipText("This is the URL of Servoy Application Server used by mobile client to synchronize data");

		final Shell outputBrowseShell = new Shell();
		outputBrowseButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{

				DirectoryDialog dirdlg = new DirectoryDialog(outputBrowseShell, SWT.NONE);
				if (dirdlg.open() != null)
				{
					outputText.setText(dirdlg.getFilterPath());
				}
			}
		});

		Label solutionLabel = new Label(container, SWT.NONE);
		solutionLabel.setText("Solution");

		solutionSelectViewer = new TreeSelectViewer(container, SWT.None);
		solutionSelectViewer.setButtonText("Browse");
		solutionSelectViewer.setTitleText("Select solution");
		solutionSelectViewer.setName("warExportDialog");
		solutionSelectViewer.setContentProvider(FlatTreeContentProvider.INSTANCE);
		solutionSelectViewer.setLabelProvider(SupportNameLabelProvider.INSTANCE_DEFAULT_NONE);

		try
		{
			solutionSelectViewer.setInput(ServoyModel.getDeveloperRepository().getRootObjectMetaDatasForType(IRepository.SOLUTIONS));
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}

		if (ServoyModelFinder.getServoyModel().getActiveProject() != null)
		{
			SolutionMetaData activeSolution = ServoyModelFinder.getServoyModel().getActiveProject().getSolutionMetaData();
			if (activeSolution != null)
			{
				solutionSelectViewer.setSelection(new StructuredSelection(activeSolution));
			}
		}
		Control solutionSelectControl = solutionSelectViewer.getControl();

		Label typeLabel = new Label(container, SWT.NONE);
		typeLabel.setText("Export type"); //$NON-NLS-1$

		exportAsWar = new Button(container, SWT.RADIO);
		exportAsWar.setText("Export as War"); //$NON-NLS-1$
		exportAsWar.setSelection(true);

		Button exportUsingPhoneGap = new Button(container, SWT.RADIO);

		Link phoneGapLink = new Link(container, SWT.NONE);
		phoneGapLink.setText("Export using <A>PhoneGap build</A>");
		phoneGapLink.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				try
				{
					IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
					IWebBrowser browser = support.getExternalBrowser();
					browser.openURL(new URL("https://build.phonegap.com"));
				}
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
				}
			}
		});

		Label lblPhoneGapUsername = new Label(container, SWT.NONE);
		lblPhoneGapUsername.setText("Email");

		phoneGapUsername = new Text(container, SWT.BORDER);

		Label lblPhoneGapPassword = new Label(container, SWT.NONE);
		lblPhoneGapPassword.setText("Password");

		phoneGapPassword = new Text(container, SWT.BORDER | SWT.PASSWORD);


		final GroupLayout groupLayout = new GroupLayout(container);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.LEADING, false).add(solutionLabel).add(serverURLLabel).add(typeLabel)).addPreferredGap(
				LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(solutionSelectControl, GroupLayout.DEFAULT_SIZE, 480, Short.MAX_VALUE).add(serverURL,
					GroupLayout.PREFERRED_SIZE, 400, Short.MAX_VALUE).add(exportAsWar, GroupLayout.PREFERRED_SIZE, 400, Short.MAX_VALUE).add(
					groupLayout.createSequentialGroup().add(18).add(outputLabel).addPreferredGap(LayoutStyle.RELATED).add(outputText, GroupLayout.DEFAULT_SIZE,
						130, Short.MAX_VALUE).addPreferredGap(LayoutStyle.RELATED).add(outputBrowseButton, GroupLayout.PREFERRED_SIZE, 80,
						GroupLayout.PREFERRED_SIZE)).add(
					groupLayout.createSequentialGroup().add(exportUsingPhoneGap, GroupLayout.PREFERRED_SIZE, 15, GroupLayout.PREFERRED_SIZE).add(phoneGapLink,
						GroupLayout.PREFERRED_SIZE, 150, Short.MAX_VALUE)).add(
					groupLayout.createSequentialGroup().add(20).add(
						groupLayout.createParallelGroup(GroupLayout.LEADING, false).add(lblPhoneGapUsername).add(lblPhoneGapPassword)).addPreferredGap(
						LayoutStyle.RELATED).add(
						groupLayout.createParallelGroup(GroupLayout.LEADING).add(phoneGapUsername, GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE).add(
							phoneGapPassword, GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE)))).addContainerGap()));

		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(solutionSelectControl, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(solutionLabel)).add(7).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(serverURL, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(serverURLLabel)).add(10).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(
					groupLayout.createSequentialGroup().add(exportAsWar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).add(
						10).add(
						groupLayout.createParallelGroup(GroupLayout.BASELINE).add(outputBrowseButton).add(outputText, GroupLayout.PREFERRED_SIZE,
							GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).add(outputLabel)).add(10).add(
						groupLayout.createParallelGroup(GroupLayout.BASELINE).add(phoneGapLink).add(exportUsingPhoneGap, GroupLayout.PREFERRED_SIZE,
							GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).add(
						groupLayout.createSequentialGroup().add(10).add(
							groupLayout.createParallelGroup(GroupLayout.BASELINE).add(phoneGapUsername, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE).add(lblPhoneGapUsername)).add(7).add(
							groupLayout.createParallelGroup(GroupLayout.BASELINE).add(phoneGapPassword, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE).add(lblPhoneGapPassword)))).add(typeLabel))));

		container.setLayout(groupLayout);

		String defaultPath = getDialogSettings().get(OUTPUT_PATH_KEY);
		if (defaultPath != null)
		{
			outputText.setText(defaultPath);
		}
		String defaultServerURL = getDialogSettings().get(SERVER_URL_KEY);
		if (defaultServerURL == null)
		{
			defaultServerURL = "http://127.0.0.1:8080";
		}
		serverURL.setText(defaultServerURL);
		ModifyListener errorMessageDetecter = new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				updateWizardState();
			}
		};
		outputText.addModifyListener(errorMessageDetecter);
		serverURL.addModifyListener(errorMessageDetecter);
		phoneGapUsername.addModifyListener(errorMessageDetecter);
		phoneGapPassword.addModifyListener(errorMessageDetecter);
		SelectionListener selectionListener = new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				updateWizardState();
				enableOption();
			}
		};
		exportAsWar.addSelectionListener(selectionListener);
		exportUsingPhoneGap.addSelectionListener(selectionListener);
		enableOption(true);
	}

	private void updateWizardState()
	{
		setErrorMessage(null);
		WarExportPage.this.getContainer().updateMessage();
		WarExportPage.this.getContainer().updateButtons();
	}

	private void enableOption()
	{
		if (exportAsWar.getSelection())
		{
			enableOption(true);
		}
		else
		{
			enableOption(false);
		}
	}

	private void enableOption(boolean enabled)
	{
		outputText.setEnabled(enabled);
		outputBrowseButton.setEnabled(enabled);
		phoneGapUsername.setEnabled(!enabled);
		phoneGapPassword.setEnabled(!enabled);
	}

	public String getOutputFolder()
	{
		return outputText.getText();
	}

	public String getServerURL()
	{
		return serverURL.getText();
	}

	public String getSolution()
	{
		IStructuredSelection selection = (IStructuredSelection)solutionSelectViewer.getSelection();
		return selection.isEmpty() ? null : ((RootObjectMetaData)selection.getFirstElement()).getName();
	}

	public boolean isWarExport()
	{
		return exportAsWar.getSelection();
	}

	@Override
	public boolean isPageComplete()
	{
		return !isCurrentPage();
	}

	@Override
	public boolean canFlipToNextPage()
	{
		return getErrorMessage() == null;
	}

	@Override
	public IWizardPage getNextPage()
	{
		super.setErrorMessage(null);
		if (canFlipToNextPage())
		{
			if (isWarExport())
			{
				finishPage.setTextMessage(doExport());
				return finishPage;
			}
			else
			{
				final String[] errorMessage = new String[1];
				final String username = phoneGapUsername.getText();
				final String password = phoneGapPassword.getText();
				try
				{
					PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress()
					{
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
						{
							errorMessage[0] = pgAppPage.getConnector().loadPhoneGapAcount(username, password);
						}
					});
				}
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
					errorMessage[0] = ex.getMessage();
				}
				if (errorMessage[0] != null)
				{
					setErrorMessage(errorMessage[0]);
					return null;
				}
				pgAppPage.populateExistingApplications();
				pgAppPage.setSolutionName(getSolution());
				pgAppPage.setServerURL(getServerURL());
				return pgAppPage;
			}
		}
		return null;
	}

	private String doExport()
	{
		File outputFile = new File(getOutputFolder());
		new MobileExporter().doExport(outputFile, getServerURL(), getSolution(), false);

		getDialogSettings().put(WarExportPage.OUTPUT_PATH_KEY, getOutputFolder());
		getDialogSettings().put(WarExportPage.SERVER_URL_KEY, getServerURL());
		return "War file was successfully exported to: " + new File(outputFile.getAbsolutePath(), getSolution() + ".war").getAbsolutePath();
	}

	@Override
	public String getErrorMessage()
	{
		if (getSolution() == null || "".equals(getSolution()))
		{
			return "No solution specified";
		}
		if (getServerURL() == null || "".equals(getServerURL()))
		{
			return "No server URL specified";
		}
		if (isWarExport())
		{
			if (getOutputFolder() == null || "".equals(getOutputFolder()))
			{
				return "No output directory specified";
			}
			File outputFile = new File(getOutputFolder());
			if (!outputFile.exists())
			{
				return "Output folder doesn't exist.";
			}
			if (!outputFile.isDirectory())
			{
				return "Output path is not a folder.";
			}
			if (!outputFile.canWrite())
			{
				return "Output folder cannot be written.";
			}
		}
		else
		{
			if (phoneGapUsername.getText() == null || "".equals(phoneGapUsername.getText()))
			{
				return "No PhoneGap username specified.";
			}
			if (phoneGapPassword.getText() == null || "".equals(phoneGapPassword.getText()))
			{
				return "No PhoneGap password specified.";
			}
		}
		return super.getErrorMessage();
	}
}
