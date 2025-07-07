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

package com.servoy.eclipse.exporter.mobile.ui.wizard;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import com.servoy.eclipse.exporter.mobile.ui.wizard.ExportMobileWizard.CustomizedFinishPage;
import com.servoy.eclipse.exporter.mobile.ui.wizard.ExportMobileWizard.IMobileExportPropertiesPage;
import com.servoy.eclipse.model.mobile.exporter.MobileExporter;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;

/**
 * @author lvostinar
 *
 */
public class WarExportPage extends WizardPage implements IMobileExportPropertiesPage
{
	public static String OUTPUT_PATH_KEY = "initialOutputPath";
	public static String PHONEGAP_EMAIL = "phonegapEmail";
	public static final String SECURE_STORAGE_ACCOUNTS_NODE = "PhoneGap Account Storage";
	public static final String NO_PHONEGAP_ACCOUNT = "-none-";
	public static final String WAR_EXPORT_TYPE_KEY = "isWarExport";
	public static final String IN_SECURE_STORE = "inSecureStore";

	private Text outputText;
	private Button outputBrowseButton;
	private Button exportAsWar;
	private Combo phoneGapUsername;
	private Text phoneGapPassword;
	private Button savePasswordCheck;
	private final CustomizedFinishPage finishPage;
	private final PhoneGapApplicationPage pgAppPage;
	private final MobileExporter mobileExporter;

	public WarExportPage(String pageName, String title, ImageDescriptor titleImage, CustomizedFinishPage finishPage, PhoneGapApplicationPage pgAppPage,
		MobileExporter mobileExporter)
	{
		super(pageName, title, titleImage);
		this.finishPage = finishPage;
		this.pgAppPage = pgAppPage;
		this.mobileExporter = mobileExporter;
	}

	public void createControl(Composite parent)
	{
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);

		Label outputLabel = new Label(container, SWT.NONE);
		outputLabel.setText("Output directory");

		outputText = new Text(container, SWT.BORDER);
		outputText.addModifyListener(new ModifyListener()
		{
			@Override
			public void modifyText(ModifyEvent e)
			{
				File outputFile = new File(getOutputFolder());
				mobileExporter.setOutputFolder(outputFile);
			}
		});
		outputBrowseButton = new Button(container, SWT.NONE);
		outputBrowseButton.setText("Browse");

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

		Label typeLabel = new Label(container, SWT.NONE);
		typeLabel.setText("Export type");

		exportAsWar = new Button(container, SWT.RADIO);
		exportAsWar.setText("Export as War");

		final Button exportUsingPhoneGap = new Button(container, SWT.RADIO);
		Link phoneGapLink = new Link(container, SWT.NONE);
		phoneGapLink.setText("Export using <A>PhoneGap build</A>");
		phoneGapLink.addMouseListener(new MouseListener()
		{

			@Override
			public void mouseUp(MouseEvent e)
			{
				exportUsingPhoneGap.setSelection(true);
				exportAsWar.setSelection(false);
				setExportType();
				updateWizardState();
			}

			@Override
			public void mouseDown(MouseEvent e)
			{
			}

			@Override
			public void mouseDoubleClick(MouseEvent e)
			{
			}
		});
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

		Label lblPhoneGapAccount = new Label(container, SWT.NONE);
		lblPhoneGapAccount.setText("Select a PhoneGap account");

		Label lblPhoneGapUsername = new Label(container, SWT.NONE);
		lblPhoneGapUsername.setText("Email");

		phoneGapUsername = new Combo(container, SWT.BORDER);

		Label lblPhoneGapPassword = new Label(container, SWT.NONE);
		lblPhoneGapPassword.setText("Password");

		phoneGapPassword = new Text(container, SWT.BORDER | SWT.PASSWORD);

		savePasswordCheck = new Button(container, SWT.CHECK);
		savePasswordCheck.setText("Store in Secure Storage");

