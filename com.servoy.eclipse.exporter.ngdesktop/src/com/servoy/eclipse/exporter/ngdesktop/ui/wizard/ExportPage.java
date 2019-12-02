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

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.core.ServoyModelManager;
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
	
	public Label statusLabel; //this is just temporary while tweaking the service

	private List<String> selectedPlatforms = new ArrayList<String>();
	private ExportNGDesktopWizard exportElectronWizard;

	public ExportPage(ExportNGDesktopWizard exportElectronWizard)
	{
		super("page1");
		this.exportElectronWizard = exportElectronWizard;
		setTitle("Export Servoy application in electron");
	}

	public void createControl(Composite parent)
	{
		Composite rootComposite = new Composite(parent, SWT.NONE);
		rootComposite.setLayout(new FormLayout());
		
		GridLayout gridLayout = new GridLayout(3, false);
		Composite composite = new Composite(rootComposite, SWT.NONE);
		composite.setLayout(gridLayout);

		Label applicationURLLabel = new Label(composite, SWT.NONE);
		applicationURLLabel.setText("Servoy application URL");
		applicationURLText = new Text(composite, SWT.BORDER);
		applicationURLText.setText(getInitialApplicationURL());

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		applicationURLText.setLayoutData(gd);

		Label platformLabel = new Label(composite, SWT.NONE);
		platformLabel.setText("Platform");

		platformGroup = new Group(composite, SWT.NONE);
		platformGroup.setLayout(new RowLayout(SWT.HORIZONTAL));

		Button winBtn = new Button(platformGroup, SWT.CHECK);
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

		Button macBtn = new Button(platformGroup, SWT.CHECK);
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

		Button linuxBtn = new Button(platformGroup, SWT.CHECK);
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

		Label outputDirLabel = new Label(composite, SWT.NONE);
		outputDirLabel.setText("Save directory");

		saveDirPath = new Text(composite, SWT.BORDER);
		String saveDir = exportElectronWizard.getDialogSettings().get("save_dir");
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
				DirectoryDialog dlg = new DirectoryDialog(getShell(), SWT.NONE);
				dlg.setFilterPath(getDlgInitPath(saveDirPath.getText()));
				String path = dlg.open();
				if (path != null)
				{
					saveDirPath.setText(path);
				}
			}
		});

			Label iconLabel = new Label(composite, SWT.NONE);
			iconLabel.setText("Icon path:");
			iconLabel.setToolTipText("Logo image (png) used by the NG Desktop Client and the installer.\nMaximum size (KB): " + ExportNGDesktopWizard.LOGO_SIZE);
			
			iconPath = new Text(composite, SWT.BORDER);
			iconPath.setToolTipText("Logo image (png) used by the NG Desktop Client and the installer.\nMaximum file size (KB): " + ExportNGDesktopWizard.LOGO_SIZE);
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
					FileDialog dlg = new FileDialog(getShell(), SWT.NONE);
					dlg.setFilterExtensions(new String[] { "*.png" });
					File myFile = new File(getDlgInitPath(iconPath.getText())); //make sure we have a usable path
					if (myFile.isDirectory()) {
						dlg.setFilterPath(myFile.getAbsolutePath());
					} else {
						dlg.setFilterPath(myFile.getParent());
					}
					String path = dlg.open();
					if (path != null)
					{
						iconPath.setText(path + File.separator + dlg.getFileName());
					}
				}
			});
			
			Label imgLabel = new Label(composite, SWT.NONE);
			imgLabel.setText("Image path:");
			imgLabel.setToolTipText("Bitmap image used in the installer. For Windows installer the recommended size is 164 px width, 314 px height.\nMaximum file size (KB): " + ExportNGDesktopWizard.IMG_SIZE);
			
			imgPath = new Text(composite, SWT.BORDER);
			imgPath.setToolTipText("Bitmap image used in the installer. For Windows installer the recommended size is 164 px width, 314 px height.\nMaximum file size (KB): " + ExportNGDesktopWizard.IMG_SIZE);
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
					FileDialog dlg = new FileDialog(getShell(), SWT.NONE);
					dlg.setFilterExtensions(new String[] { "*.bmp" });
					File myFile = new File(getDlgInitPath(imgPath.getText())); //make sure we have a usable path
					if (myFile.isDirectory()) {
						dlg.setFilterPath(myFile.getAbsolutePath());
					} else {
						dlg.setFilterPath(myFile.getParent());
					}
					String path = dlg.open();
					if (path != null)
					{
						imgPath.setText(path + File.separator + dlg.getFileName());
					}
				}
			});
			
			Label copyrightLabel = new Label(composite, SWT.NONE);
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
						
			Label sizeLabel = new Label(composite, SWT.NONE);
			sizeLabel.setText("NG Desktop size:");
			sizeGroup = new Group(composite, SWT.NONE);
			RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
			rowLayout.fill = true;
			sizeGroup.setLayout(rowLayout);
			
			widthText = new Text(sizeGroup, SWT.BORDER);
			value = exportElectronWizard.getDialogSettings().get("ngdesktop_width");
			widthText.setText(value != null ? value : "");
			Label widthLabel = new Label(sizeGroup, SWT.NONE);
			widthLabel.setText("Width  ");
			
			heightText = new Text(sizeGroup, SWT.BORDER);
			value = exportElectronWizard.getDialogSettings().get("ngdesktop_height");
			heightText.setText(value != null ? value : "");
			Label heightLabel = new Label(sizeGroup, SWT.NONE);
			heightLabel.setText("Height");
			
			
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 2;
			sizeGroup.setLayoutData(gd);		
						
			statusLabel = new Label(composite, SWT.NONE);
			statusLabel.setEnabled(false); //set to gray
			statusLabel.setText("Status (beta): ");
			gd = new GridData( SWT.FILL, SWT.FILL, true, true );
			gd.verticalAlignment = GridData.VERTICAL_ALIGN_END;
			gd.horizontalSpan = 3;
			statusLabel.setLayoutData(gd);
			statusLabel.setVisible(true);
			
			Label noteLabel =  new Label(rootComposite, SWT.BOTTOM);
			noteLabel.setText("*For now we only support generating Windows branded installers");
			FormData fd = new FormData();
			fd.bottom = new FormAttachment(100);
			fd.left = new FormAttachment(0);
			noteLabel.setLayoutData(fd);
			noteLabel.setEnabled(false);
		
		setControl(composite);
	}
	
	public String getIconPath() {
		return iconPath.getText();
	}
	
	public String getImgPath() {
		return imgPath.getText();
	}
	
	public String getCopyright() {
		return copyrightText.getText();
	}
	
	private static String getInitialImportPath()
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
			String solutionName = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getSolution().getName();
			applicationURL = "http://localhost:" + ApplicationServerRegistry.get().getWebServerPort() + "/solutions/" + solutionName + "/index.html";
		}
		return applicationURL;
	}

	private String getDlgInitPath(String value)
	{
		if (value != null && value.trim().length() > 0 && (new File(value)).exists()) {
			return value;
		}
		String newValue = System.getProperty("user.home");
		if (newValue != null) return newValue;
		//still here? then return something ... :D
		return getInitialImportPath();
	}

	private Object platformSelectionChangeListener(String selectedPlatform)
	{
		int index = selectedPlatforms.indexOf(selectedPlatform);
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
		IDialogSettings settings = exportElectronWizard.getDialogSettings();
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
	}
}