/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.team.ui;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.team.Activator;


public class RepositoryWizardPage extends WizardPage
{
	private static final String S_SERVER_ADDRESS = "address";
	private static final String S_USER = "user";
	private static final String S_PASSWORD = "password";

	public static final String NAME = "RepositoryWizardPage";
	private Text txServerAddress;
	private Text txUser;
	private Text txPassword;

	public RepositoryWizardPage(String title, String description, ImageDescriptor titleImage)
	{
		super(RepositoryWizardPage.NAME, title, titleImage);
		setDescription(description);
		setTitle(title);
	}

	public void createControl(Composite parent)
	{
		String tServerAddress = null;
		String tUser = null;
		String tPassword = null;

		Preferences pref = Activator.getDefault().getPluginPreferences();

		tServerAddress = pref.getString(S_SERVER_ADDRESS);
		tUser = pref.getString(S_USER);
		tPassword = pref.getString(S_PASSWORD);


		Composite cp = Util.createComposite(parent, 2);

		Util.createLabel(cp, "Server address");
		txServerAddress = Util.createTextField(cp);
		txServerAddress.setText(tServerAddress.length() < 1 ? "localhost" : tServerAddress);

		Util.createLabel(cp, "User");
		txUser = Util.createTextField(cp);
		txUser.setText(tUser);


		Util.createLabel(cp, "Password");
		txPassword = Util.createPasswordField(cp);
		txPassword.setText(tPassword);


		setControl(cp);
	}

	public void setServerAddress(String serverAddress)
	{
		if (txServerAddress != null) txServerAddress.setText(serverAddress);
	}

	public String getServerAddress()
	{
		if (txServerAddress != null) return txServerAddress.getText();
		else return null;
	}

	public String getUser()
	{
		if (txUser != null) return txUser.getText();
		else return null;
	}

	public String getPassword()
	{
		if (txPassword != null) return txPassword.getText();
		else return null;
	}

	public void saveEnteredValues()
	{
		Preferences pref = Activator.getDefault().getPluginPreferences();

		pref.setValue(S_SERVER_ADDRESS, txServerAddress.getText());
		pref.setValue(S_USER, txUser.getText());

		Activator.getDefault().savePluginPreferences();
	}
}