		final GroupLayout groupLayout = new GroupLayout(container);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(groupLayout.createParallelGroup(GroupLayout.LEADING, false).add(typeLabel))
				.addPreferredGap(
					LayoutStyle.RELATED)
				.add(
					groupLayout.createParallelGroup(GroupLayout.LEADING).add(exportAsWar, GroupLayout.PREFERRED_SIZE, 400, Short.MAX_VALUE).add(
						groupLayout.createSequentialGroup().add(18).add(outputLabel).addPreferredGap(LayoutStyle.RELATED)
							.add(outputText, GroupLayout.DEFAULT_SIZE,
								130, Short.MAX_VALUE)
							.addPreferredGap(LayoutStyle.RELATED).add(outputBrowseButton, GroupLayout.PREFERRED_SIZE, 80,
								GroupLayout.PREFERRED_SIZE))
						.add(
							groupLayout.createSequentialGroup().add(exportUsingPhoneGap, GroupLayout.PREFERRED_SIZE, 15, GroupLayout.PREFERRED_SIZE).add(
								phoneGapLink,
								GroupLayout.PREFERRED_SIZE, 150, Short.MAX_VALUE))
						.add(
							groupLayout.createSequentialGroup().add(20).add(
								groupLayout.createParallelGroup(GroupLayout.LEADING, false).add(lblPhoneGapUsername).add(lblPhoneGapPassword)).addPreferredGap(
									LayoutStyle.RELATED)
								.add(
									groupLayout.createParallelGroup(GroupLayout.LEADING).add(phoneGapUsername, GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE)
										.add(
											phoneGapPassword, GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE)))
						.add(
							groupLayout.createSequentialGroup().add(20).add(savePasswordCheck).addPreferredGap(LayoutStyle.RELATED)))
				.addContainerGap()));

		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(
					groupLayout.createSequentialGroup().add(exportAsWar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).add(
						10).add(
							groupLayout.createParallelGroup(GroupLayout.BASELINE).add(outputBrowseButton).add(outputText, GroupLayout.PREFERRED_SIZE,
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).add(outputLabel))
						.add(10).add(
							groupLayout.createParallelGroup(GroupLayout.BASELINE).add(phoneGapLink).add(exportUsingPhoneGap, GroupLayout.PREFERRED_SIZE,
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.add(10).add(
							groupLayout.createSequentialGroup().add(10).add(
								groupLayout.createParallelGroup(GroupLayout.BASELINE)
									.add(phoneGapUsername, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE)
									.add(lblPhoneGapUsername))
								.add(7).add(
									groupLayout.createParallelGroup(GroupLayout.BASELINE)
										.add(phoneGapPassword, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
											GroupLayout.PREFERRED_SIZE)
										.add(lblPhoneGapPassword)))
						.add(7).add(
							groupLayout.createParallelGroup(GroupLayout.BASELINE).add(savePasswordCheck, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE)))
					.add(typeLabel))));

		container.setLayout(groupLayout);

		String defaultPath = getDialogSettings().get(OUTPUT_PATH_KEY);
		if (defaultPath != null)
		{
			outputText.setText(defaultPath);
		}
		else
		{
			File webappsFolder = new File(ApplicationServerRegistry.get().getServoyApplicationServerDirectory(), "server/webapps");
			outputText.setText(webappsFolder.getAbsolutePath());
		}

		savePasswordCheck.setSelection(getDialogSettings().getBoolean(IN_SECURE_STORE));
		//load existing PhoneGap accounts
		loadPhoneGapAccounts();

		ModifyListener errorMessageDetecter = new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				updateWizardState();
			}
		};
		outputText.addModifyListener(errorMessageDetecter);
		phoneGapUsername.addModifyListener(errorMessageDetecter);
		phoneGapPassword.addModifyListener(errorMessageDetecter);
		SelectionListener selectionListener = new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				setExportType();
				updateWizardState();
			}
		};
		exportAsWar.addSelectionListener(selectionListener);
		exportUsingPhoneGap.addSelectionListener(selectionListener);

		boolean isWarExport = getDialogSettings().get(WAR_EXPORT_TYPE_KEY) != null ? getDialogSettings().getBoolean(WAR_EXPORT_TYPE_KEY) : true;
		exportAsWar.setSelection(isWarExport);
		exportUsingPhoneGap.setSelection(!isWarExport);
		setExportType();
	}

	private void loadPhoneGapAccounts()
	{
		// check for PhoneGap secure storage
		ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();
		if (securePreferences.nodeExists(SECURE_STORAGE_ACCOUNTS_NODE))
		{
			final ISecurePreferences node = securePreferences.node(SECURE_STORAGE_ACCOUNTS_NODE);
			try
			{
				boolean foundLastAccountUsed = false;
				for (String emailKey : node.keys())
				{
					String password = node.get(emailKey, null);
					if (password != null) phoneGapUsername.add(emailKey);
					if (getDialogSettings().get(PHONEGAP_EMAIL) != null && getDialogSettings().get(PHONEGAP_EMAIL).equals(emailKey))
					{
						// select last account used
						phoneGapUsername.select(phoneGapUsername.indexOf(emailKey));
						phoneGapPassword.setText(node.get(emailKey, null));
						foundLastAccountUsed = true;
					}
				}
				if (!foundLastAccountUsed)
				{
					if (getDialogSettings().get(PHONEGAP_EMAIL) != null)
					{
						// fill in the last used email address
						phoneGapUsername.setText(getDialogSettings().get(PHONEGAP_EMAIL));
					}
					else
					{
						// select the first element in accounts list
						phoneGapUsername.select(0);
						if (phoneGapUsername.getText() != null) phoneGapPassword.setText(node.get(phoneGapUsername.getText(), null));
					}
				}
			}
			catch (StorageException e)
			{
				Debug.error(e);
			}
			phoneGapUsername.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					String userEmail = phoneGapUsername.getItem(phoneGapUsername.getSelectionIndex());
					try
					{
						phoneGapPassword.setText(node.get(userEmail, null));
					}
					catch (StorageException ex)
					{
						Debug.error(ex);
					}
				}
			});
		}
		else if (getDialogSettings().get(PHONEGAP_EMAIL) != null)
		{
			phoneGapUsername.setText(getDialogSettings().get(PHONEGAP_EMAIL)); // fill in the last used email address
		}
	}

	private void updateWizardState()
	{
		setErrorMessage(null);
		WarExportPage.this.getContainer().updateMessage();
		WarExportPage.this.getContainer().updateButtons();
		getDialogSettings().put(WarExportPage.WAR_EXPORT_TYPE_KEY, isWarExport());
	}

	private void setExportType()
	{
		if (exportAsWar.getSelection())
		{
			setExportType(true);
		}
		else
		{
			setExportType(false);
		}
	}

	private void setExportType(boolean isDefault)
	{
		outputText.setEnabled(isDefault);
		outputBrowseButton.setEnabled(isDefault);
		phoneGapUsername.setEnabled(!isDefault);
		phoneGapPassword.setEnabled(!isDefault);
	}

	private String getOutputFolder()
	{
		return outputText.getText();
	}

	public boolean isWarExport()
	{
		return exportAsWar.getSelection();
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
				exportWar();
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

							// in case one uses his github account to login to phonegap
//							errorMessage[0] = pgAppPage.getConnector().loadPhoneGapAcount("BTsF5NMzv2NdKxQ9noCq"); // your Phonegap Build token here (generated on the site)
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
				return pgAppPage;
			}
		}
		return null;
	}

	public String exportWar()
	{
		String serverDir = ApplicationServerRegistry.get().getServoyApplicationServerDirectory();
		if (!serverDir.endsWith(File.separator)) serverDir = serverDir + File.separator;
		serverDir = new StringBuilder(serverDir).append("server").append(File.separator).append("webapps").append(File.separator).toString();
		String outputFolder = getOutputFolder();
		if (!outputFolder.endsWith(File.pathSeparator)) outputFolder = outputFolder + File.separator;

		EditorUtil.saveDirtyEditors(getShell(), true);
		boolean isLocalDeploy = serverDir.equalsIgnoreCase(outputFolder);
		String error = doExport(isLocalDeploy && finishPage.isOpenUrl() ? 3000 : 0);
		if (isLocalDeploy)
		{
			finishPage.setApplicationURL(new StringBuilder("http://localhost:").append(ApplicationServerRegistry.get().getWebServerPort()).append("/").append(
				mobileExporter.getSolutionName()).append("/index.html").toString(), "Open WAR application in browser at finish.", false);
			finishPage.createControl(WarExportPage.this.getControl().getParent());
			finishPage.getControl().getParent().layout(true);
		}
		finishPage.setTextMessage(error == null ? "War file was successfully exported to: " +
			new File(mobileExporter.getOutputFolder().getAbsolutePath(), mobileExporter.getSolutionName() + ".war").getAbsolutePath()
			: "Unexpected exception while exporting war file: " + error);
		return error;
	}

	private String doExport(final long delayAfterExport)
	{
		final String[] errorMessage = new String[1];

		IRunnableWithProgress job = new IRunnableWithProgress()
		{
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
			{
				try
				{
					mobileExporter.doExport(false, monitor);
				}
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
					errorMessage[0] = ex.getMessage();
				}
				if (delayAfterExport > 0) Thread.sleep(delayAfterExport);
			}
		};
		try
		{
			getContainer().run(true, false, job);
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		return errorMessage[0];
	}

	@Override
	public String getErrorMessage()
	{
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

	String getPassword()
	{
		return phoneGapPassword.getText();
	}

	String getUsername()
	{
		return phoneGapUsername.getText();
	}

	@Override
	public boolean saveProperties()
	{
		if (isWarExport())
		{
			getDialogSettings().put(WarExportPage.OUTPUT_PATH_KEY, getOutputFolder());
		}
		else
		{
			getDialogSettings().put(WarExportPage.PHONEGAP_EMAIL, phoneGapUsername.getText());
			getDialogSettings().put(WarExportPage.IN_SECURE_STORE, savePasswordCheck.getSelection());
			if (savePasswordCheck.getSelection())
			{
				// new PhoneGap account or updating existing account
				ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();
				ISecurePreferences node = securePreferences.node(SECURE_STORAGE_ACCOUNTS_NODE);
				String password = phoneGapPassword.getText();
				String username = phoneGapUsername.getText();
				try
				{
					if (!password.equals(node.get(username, null)))
					{
						node.put(username, password, true);
					}
				}
				catch (StorageException e)
				{
					Debug.error(e);
					setErrorMessage("Cannot save the password to Secure Storage: " + e.getMessage() + " Please check your Secure Storage settings.");
					return false;
				}
			}
		}
		return true;
	}
}