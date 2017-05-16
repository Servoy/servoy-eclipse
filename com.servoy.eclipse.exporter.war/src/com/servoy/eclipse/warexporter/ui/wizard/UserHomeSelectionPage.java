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

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.warexporter.export.ExportWarModel;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.SortedProperties;

/**
 * @author gboros
 *
 */
public class UserHomeSelectionPage extends WizardPage implements Listener
{
	private final ExportWarModel exportModel;
	private Text userHomeText;

	public UserHomeSelectionPage(String title, ExportWarModel exportModel)
	{
		super(title);
		this.exportModel = exportModel;
		setTitle("User home directory");
		setDescription("Specify the user home directory that will be used by the deployed war");
	}

	public void createControl(Composite parent)
	{
		GridLayout gridLayout = new GridLayout(4, false);
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(gridLayout);

		Label label = new Label(composite, SWT.NONE);
		label.setText("User home directory ");

		userHomeText = new Text(composite, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		userHomeText.setLayoutData(gd);
		userHomeText.addListener(SWT.KeyUp, this);

		label = new Label(composite, SWT.NONE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 4;
		label.setLayoutData(gd);
		label.setText(
			"\nNOTE: This must be a writable directory where Servoy application related files will be stored.\nIf you leave it empty, the system user home directory will be used.");


		setControl(composite);
	}

	@Override
	public IWizardPage getNextPage()
	{
		return null;
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

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
	 */
	@Override
	public void handleEvent(Event event)
	{
		exportModel.setUserHome(userHomeText.getText());
	}
}