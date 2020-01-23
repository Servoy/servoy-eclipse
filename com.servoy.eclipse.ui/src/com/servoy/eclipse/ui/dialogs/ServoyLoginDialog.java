/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

package com.servoy.eclipse.ui.dialogs;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.j2db.util.Utils;

/**
 * @author gboros
 */
public class ServoyLoginDialog extends TitleAreaDialog
{
	private static String SERVOY_LOGIN_STORE_KEY = "SERVOY_LOGIN_INFO";
	private static String SERVOY_LOGIN_USERNAME = "USERNAME";
	private static String SERVOY_LOGIN_PASSWORD = "PASSWORD";
	private static String SERVOY_LOGIN_TOKEN = "TOKEN";

	public ServoyLoginDialog(Shell parentShell)
	{
		super(parentShell);
	}

	public String doLogin()
	{
		String username = null;
		String password = null;
		String loginToken = null;

		ISecurePreferences preferences = SecurePreferencesFactory.getDefault();

		ISecurePreferences node = preferences.node(SERVOY_LOGIN_STORE_KEY);
		try
		{
			username = node.get(SERVOY_LOGIN_USERNAME, null);
			password = node.get(SERVOY_LOGIN_PASSWORD, null);
			loginToken = node.get(SERVOY_LOGIN_TOKEN, null);
		}
		catch (StorageException ex)
		{
			ServoyLog.logError(ex);
		}

		if (loginToken == null)
		{
			if (open() == OK)
			{
				username = dlgUsername;
				password = dlgPassword;

				loginToken = "secret";

				try
				{
					node.put(SERVOY_LOGIN_USERNAME, username, true);
					node.put(SERVOY_LOGIN_PASSWORD, password, true);
					node.put(SERVOY_LOGIN_TOKEN, loginToken, true);
				}
				catch (StorageException ex)
				{
					ServoyLog.logError(ex);
				}
			}
		}

		return loginToken;
	}

	private String dlgUsername;
	private String dlgPassword;

	@Override
	protected Control createContents(Composite parent)
	{
		Control contents = super.createContents(parent);
		setTitle("Servoy login");
		getShell().setText("Servoy");
		setMessage("Welcome, please login to Servoy");
		setTitleImage(Activator.getDefault().loadImageFromBundle("solution_wizard_description.png"));
		return contents;
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		parent.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

		GridLayout gridLayout = new GridLayout(2, false);
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(gridLayout);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
		composite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

		Label lbl = new Label(composite, SWT.NONE);
		lbl.setText("Username");
		lbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		FontDescriptor descriptor = FontDescriptor.createFrom(lbl.getFont());
		descriptor = descriptor.setStyle(SWT.BOLD);
		lbl.setFont(descriptor.createFont(getShell().getDisplay()));
		Text usernameTxt = new Text(composite, SWT.BORDER);
		//usernameTxt.setText(exportSolutionWizard.getDeployUsername());
		GridData gd = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		gd.horizontalIndent = 10;
		usernameTxt.setLayoutData(gd);
		usernameTxt.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				dlgUsername = usernameTxt.getText();
			}
		});

		lbl = new Label(composite, SWT.NONE);
		lbl.setText("Password");
		lbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		lbl.setFont(descriptor.createFont(getShell().getDisplay()));
		// On MacOS, SWT 3.5 does not send events to listeners on password fields.
		// See: http://www.eclipse.org/forums/index.php?t=msg&goto=508058&
		int style = SWT.BORDER;
		if (!Utils.isAppleMacOS()) style |= SWT.PASSWORD;
		Text passwordTxt = new Text(composite, style);
		//passwordTxt.setText(exportSolutionWizard.getDeployPassword());
		gd = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		gd.verticalIndent = 10;
		gd.horizontalIndent = 10;
		passwordTxt.setLayoutData(gd);
		passwordTxt.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				dlgPassword = passwordTxt.getText();
			}
		});
		if (Utils.isAppleMacOS()) passwordTxt.setEchoChar('\u2022');

		return composite;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent)
	{
		parent.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		org.eclipse.swt.widgets.Button okBtn = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		okBtn.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
	}

	@Override
	protected Control createButtonBar(Composite parent)
	{
		Control control = super.createButtonBar(parent);
		control.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		return control;
	}

	public void clearSavedInfo()
	{
		ISecurePreferences preferences = SecurePreferencesFactory.getDefault();
		ISecurePreferences node = preferences.node(SERVOY_LOGIN_STORE_KEY);
		node.clear();
	}

	@Override
	public boolean isHelpAvailable()
	{
		return false;
	}
}