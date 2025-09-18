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
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.osgi.service.prefs.BackingStoreException;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.BuilderUtils;
import com.servoy.eclipse.model.mobile.exporter.MobileExporter;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.wizards.DirtySaveExportWizard;
import com.servoy.eclipse.ui.wizards.FinishPage;
import com.servoy.eclipse.warexporter.export.ExportWarModel;

public class ExportMobileWizard extends DirtySaveExportWizard implements IExportWizard
{
	private static final String PROPERTY_IS_OPEN_URL = "isOpenURL";

	private static final String CAN_FINISH = "canFinish_";

	private final MobileExporter mobileExporter;

	private final CustomizedFinishPage finishPage;

	private final PhoneGapApplicationPage pgAppPage;

	private final WarExportPage warExportPage;

	private final LicensePage licensePage;

	private final MediaOrderPage mediaOrderPage;

	private final ExportOptionsPage optionsPage;

	private WizardPage errorPage;


	public ExportMobileWizard()
	{
		finishPage = new CustomizedFinishPage("lastPage");
		mobileExporter = new MobileExporter(new ExportWarModel(ExportWarModel.getDialogSettings())
		{
			@Override
			public String exportNG2Mode()
			{
				return "build_mobile";
			}
		});
		pgAppPage = new PhoneGapApplicationPage("PhoneGap Application", mobileExporter);
		warExportPage = new WarExportPage("outputPage", "Choose output", null, finishPage, pgAppPage, mobileExporter);
		pgAppPage.setWarExportPage(warExportPage);
		licensePage = new LicensePage("licensePage", mobileExporter);
		mediaOrderPage = new MediaOrderPage("mediaOrderPage", mobileExporter);
		optionsPage = new ExportOptionsPage("optionsPage", mobileExporter);

		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("MobileExportWizard");
		if (section == null)
		{
			section = workbenchSettings.addNewSection("MobileExportWizard");
		}
		setDialogSettings(section);
		finishPage.setTitle("Export finished");
		setWindowTitle("Mobile Export");
	}

	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		ServoyProject activeProject;
		if ((activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject()) == null)
		{
			createErrorPage("No active Servoy solution project found", "No active Servoy solution project found",
				"Please activate a Servoy solution project before trying to export");
		}
		else if (BuilderUtils.getMarkers(activeProject) == BuilderUtils.HAS_ERROR_MARKERS)
		{
			createErrorPage("Solution with errors", "Solution with errors", "Cannot export solution with errors, please fix them first");
		}
		else
		{
			mobileExporter.setSolutionName(activeProject.getSolution().getName());
		}
	}

	private void createErrorPage(String pageName, String title, String errorMessage)
	{
		errorPage = new WizardPage(pageName)
		{
			public void createControl(Composite parent)
			{
				setControl(new Composite(parent, SWT.NONE));
			}
		};
		errorPage.setTitle(title);
		errorPage.setErrorMessage(errorMessage);
		errorPage.setPageComplete(false);
	}

	@Override
	public boolean performFinish()
	{
		if (!saveDialogProperties()) return false;

		boolean finished = false;
		if (warExportPage.isWarExport())
		{
			finished = exportWar();
		}
		else
		{
			finished = exportToPhoneGap();
		}
		getDialogSettings().put(CAN_FINISH + mobileExporter.getSolutionName(), finished);
		return finished;
	}

	/**
	 * Save all dialog properties on finish.
	 */
	private boolean saveDialogProperties()
	{
		boolean saved = true;
		for (int i = 0; i < getPageCount(); i++)
		{
			IWizardPage page = getPages()[i];
			if (page instanceof IMobileExportPropertiesPage)
			{
				saved = saved && ((IMobileExportPropertiesPage)page).saveProperties();
			}
		}
		return saved;
	}

	private boolean exportToPhoneGap()
	{
		try
		{
			final PhoneGapApplication app = pgAppPage.getPhoneGapApplication();
			final File configFile = pgAppPage.getConfigFile();
			final boolean openPhoneGapUrl = pgAppPage.openPhoneGapUrl();
			final String username = warExportPage.getUsername();
			final String password = warExportPage.getPassword();
			Job uploadToPhoneGap = new Job("Uploading to PhoneGap build")
			{
				@Override
				protected IStatus run(IProgressMonitor monitor)
				{
					String error = pgAppPage.getConnector().createOrUpdatePhoneGapApplication(username, password, app, mobileExporter, configFile);
					if (error != null)
					{
						return new Status(IStatus.ERROR, com.servoy.eclipse.exporter.mobile.Activator.PLUGIN_ID, error);
					}

					if (openPhoneGapUrl)
					{
						try
						{
							IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
							IWebBrowser browser = support.getExternalBrowser();
							browser.openURL(new URL("https://build.phonegap.com/people/sign_in"));
						}
						catch (Exception ex)
						{
							ServoyLog.logError(ex);
						}
					}
					return Status.OK_STATUS;
				}
			};
			uploadToPhoneGap.schedule();
		}
		catch (Exception ex)
		{
			((WizardPage)getContainer().getCurrentPage()).setErrorMessage(ex.getMessage());
			return false;
		}
		return true;
	}

	private boolean exportWar()
	{
		//if current page is finish then export was already done
		if (!getContainer().getCurrentPage().equals(finishPage))
		{
			String error = warExportPage.exportWar();
			if (error != null)
			{
				((WizardPage)getContainer().getCurrentPage()).setErrorMessage(error);
				return false;
			}
		}

		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(com.servoy.eclipse.exporter.mobile.Activator.PLUGIN_ID);
		preferences.putBoolean(PROPERTY_IS_OPEN_URL, finishPage.isOpenUrl());
		try
		{
			preferences.flush();
		}
		catch (BackingStoreException e)
		{
			ServoyLog.logError(e);
		}
		if (finishPage.getOpenUrl() != null)
		{
			try
			{
				IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
				IWebBrowser browser = support.getExternalBrowser();
				browser.openURL(new URL(finishPage.getOpenUrl()));
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
		return true;
	}

	//Also used to specify the static order of the pages.
	//Please see http://wiki.eclipse.org/FAQ_How_do_I_specify_the_order_of_pages_in_a_wizard%3F for more info.
	@Override
	public void addPages()
	{
		if (errorPage != null)
		{
			addPage(errorPage);
		}
		else
		{
			addPage(optionsPage);
			if (mobileExporter.getMediaOrder().size() > 0) addPage(mediaOrderPage);
			addPage(licensePage);
			addPage(warExportPage);
			addPage(pgAppPage);
			addPage(finishPage);
		}
	}


	@Override
	public boolean canFinish()
	{
		if (getDialogSettings().getBoolean(CAN_FINISH + mobileExporter.getSolutionName()) || pgAppPage.equals(getContainer().getCurrentPage()) ||
			finishPage.equals(getContainer().getCurrentPage()))
		{
			for (int i = 0; i < getPageCount(); i++)
			{
				IWizardPage page = getPages()[i];
				if (page.getErrorMessage() != null || !page.isPageComplete())
				{
					return false;
				}
			}
			return true;
		}
		return false;
	}


	public class CustomizedFinishPage extends FinishPage
	{
		private String url = null;
		private String urlDescription;
		private boolean urlSelected;
		private Button openURL = null;

		public CustomizedFinishPage(String pageName)
		{
			super(pageName);
		}

		@Override
		public boolean canFlipToNextPage()
		{
			return false;
		}

		@Override
		public void createControl(Composite parent)
		{
			if (url != null)
			{
				Composite container = new Composite(parent, SWT.NONE);
				GridLayout layout = new GridLayout();
				container.setLayout(layout);
				layout.numColumns = 1;

				message = new Text(container, SWT.WRAP | SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
				GridData gridData = new GridData();
				gridData.horizontalAlignment = GridData.FILL;
				gridData.verticalAlignment = GridData.FILL;
				gridData.grabExcessHorizontalSpace = true;
				gridData.grabExcessVerticalSpace = true;
				gridData.horizontalSpan = 1;
				message.setLayoutData(gridData);
				message.setEditable(false);

				openURL = new Button(container, SWT.CHECK);
				openURL.setSelection(urlSelected);
				openURL.setText(urlDescription);
				gridData = new GridData();
				gridData.grabExcessHorizontalSpace = true;
				gridData.grabExcessVerticalSpace = false;
				gridData.horizontalAlignment = GridData.FILL;
				openURL.setLayoutData(gridData);


				setControl(container);
				setPageComplete(true);
			}
			else
			{
				super.createControl(parent);
			}
		}

		public void setApplicationURL(String url, String urlDescription, boolean defaultSelected)
		{
			this.url = url;
			this.urlDescription = urlDescription;
			IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(com.servoy.eclipse.exporter.mobile.Activator.PLUGIN_ID);
			this.urlSelected = preferences.getBoolean(PROPERTY_IS_OPEN_URL, defaultSelected);
		}

		public String getOpenUrl()
		{
			if (isOpenUrl())
			{
				return url;
			}
			return null;
		}

		public boolean isOpenUrl()
		{
			return openURL != null && openURL.getSelection();
		}
	}

	/**
	 * Interface for wizard pages to save last used settings.
	 * @author emera
	 */
	interface IMobileExportPropertiesPage
	{
		public boolean saveProperties();
	}
}
