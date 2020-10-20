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

package com.servoy.eclipse.exporter.ngdesktop.ui.wizard;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.json.JSONArray;
import org.json.JSONObject;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.ClientVersion;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * @author gboros
 */
public class ExportPage extends WizardPage
{

	public static String WINDOWS_PLATFORM = "win";
	public static String MACOS_PLATFORM = "mac";
	public static String LINUX_PLATFORM = "linux";

	private Text applicationURLText;
	private Text saveDirPath;
	private Button browseDirButton;
	private Group platformGroup;


	private Text iconPath;
	private Button browseIconButton;
	private Text imgPath;
	private Button browseImgButton;
	private Text copyrightText;
	private Group sizeGroup;
	private Text widthText;
	private Text heightText;
	private Group versionGroup;
	private Combo srcVersionCombo;
	private Button includeUpdateBtn;

	private final List<String> selectedPlatforms = new ArrayList<String>();
	private final ExportNGDesktopWizard exportElectronWizard;
	private final String versionsUrl = "https://download.servoy.com/ngdesktop/src/versions.txt";
	private final String[] versions = { "2020.06", "2020.12" };
	private final boolean[] updates = { false, true };
	private Map<String, Boolean> srcVersions = new TreeMap<String, Boolean>(); //sort by keys

	public ExportPage(ExportNGDesktopWizard exportElectronWizard)
	{
		super("page1");
		this.exportElectronWizard = exportElectronWizard;
		setTitle("Export Servoy application");
		for (int index = 0; index < versions.length; index++)
			srcVersions.put(versions[index], Boolean.valueOf(updates[index]));
	}

