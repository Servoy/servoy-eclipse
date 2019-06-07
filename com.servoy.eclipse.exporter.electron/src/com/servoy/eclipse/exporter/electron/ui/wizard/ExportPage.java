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

package com.servoy.eclipse.exporter.electron.ui.wizard;

import java.util.Arrays;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
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
	private Text applicationURLText;
	private Text saveDir;
	private Group platformGroup;
	private Group packageGroup;
	private Button permanent;
	private String selectedPlatform = "win";
	private String selectedArchive = "tarball";

	public ExportPage(ExportElectronWizard exportElectronWizard)
	{
		super("page1");
		setTitle("Export Servoy application in electron");
	}

	public void createControl(Composite parent)
	{
		GridLayout gridLayout = new GridLayout(3, false);
		Composite composite = new Composite(parent, SWT.NONE);
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

		
		Button winBtn = new Button(platformGroup, SWT.RADIO);
		winBtn.setData("win");
		winBtn.setText("Windows");
		winBtn.setSelection(true);
		
		winBtn.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent event)
			{
				platformSelectionChangeListener((String)event.widget.getData());
			}
		});
		 
		
		Button macBtn = new Button(platformGroup, SWT.RADIO);
		macBtn.setText("MacOS");
		macBtn.setData("mac");
		
		macBtn.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent event)
			{
				platformSelectionChangeListener((String)event.widget.getData());
			}
		});

		Button linuxBtn = new Button(platformGroup, SWT.RADIO);
		linuxBtn.setText("Linux");
		linuxBtn.setData("linux");
		
		linuxBtn.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent event)
			{
				platformSelectionChangeListener((String)event.widget.getData());
			}
		});
		
		Button linuxMacBtn = new Button(platformGroup, SWT.RADIO);
		linuxBtn.setText("Linux & Mac");
		linuxBtn.setData("l_w");
		
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
		
		
		Label packageLabel = new Label(composite, SWT.NONE);
		packageLabel.setText("Target application package");
		
		packageGroup = new Group(composite, SWT.NONE);
		packageGroup.setLayout(new RowLayout(SWT.HORIZONTAL));
		
		Button tarballBtn = new Button(packageGroup, SWT.RADIO);
		tarballBtn.setText("Tarball");
		tarballBtn.setData("tarball");
		tarballBtn.setSelection(true);
		
		tarballBtn.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent event)
			{
				packageSelectionChangeListener((String)event.widget.getData());
			}
		});
		 
		Button installerBtn = new Button(packageGroup, SWT.RADIO);
		installerBtn.setText("Installer");
		installerBtn.setData("installer");
		
		installerBtn.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent event)
			{
				packageSelectionChangeListener((String)event.widget.getData());
			}
		});
		
		Button zipBtn = new Button(packageGroup, SWT.RADIO);
		zipBtn.setText("Zip");
		zipBtn.setData("zip");
		
		zipBtn.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent event)
			{
				packageSelectionChangeListener((String)event.widget.getData());
			}
		});
		
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		packageGroup.setLayoutData(gd);
		
		
		permanent = new Button(composite, SWT.CHECK);
		permanent.setText("Keep download permanent");
		permanent.setLayoutData(gd);
		
		
		Label outputDirLabel = new Label(composite, SWT.NONE);
		outputDirLabel.setText("Save directory");	
		
		saveDir = new Text(composite, SWT.BORDER);
		saveDir.setText(getInitialSaveDir());
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		saveDir.setLayoutData(gd);
		
		setControl(composite);
	}
	
	private String getInitialApplicationURL()
	{
		String solutionName = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getSolution().getName();
		String applicationURL = "http://localhost:" + ApplicationServerRegistry.get().getWebServerPort() + "/solutions/" + solutionName +
				"/index.html";
		return applicationURL;
	}
	
	public String getApplicationURL()
	{
		return applicationURLText.getText();
	}
	
	private String getInitialSaveDir()
	{
		return System.getProperty("user.home");
	}
	
	public String getSaveDir()
	{
		return saveDir.getText();
	}
	
	public void platformSelectionChangeListener(String selectedPlatform) {
		this.selectedPlatform = selectedPlatform;
	}
	
	public void packageSelectionChangeListener(String selectedArchive) {
		this.selectedArchive = selectedArchive;
	}
	
	public String getSelectedPlatform() {
		return selectedPlatform;
	}
	
	public String getSelectedPackageType() {
		return selectedArchive;
	}
	
	public boolean getIsPermanent() {
		return permanent.getSelection();
	}
}