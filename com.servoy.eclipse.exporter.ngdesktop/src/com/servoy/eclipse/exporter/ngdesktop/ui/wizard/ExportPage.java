/*
  This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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

package com.servoy.eclipse.exporter.ngdesktop.ui.wizard;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.SemVerComparator;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.ServoyLoginDialog;
import com.servoy.eclipse.ui.preferences.NGDesktopConfiguration;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * @author gboros
 */
public class ExportPage extends WizardPage
{

	public static String WINDOWS_PLATFORM = "win";
	public static String MACOS_PLATFORM = "mac";
	public static String LINUX_PLATFORM = "linux";

	private Text applicationUrlText;
	private Composite platformGroup;

	private Text iconPath;
	private Button browseIconButton;
	private Text imgPath;
	private Button browseImgButton;
	private Text copyrightText;
	private Composite sizeGroup;
	private Text widthText;
	private Text heightText;
	private Composite versionGroup;
	private Combo srcVersionCombo;
	private Button includeUpdateBtn;
	private Text appNameText;
	private Text updateUrlText;
	private Text emailAddress;
	private Button storeBtn;
	private Label deprecatedLabel;

	private final List<String> selectedPlatforms = new ArrayList<String>();
	private final ExportNGDesktopWizard exportElectronWizard;
	private final String FIRST_VERSION_THAT_SUPPORTS_UPDATES = "2020.12";
	private List<String> remoteVersions = new ArrayList<String>();

	public ExportPage(ExportNGDesktopWizard exportElectronWizard)
	{
		super("page1");
		this.exportElectronWizard = exportElectronWizard;
		setTitle("Export Servoy application");
	}