	@Override
	public void createControl(Composite parent)
	{
		final Composite rootComposite = new Composite(parent, SWT.NONE);
		rootComposite.setLayout(new FormLayout());

		final GridLayout gridLayout = new GridLayout(3, false);
		final Composite composite = new Composite(rootComposite, SWT.NONE);
		composite.setLayout(gridLayout);

		final Label applicationURLLabel = new Label(composite, SWT.NONE);
		applicationURLLabel.setText("Servoy application URL");
		applicationURLText = new Text(composite, SWT.BORDER);
		applicationURLText.setText(getInitialApplicationURL());

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		applicationURLText.setLayoutData(gd);

		final Label platformLabel = new Label(composite, SWT.NONE);
		platformLabel.setText("Platform");

		platformGroup = new Group(composite, SWT.NONE);
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

		final Label outputDirLabel = new Label(composite, SWT.NONE);
		outputDirLabel.setText("Save directory");

		saveDirPath = new Text(composite, SWT.BORDER);
		final String saveDir = exportElectronWizard.getDialogSettings().get("save_dir");
		saveDirPath.setText(saveDir != null ? saveDir : "");
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		saveDirPath.setLayoutData(gd);


		browseDirButton = new Button(composite, SWT.NONE);
		browseDirButton.setText("Browse");
		browseDirButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				final DirectoryDialog dlg = new DirectoryDialog(getShell(), SWT.NONE);
				dlg.setFilterPath(getDlgInitPath(saveDirPath.getText()));
				final String path = dlg.open();
				if (path != null) saveDirPath.setText(path);
			}
		});

		final Label iconLabel = new Label(composite, SWT.NONE);
		iconLabel.setText("Icon path:");
		iconLabel
			.setToolTipText("Logo image (png) used by the NG Desktop Client and the installer.\nMaximum file size (KB): " + ExportNGDesktopWizard.LOGO_SIZE +
				"\nMinimum logo size: 256 x 256 px");

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
		sizeGroup = new Group(composite, SWT.NONE);
		final RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
		rowLayout.fill = true;
		sizeGroup.setLayout(rowLayout);

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
		srcVersionLabel.setText("Source version:");
		srcVersionLabel.setToolTipText("NG Desktop source version");

		versionGroup = new Group(composite, SWT.NONE);
		versionGroup.setLayout(new RowLayout(SWT.HORIZONTAL));


		srcVersionCombo = new Combo(versionGroup, SWT.READ_ONLY);
		srcVersions = getAvailableVersions();

		final Iterator<String> keys = srcVersions.keySet().iterator();
		while (keys.hasNext())
		{
			final String key = keys.next();
			srcVersionCombo.add(key);
			final Object data = srcVersions.get(key);
			srcVersionCombo.setData(key, data);
		}
		srcVersionCombo.select(0);
		srcVersionCombo.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent event)
			{
				final String key = ((Combo)event.widget).getText();
				final Boolean supportUpdates = (Boolean)event.widget.getData(key);
				includeUpdateBtn.setEnabled(supportUpdates.booleanValue());
				if (!supportUpdates.booleanValue())
				{
					includeUpdateBtn.setData(Boolean.valueOf(false));
					includeUpdateBtn.setSelection(false);
				}
			}
		});

		includeUpdateBtn = new Button(versionGroup, SWT.CHECK);
		includeUpdateBtn.setText("Include update");
		includeUpdateBtn.setEnabled(srcVersions.get(srcVersionCombo.getText()).booleanValue());
		includeUpdateBtn.setData(Boolean.valueOf(false));

		includeUpdateBtn.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent event)
			{
				includeUpdateListener(((Button)event.widget).getSelection());
			}
		});

		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		versionGroup.setLayoutData(gd);

		final Label noteLabel = new Label(composite, SWT.NONE);
		noteLabel.setText("*For now we only support generating Windows branded installers");
		noteLabel.setEnabled(false); //set to gray
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.verticalAlignment = GridData.VERTICAL_ALIGN_END;
		gd.horizontalSpan = 3;
		noteLabel.setLayoutData(gd);
		noteLabel.setVisible(true);

		setControl(composite);
		this.getWizard().getContainer().getShell().pack();
	}

	private void includeUpdateListener(boolean value)
	{
		includeUpdateBtn.setData(Boolean.valueOf(value));
	}

	private Map<String, Boolean> getAvailableVersions()
	{
		final Map<String, Boolean> result = new TreeMap<String, Boolean>(); //sorted map by keys
		try
		{
			final URL url = new URL(versionsUrl);
			final StringBuffer sb = new StringBuffer();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream())))
			{
				String line = null;
				while ((line = br.readLine()) != null)
					sb.append(line);
				final JSONObject jsonObj = new JSONObject(sb.toString());
				final JSONArray value = (JSONArray)jsonObj.get("versions");
				final JSONObject myObj = (JSONObject)value.get(0);
				value.forEach((item) -> {
					final int devVersion = ((JSONObject)item).getInt("developerMinVersion");
					if (ClientVersion.getReleaseNumber() < devVersion)
						return;
					final Boolean supportUpdate = Boolean.valueOf(((JSONObject)item).getBoolean("supportUpdate"));
					final String ngdesktopVersion = ((JSONObject)item).getString("ngDesktopVersion");
					result.put(ngdesktopVersion, supportUpdate);
				});
				if (result.size() > 0)
					return result;
			}
		}
		catch (final IOException | NumberFormatException e)
		{
			ServoyLog.logError(e);
		}
		return srcVersions;
	}

	private String getInitialImportPath()
	{
		String as_dir = ApplicationServerRegistry.get().getServoyApplicationServerDirectory().replace("\\", "/").replace("//", "/");
		if (!as_dir.endsWith("/")) as_dir += "/";
		return Paths.get(as_dir).getParent().toString();
	}

	private String getInitialApplicationURL()
	{
		String applicationURL = exportElectronWizard.getDialogSettings().get("application_url");
		if (applicationURL == null)
		{
			final String solutionName = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getSolution().getName();
			applicationURL = "http://localhost:" + ApplicationServerRegistry.get().getWebServerPort() + "/solutions/" + solutionName + "/index.html";
		}
		return applicationURL;
	}

	private String getDlgInitPath(String value)
	{
		if (value != null && value.trim().length() > 0 && new File(value).exists()) return value;
		final String newValue = System.getProperty("user.home");
		if (newValue != null) return newValue;
		//still here? then return something ... :D
		return getInitialImportPath();
	}

	private Object platformSelectionChangeListener(String selectedPlatform)
	{
		final int index = selectedPlatforms.indexOf(selectedPlatform);
		// the result type is different depending on the execution leaf
		return index >= 0 ? selectedPlatforms.remove(index) : selectedPlatforms.add(selectedPlatform);
	}


	public List<String> getSelectedPlatforms()
	{
		return selectedPlatforms;
	}

	public void saveState()
	{
		//invalid values are not saved; that's easier for later validation;
		final IDialogSettings settings = exportElectronWizard.getDialogSettings();
		settings.put("win_export", selectedPlatforms.indexOf(WINDOWS_PLATFORM) != -1);
		settings.put("osx_export", selectedPlatforms.indexOf(MACOS_PLATFORM) != -1);
		settings.put("linux_export", selectedPlatforms.indexOf(LINUX_PLATFORM) != -1);
		settings.put("save_dir", saveDirPath.getText().trim());
		settings.put("app_url", applicationURLText.getText().trim());
		settings.put("icon_path", iconPath.getText().trim());
		settings.put("image_path", imgPath.getText().trim());
		settings.put("copyright", copyrightText.getText());
		settings.put("ngdesktop_width", widthText.getText().trim());
		settings.put("ngdesktop_height", heightText.getText().trim());
		settings.put("ngdesktop_version", srcVersionCombo.getText());
		settings.put("ngdesktop_include_update", includeUpdateBtn.getData().toString());
	}
}