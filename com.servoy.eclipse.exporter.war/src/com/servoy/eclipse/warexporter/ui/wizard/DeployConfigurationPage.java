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

package com.servoy.eclipse.warexporter.ui.wizard;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.warexporter.export.ExportWarModel;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.SortedProperties;

/**
 * @author gboros
 *
 */
public class DeployConfigurationPage extends WizardPage implements Listener, SelectionListener, IRestoreDefaultPage
{
	private final ExportWarModel exportModel;
	private Button overwriteDBServerPropertiesBtn;
	private Button overwriteAllPropertiesBtn;
	private Text userHomeText;
	private Button automaticallyUpgradeRepository;
	private Button createTomcatContextXML;
	private Button antiResourceLocking;
	private Button clearReferencesStatic;
	private Button clearReferencesStopThreads;
	private Button clearReferencesStopTimerThreads;
	private Text fileContextNameText;
	private Button browseContextButton;
	private Button exportTitaniumSourceMaps;


	public DeployConfigurationPage(String title, ExportWarModel exportModel)
	{
		super(title);
		this.exportModel = exportModel;
		setTitle("Deploy configuration");
		setDescription("Specify settings for the deployed war");
	}

	public void createControl(Composite parent)
	{
		GridLayout gridLayout = new GridLayout(3, false);
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(gridLayout);

		// context.xml
		Label contextText = new Label(composite, NONE);
		contextText.setText(
			"Add the given Tomcat context.xml file in the war file (META-INF/context.xml)");
		contextText
			.setToolTipText("This context.file can be used to configure this war for tomcat (resource locking, cookie setings), see tomcat documentation");
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		contextText.setLayoutData(gd);


		fileContextNameText = new Text(composite, SWT.BORDER);
		fileContextNameText.addListener(SWT.KeyUp, this);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		gd.grabExcessHorizontalSpace = true;
		gd.minimumWidth = 100;
		fileContextNameText.setLayoutData(gd);
		if (exportModel.getTomcatContextXMLFileName() != null) fileContextNameText.setText(exportModel.getTomcatContextXMLFileName());

		browseContextButton = new Button(composite, SWT.PUSH);
		browseContextButton.setText("Browse...");
		browseContextButton.addListener(SWT.Selection, this);

		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 4;
		new Label(composite, SWT.NONE).setLayoutData(gd); // just an empty vertical space

//		createTomcatContextXML = new Button(composite, SWT.CHECK);
//		createTomcatContextXML.setText("Create Tomcat META-INF/context.xml");
//		createTomcatContextXML.setSelection(exportModel.isCreateTomcatContextXML());
//		createTomcatContextXML.setToolTipText("Adds a Tomcat specific META-INF/context.xml file in the war file which allows enabling the options below.\n" +
//			"Please note that the file is copied (and renamed) to $CATALINA_BASE/conf/[enginename]/[hostname]/ only the first time the war is deployed.\n" +
//			"Subsequent updates of META-INF/context.xml in the war file will be ignored by tomcat.");
//		createTomcatContextXML.addSelectionListener(new SelectionAdapter()
//		{
//			@Override
//			public void widgetSelected(SelectionEvent e)
//			{
//				clearReferencesStatic.setEnabled(createTomcatContextXML.getSelection());
//				clearReferencesStopThreads.setEnabled(createTomcatContextXML.getSelection());
//				clearReferencesStopTimerThreads.setEnabled(createTomcatContextXML.getSelection());
//				antiResourceLocking.setEnabled(createTomcatContextXML.getSelection());
//				exportModel.setCreateTomcatContextXML(createTomcatContextXML.getSelection());
//			}
//		});
//		createTomcatContextXML.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 4, 1));
//
//		new Label(composite, SWT.NONE);
//		new Label(composite, SWT.NONE);
//		antiResourceLocking = new Button(composite, SWT.CHECK);
//		antiResourceLocking.setText("Set antiResourceLocking to true");
//		antiResourceLocking.setSelection(exportModel.isAntiResourceLocking());
//		antiResourceLocking.setToolTipText(
//			"Recomended for Tomcat instalations running on Windows. It avoids a file locking issue when undeploying the war. \n" +
//				"This will impact the startup time of the application.");
//		antiResourceLocking.setEnabled(createTomcatContextXML.getSelection());
//		antiResourceLocking.addSelectionListener(new SelectionAdapter()
//		{
//			@Override
//			public void widgetSelected(SelectionEvent e)
//			{
//				exportModel.setAntiResourceLocking(antiResourceLocking.getSelection());
//			}
//		});
//		antiResourceLocking.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
//
//		new Label(composite, SWT.NONE);
//		new Label(composite, SWT.NONE);
//		clearReferencesStatic = new Button(composite, SWT.CHECK);
//		clearReferencesStatic.setText("Set clearReferencesStatic to true");
//		clearReferencesStatic.setSelection(exportModel.isClearReferencesStatic());
//		clearReferencesStatic.setEnabled(createTomcatContextXML.getSelection());
//		clearReferencesStatic.setToolTipText(
//			"In order to avoid memory leaks, Tomcat will null out static fields from loaded classes after the application has been stopped.");
//		clearReferencesStatic.addSelectionListener(new SelectionAdapter()
//		{
//			@Override
//			public void widgetSelected(SelectionEvent e)
//			{
//				exportModel.setClearReferencesStatic(clearReferencesStatic.getSelection());
//			}
//		});
//		clearReferencesStatic.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
//
//
//		new Label(composite, SWT.NONE);
//		new Label(composite, SWT.NONE);
//		clearReferencesStopThreads = new Button(composite, SWT.CHECK);
//		clearReferencesStopThreads.setText("Set clearReferencesStopThreads to true (USE WITH CARE)");
//		clearReferencesStopThreads.setSelection(exportModel.isClearReferencesStopThreads());
//		clearReferencesStopThreads.setEnabled(createTomcatContextXML.getSelection());
//		clearReferencesStopThreads.setToolTipText(
//			"In order to avoid memory leaks, Tomcat will attempt to terminate threads that have been started by the web application.\n" +
//				"Still running threads are stopped via the deprecated Thread.stop() method.");
//		clearReferencesStopThreads.addSelectionListener(new SelectionAdapter()
//		{
//			@Override
//			public void widgetSelected(SelectionEvent e)
//			{
//				exportModel.setClearReferencesStopThreads(clearReferencesStopThreads.getSelection());
//			}
//		});
//		clearReferencesStopThreads.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
//
//		new Label(composite, SWT.NONE);
//		new Label(composite, SWT.NONE);
//		clearReferencesStopTimerThreads = new Button(composite, SWT.CHECK);
//		clearReferencesStopTimerThreads.setText("Set clearReferencesStopTimerThreads to true");
//		clearReferencesStopTimerThreads.setSelection(exportModel.isClearReferencesStopTimerThreads());
//		clearReferencesStopTimerThreads.setToolTipText(
//			"In order to avoid memory leaks, Tomcat attempts to terminate java.util.Timer threads that have been started by the web application.\n" +
//				"Unlike standard threads, timer threads can be stopped safely although there may still be side-effects for the application.");
//
//		clearReferencesStopTimerThreads.setEnabled(createTomcatContextXML.getSelection());
//
//		clearReferencesStopTimerThreads.addSelectionListener(new SelectionAdapter()
//		{
//			@Override
//			public void widgetSelected(SelectionEvent e)
//			{
//				exportModel.setClearReferencesStopTimerThreads(clearReferencesStopTimerThreads.getSelection());
//			}
//		});
//		clearReferencesStopTimerThreads.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));

		automaticallyUpgradeRepository = new Button(composite, SWT.CHECK);
		automaticallyUpgradeRepository.setSelection(exportModel.isAutomaticallyUpgradeRepository());
		automaticallyUpgradeRepository.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				exportModel.setAutomaticallyUpgradeRepository(automaticallyUpgradeRepository.getSelection());
			}
		});
		automaticallyUpgradeRepository.setText("Automatically upgrade repository if needed.");
		automaticallyUpgradeRepository.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1));

		overwriteDBServerPropertiesBtn = new Button(composite, SWT.CHECK);
		overwriteDBServerPropertiesBtn.setSelection(exportModel.isOverwriteDeployedDBServerProperties());
		overwriteDBServerPropertiesBtn.addSelectionListener(this);
		overwriteDBServerPropertiesBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1));
		overwriteDBServerPropertiesBtn.setText("Overwrite changed DB servers properties from previous deployment");
		overwriteDBServerPropertiesBtn.setToolTipText(
			"Overwrite all DB-server-configuration-related changes that were made after previous deployment of the war via admin page with the ones from war export.");

		overwriteAllPropertiesBtn = new Button(composite, SWT.CHECK);
		overwriteAllPropertiesBtn.setSelection(exportModel.isOverwriteDeployedServoyProperties());
		overwriteAllPropertiesBtn.addSelectionListener(this);
		overwriteAllPropertiesBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1));
		overwriteAllPropertiesBtn.setText("Overwrite all Servoy properties");
		overwriteAllPropertiesBtn.setToolTipText("Overwrite all servoy.properties changes that were made on the previous deploy of the war");


		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		new Label(composite, SWT.NONE).setLayoutData(gd); // just an empty vertical space

		Label label = new Label(composite, SWT.NONE);
		label.setText("User home directory: ");
		userHomeText = new Text(composite, SWT.BORDER);
		userHomeText.setText(exportModel.getUserHome() != null ? exportModel.getUserHome() : "");
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		gd.grabExcessHorizontalSpace = true;
		gd.minimumWidth = 100;
		userHomeText.setLayoutData(gd);
		userHomeText.addListener(SWT.KeyUp, this);

		label = new Label(composite, SWT.NONE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);
		label.setText(
			"NOTE: This must be a writable directory where Servoy application related files will be stored.\nIf you leave it empty, the system user home directory will be used.");

		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		new Label(composite, SWT.NONE).setLayoutData(gd); // just an empty vertical space

		exportTitaniumSourceMaps = new Button(composite, SWT.CHECK);
		exportTitaniumSourceMaps.setText("Include Titanium client source-maps - for debugging");
		exportTitaniumSourceMaps.setToolTipText(
			"It can be useful if you want the titanium client side (browser) code to be debugged easier (see the original sources when debugging).");
		exportTitaniumSourceMaps.setSelection(exportModel.exportNG2Mode() == "sourcemaps");
		exportTitaniumSourceMaps.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1));
		exportTitaniumSourceMaps.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				exportModel.setExportNG2Mode(exportTitaniumSourceMaps.getSelection() ? "sourcemaps" : "true"); // as Titanium is the only client to use currently, we never give "false" from the UI exporter
			}
		});

		setControl(composite);
	}

	@Override
	public void setVisible(boolean visible)
	{
		if (visible && exportModel.getServoyPropertiesFileName() != null)
		{
			userHomeText.setText("");
			exportModel.setUserHome(null);
			try (FileInputStream fis = new FileInputStream(new File(exportModel.getServoyPropertiesFileName())))
			{
				Properties properties = new SortedProperties();
				properties.load(fis);
				String propertyUserHome = properties.getProperty(Settings.USER_HOME);
				if (propertyUserHome != null)
				{
					userHomeText.setText(propertyUserHome);
				}
			}
			catch (IOException e)
			{
				ServoyLog.logError(e);
			}
		}
		super.setVisible(visible);
	}

	@Override
	public void handleEvent(Event event)
	{
		if (event.widget == userHomeText)
			exportModel.setUserHome(userHomeText.getText());
		else if (event.widget == fileContextNameText)
		{
			String potentialFileName = fileContextNameText.getText();
			exportModel.setTomcatContextXMLFileName(potentialFileName);
		}
		else if (event.widget == browseContextButton)
		{
			Shell shell = new Shell();
			GridLayout gridLayout = new GridLayout();
			shell.setLayout(gridLayout);
			FileDialog dlg = new FileDialog(shell, SWT.OPEN);
			String fileName = exportModel.getTomcatContextXMLFileName();
			if (fileName == null) fileName = "context.xml";
			File f = new File(fileName);
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
			String[] extensions = { "*.xml" };
			dlg.setFilterExtensions(extensions);
			String chosenFileName = dlg.open();
			if (chosenFileName != null)
			{
				exportModel.setTomcatContextXMLFileName(chosenFileName);
				fileContextNameText.setText(chosenFileName);
			}
		}
	}

	@Override
	public void widgetSelected(SelectionEvent e)
	{
		if (e.widget == overwriteDBServerPropertiesBtn)
		{
			exportModel.setOverwriteDeployedDBServerProperties(overwriteDBServerPropertiesBtn.getSelection());
		}
		else if (e.widget == overwriteAllPropertiesBtn)
		{
			exportModel.setOverwriteDeployedServoyProperties(overwriteAllPropertiesBtn.getSelection());
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e)
	{
		// TODO Auto-generated method stub
	}


	@Override
	public void restoreDefaults()
	{
		overwriteDBServerPropertiesBtn.setSelection(true);
		overwriteAllPropertiesBtn.setSelection(false);
		userHomeText.setText("");

		exportModel.setOverwriteDeployedDBServerProperties(true);
		exportModel.setOverwriteDeployedServoyProperties(false);
		exportModel.setUserHome(null);

		automaticallyUpgradeRepository.setSelection(false);
		exportModel.setAutomaticallyUpgradeRepository(false);

		fileContextNameText.setText("");
		exportModel.setTomcatContextXMLFileName("");
		createTomcatContextXML.setSelection(false);
		exportModel.setCreateTomcatContextXML(false);
		antiResourceLocking.setSelection(false);
		antiResourceLocking.setEnabled(false);
		exportModel.setAntiResourceLocking(false);
		clearReferencesStatic.setSelection(false);
		clearReferencesStatic.setEnabled(false);
		exportModel.setClearReferencesStatic(false);
		clearReferencesStopThreads.setEnabled(false);
		clearReferencesStopThreads.setSelection(false);
		exportModel.setClearReferencesStopThreads(false);
		clearReferencesStopTimerThreads.setSelection(false);
		clearReferencesStopTimerThreads.setEnabled(false);
		exportModel.setClearReferencesStopTimerThreads(false);

		exportTitaniumSourceMaps.setSelection(false);
	}

	@Override
	public void performHelp()
	{
		PlatformUI.getWorkbench().getHelpSystem().displayHelp("com.servoy.eclipse.exporter.war.export_war_deploy_configuration");
	}
}