	@Override
	public void createControl(Composite parent)
	{
		//final Composite rootComposite = new Composite(parent, SWT.NONE);
		//rootComposite.setLayout(new FormLayout());

		final GridLayout gridLayout = new GridLayout(3, false);
		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(gridLayout);

		final Label applicationUrlLabel = new Label(composite, SWT.NONE);
		applicationUrlLabel.setText("Servoy application URL");
		applicationUrlText = new Text(composite, SWT.BORDER);
		applicationUrlText.setText(getApplicationUrl());

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		applicationUrlText.setLayoutData(gd);

		final Label platformLabel = new Label(composite, SWT.NONE);
		platformLabel.setText("Platform");

		platformGroup = new Composite(composite, SWT.NONE);
		platformGroup.setLayout(new RowLayout(SWT.HORIZONTAL));

		final Button winBtn = new Button(platformGroup, SWT.CHECK);
		winBtn.setData(WINDOWS_PLATFORM);
		winBtn.setText("Windows");
		winBtn.setSelection(exportElectronWizard.getDialogSettings().getBoolean("win_export"));
		if (winBtn.getSelection()) selectedPlatforms.add(WINDOWS_PLATFORM);

		winBtn.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent event)
			{
				platformSelectionChangeListener((String)event.widget.getData());
			}
		});

		final Button macBtn = new Button(platformGroup, SWT.CHECK);
		macBtn.setText("MacOS");
		macBtn.setData(MACOS_PLATFORM);
		macBtn.setSelection(exportElectronWizard.getDialogSettings().getBoolean("osx_export"));
		if (macBtn.getSelection()) selectedPlatforms.add(MACOS_PLATFORM);

		macBtn.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent event)
			{
				platformSelectionChangeListener((String)event.widget.getData());
			}
		});

		final Button linuxBtn = new Button(platformGroup, SWT.CHECK);
		linuxBtn.setText("Linux");
		linuxBtn.setData(LINUX_PLATFORM);
		linuxBtn.setSelection(exportElectronWizard.getDialogSettings().getBoolean("linux_export"));
		if (linuxBtn.getSelection()) selectedPlatforms.add(LINUX_PLATFORM);

		linuxBtn.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent event)
			{
				platformSelectionChangeListener((String)event.widget.getData());
			}
		});

		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		platformGroup.setLayoutData(gd);

		final Label iconLabel = new Label(composite, SWT.NONE);
		iconLabel.setText("Icon path:");
		iconLabel
			.setToolTipText("Logo image (png) used by the NG Desktop Client and the installer.\nMaximum file size (KB): " + ExportNGDesktopWizard.LOGO_SIZE +
				"\nMinimum logo size: 512 x 512 px");

		iconPath = new Text(composite, SWT.BORDER);
		iconPath
			.setToolTipText("Logo image (png) used by the NG Desktop Client and the installer.\nMaximum file size (KB): " + ExportNGDesktopWizard.LOGO_SIZE);
		iconPath.setEditable(true);
		iconPath.setVisible(true);
		String value = exportElectronWizard.getDialogSettings().get("icon_path");
		iconPath.setText(value != null ? value : "");
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		iconPath.setLayoutData(gd);


		browseIconButton = new Button(composite, SWT.NONE);
		browseIconButton.setText("Browse");
		browseIconButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				final FileDialog dlg = new FileDialog(getShell(), SWT.NONE);
				dlg.setFilterExtensions(new String[] { "*.png" });
				final File myFile = new File(getDlgInitPath(iconPath.getText())); //make sure we have a usable path
				dlg.setFilterPath(myFile.isDirectory() ? myFile.getAbsolutePath() : myFile.getParent());
				final String path = dlg.open();
				if (path != null) iconPath.setText(path);
			}
		});

		final Label imgLabel = new Label(composite, SWT.NONE);
		imgLabel.setText("Image path:");
		imgLabel.setToolTipText(
			"Bitmap image used in the installer. For Windows installer the recommended size is 164 px width, 314 px height.\nMaximum file size (KB): " +
				ExportNGDesktopWizard.IMG_SIZE);

		imgPath = new Text(composite, SWT.BORDER);
		imgPath.setToolTipText(
			"Bitmap image used in the installer. For Windows installer the recommended size is 164 px width, 314 px height.\nMaximum file size (KB): " +
				ExportNGDesktopWizard.IMG_SIZE);
		imgPath.setEditable(true);
		imgPath.setVisible(true);
		value = exportElectronWizard.getDialogSettings().get("image_path");
		imgPath.setText(value != null ? value : "");
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		imgPath.setLayoutData(gd);


		browseImgButton = new Button(composite, SWT.NONE);
		browseImgButton.setText("Browse");
		browseImgButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				final FileDialog dlg = new FileDialog(getShell(), SWT.NONE);
				dlg.setFilterExtensions(new String[] { "*.bmp" });
				final File myFile = new File(getDlgInitPath(imgPath.getText())); //make sure we have a usable path
				dlg.setFilterPath(myFile.isDirectory() ? myFile.getAbsolutePath() : myFile.getParent());
				final String path = dlg.open();
				if (path != null) imgPath.setText(path);
			}
		});

		final Label copyrightLabel = new Label(composite, SWT.NONE);
		copyrightLabel.setText("Copyright:");
		copyrightLabel.setToolTipText("The maximum allowed length is " + ExportNGDesktopWizard.COPYRIGHT_LENGTH + " chars");

		copyrightText = new Text(composite, SWT.BORDER);
		copyrightText.setToolTipText("The maximum allowed length is " + ExportNGDesktopWizard.COPYRIGHT_LENGTH + " chars");
		copyrightText.setEditable(true);
		copyrightText.setVisible(true);
		value = exportElectronWizard.getDialogSettings().get("copyright");
		copyrightText.setText(value != null ? value : "");
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		copyrightText.setLayoutData(gd);

		final Label sizeLabel = new Label(composite, SWT.NONE);
		sizeLabel.setText("NG Desktop size:");
		sizeGroup = new Composite(composite, SWT.NONE);
		sizeGroup.setLayout(new GridLayout(4, false));

		widthText = new Text(sizeGroup, SWT.BORDER);
		value = exportElectronWizard.getDialogSettings().get("ngdesktop_width");
		widthText.setText(value != null ? value : "");
		final Label widthLabel = new Label(sizeGroup, SWT.NONE);
		widthLabel.setText("Width  ");

		heightText = new Text(sizeGroup, SWT.BORDER);
		value = exportElectronWizard.getDialogSettings().get("ngdesktop_height");
		heightText.setText(value != null ? value : "");
		final Label heightLabel = new Label(sizeGroup, SWT.NONE);
		heightLabel.setText("Height");


		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		sizeGroup.setLayoutData(gd);

		final Label srcVersionLabel = new Label(composite, SWT.NONE);
		srcVersionLabel.setText("Version:");
		srcVersionLabel.setToolTipText("NG Desktop version");

		versionGroup = new Composite(composite, SWT.NONE);
		versionGroup.setLayout(new GridLayout(3, false));


		srcVersionCombo = new Combo(versionGroup, SWT.READ_ONLY);
		srcVersionCombo.setToolTipText("NG Desktop version");
		remoteVersions = NGDesktopConfiguration.getAvailableVersions();
		srcVersionCombo.add("latest");

		deprecatedLabel = new Label(versionGroup, SWT.NONE);
		deprecatedLabel.setText("Deprecated: will be removed in the next release");
		deprecatedLabel.setVisible(false);

		remoteVersions.forEach((s) -> {
			srcVersionCombo.add(s);
		});

		srcVersionCombo.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent event)
			{
				srcVersionListener(event);
			}
		});

		srcVersionCombo.select(getVersionIndex() + 1);


		includeUpdateBtn = new Button(versionGroup, SWT.CHECK);
		GridDataFactory.swtDefaults()//
			.grab(true, false)//
			.hint(0, SWT.DEFAULT)// width hint prevents text from expanding to full line
			.align(SWT.FILL, SWT.CENTER)//
			.applyTo(includeUpdateBtn);
		setIncludeUpdateState();
		includeUpdateBtn.setSelection(exportElectronWizard.getDialogSettings().getBoolean("include_update")); //initial selection

		gd = new GridData();
		gd.widthHint = 180;
		includeUpdateBtn.setLayoutData(gd);

		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		gd.verticalSpan = 1;
		versionGroup.setLayoutData(gd);

		final Label appNameLabel = new Label(composite, SWT.NONE);
		appNameLabel.setText("Application name:");
		appNameLabel.setToolTipText("Name of the application");

		appNameText = new Text(composite, SWT.BORDER);
		appNameText.setToolTipText("The maximum allowed length is " + ExportNGDesktopWizard.APP_NAME_LENGTH + " chars");
		appNameText.setEditable(true);
		appNameText.setVisible(true);
		appNameText.setEnabled(true);
		appNameText.setText(getAppName());
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		appNameText.setLayoutData(gd);

		final Label updateUrlLabel = new Label(composite, SWT.NONE);
		updateUrlLabel.setText("Update location:");
		updateUrlLabel.setToolTipText("URL location of the update files.\nOpen context help for details.");

		updateUrlText = new Text(composite, SWT.BORDER);
		updateUrlText.setToolTipText("The maximum allowed length is " + ExportNGDesktopWizard.COPYRIGHT_LENGTH + " chars");
		updateUrlText.setEditable(true);
		updateUrlText.setVisible(true);
		updateUrlText.setEnabled(isUpdatableVersion());
		updateUrlText.setText(getUpdateUrl());
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		updateUrlText.setLayoutData(gd);

		final Label emailLabel = new Label(composite, SWT.NONE);
		emailLabel.setText("Email address:");
		emailLabel.setToolTipText("Email address where to receive download links ...");


		emailAddress = new Text(composite, SWT.BORDER);
		emailAddress.setToolTipText("Email address where to receive download links...");
		emailAddress.setEditable(true);
		emailAddress.setVisible(true);
		value = exportElectronWizard.getDialogSettings().get("email_address");
		emailAddress.setText(value != null ? value : getLoginUsername() != null ? getLoginUsername() : "");
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		emailAddress.setLayoutData(gd);

		final Label storeLabel = new Label(composite, SWT.NONE);
		storeLabel.setText(String.format("Store artifacts for %d days", ExportNGDesktopWizard.STORE_TIMEOUT));

		storeBtn = new Button(composite, SWT.CHECK);
		storeBtn.setText("");
		storeBtn.setToolTipText("If not checked, artifacts will be deleted after several hours ...");
		storeBtn.setSelection(exportElectronWizard.getDialogSettings().getBoolean("store_data"));
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		storeBtn.setLayoutData(gd);

		// add 2 more labels so that the default size is correct
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		Label lbl = new Label(composite, SWT.NONE);
		lbl.setLayoutData(gd);
		lbl = new Label(composite, SWT.NONE);
		lbl.setLayoutData(gd);

		setControl(composite);
	}

	private String getLoginUsername()
	{
		final ISecurePreferences preferences = SecurePreferencesFactory.getDefault();

		final ISecurePreferences node = preferences.node(ServoyLoginDialog.SERVOY_LOGIN_STORE_KEY);
		try
		{
			return node.get(ServoyLoginDialog.SERVOY_LOGIN_USERNAME, null);
		}
		catch (final StorageException ex)
		{
			ServoyLog.logError(ex);
		}
		return null;

	}

	private void srcVersionListener(SelectionEvent event)
	{
		setIncludeUpdateState();
		updateUrlText.setEnabled(isUpdatableVersion());
		deprecatedLabel.setVisible(isDeprecatedVersion());
	}

	private boolean isDeprecatedVersion()
	{
		return srcVersionCombo.getText().startsWith("*");
	}

	private String getInitialImportPath()
	{
		String as_dir = ApplicationServerRegistry.get().getServoyApplicationServerDirectory().replace("\\", "/").replace("//", "/");
		if (!as_dir.endsWith("/")) as_dir += "/";
		return Paths.get(as_dir).getParent().toString();
	}

	private String getApplicationUrl()
	{
		String applicationUrl = exportElectronWizard.getDialogSettings().get("app_url");
		if (applicationUrl == null)
		{
			final String solutionName = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getSolution().getName();
			applicationUrl = "http://localhost:" + ApplicationServerRegistry.get().getWebServerPort() + "/solution/" + solutionName + "/index.html";
		}
		return applicationUrl;
	}

	private int getVersionIndex()
	{
		//we need an index from a sorted list
		final String version = exportElectronWizard.getDialogSettings().get("ngdesktop_version");
		final int result = remoteVersions.indexOf(version);
		return result < 0 ? -1 : result;
	}

	private String getUpdateUrl()
	{
		String urlStr = exportElectronWizard.getDialogSettings().get("update_url");
		if (urlStr == null || urlStr.trim().length() == 0)
		{
			urlStr = applicationUrlText.getText().trim();
			URL myUrl = null;
			try
			{
				myUrl = new URL(urlStr);
			}
			catch (final MalformedURLException e)
			{
				urlStr = getApplicationUrl();
				try
				{
					myUrl = new URL(urlStr);
				}
				catch (final MalformedURLException e1)
				{
					//nothing to do
				}
			}
			if (myUrl != null) urlStr = myUrl.getProtocol() + "://" + myUrl.getAuthority() + "/updates";
		}
		return urlStr;
	}

	private String getAppName()
	{
		String appName = exportElectronWizard.getDialogSettings().get("application_name");
		if (appName == null || appName.trim().length() == 0) appName = "ngdesktop";
		return appName;
	}

	private String getDlgInitPath(String value)
	{
		if (value != null && value.trim().length() > 0 && new File(value).exists()) return value;
		final String newValue = System.getProperty("user.home");
		if (newValue != null) return newValue;
		//still here? then return something ... :D
		return getInitialImportPath();
	}

	private void platformSelectionChangeListener(String selectedPlatform)
	{
		final int index = selectedPlatforms.indexOf(selectedPlatform);

		if (index >= 0) selectedPlatforms.remove(index);
		else selectedPlatforms.add(selectedPlatform);

		setIncludeUpdateState();
		updateUrlText.setEnabled(isUpdatableVersion());
	}

	public List<String> getSelectedPlatforms()
	{
		return selectedPlatforms;
	}

	private void setIncludeUpdateState()
	{
		if (!selectedPlatforms.contains(WINDOWS_PLATFORM))
		{
			includeUpdateBtn.setVisible(false);
			return;
		}
		if (!isUpdatableVersion())
		{
			includeUpdateBtn.setVisible(false);
			return;
		}
		includeUpdateBtn.setText("Include update");
		if (selectedPlatforms.size() > 1) includeUpdateBtn.setText("Include update (Windows)");
		includeUpdateBtn.setVisible(true);
		if (includeUpdateBtn.getSelection() == true && isUpdatableVersion()) return; //selection is not changing
		includeUpdateBtn.setSelection(false);
	}

	private boolean isUpdatableVersion()
	{
		if (selectedPlatforms.size() == 0) return false;
		final int result = SemVerComparator.compare(srcVersionCombo.getText(), FIRST_VERSION_THAT_SUPPORTS_UPDATES);
		if (result >= 0)
			return true;
		return false;
	}

	public void saveState()
	{
		String ngdesktop_version = srcVersionCombo.getText();
		if (ngdesktop_version.startsWith("*")) ngdesktop_version = ngdesktop_version.substring(1).trim();

		final IDialogSettings settings = exportElectronWizard.getDialogSettings();
		settings.put("win_export", selectedPlatforms.indexOf(WINDOWS_PLATFORM) != -1);
		settings.put("osx_export", selectedPlatforms.indexOf(MACOS_PLATFORM) != -1);
		settings.put("linux_export", selectedPlatforms.indexOf(LINUX_PLATFORM) != -1);
		settings.put("app_url", applicationUrlText.getText().trim());
		settings.put("ngdesktop_version", ngdesktop_version);
		settings.put("include_update", includeUpdateBtn.isVisible() && includeUpdateBtn.getSelection());
		settings.put("icon_path", iconPath.getText().trim());
		settings.put("image_path", imgPath.getText().trim());
		settings.put("copyright", copyrightText.getText());
		settings.put("ngdesktop_height", heightText.getText().trim());
		settings.put("ngdesktop_width", widthText.getText().trim());
		settings.put("application_name", appNameText.getText().trim());
		settings.put("update_url", updateUrlText.getText().trim());
		settings.put("email_address", emailAddress.getText().trim());
		settings.put("store_data", storeBtn.getSelection());
	}

	@Override
	public void performHelp()
	{
		PlatformUI.getWorkbench().getHelpSystem().displayHelp("com.servoy.eclipse.ui.export_ngdesktop_solution");
	}
}