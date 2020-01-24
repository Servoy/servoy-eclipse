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

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.json.JSONObject;

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
	private static String CROWD_URL = "https://analytics-dev.analytics.servoy-cloud.eu/servoy-service/rest_ws/svyAnalyticsServer/v1/auth";

	private String dlgUsername = "";
	private String dlgPassword = "";
	private String errorMessage = null;

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
		}
		catch (StorageException ex)
		{
			ServoyLog.logError(ex);
		}

		boolean firstLogin = false;
		if (username == null || password == null)
		{
			if (open() == OK)
			{
				firstLogin = true;
				username = dlgUsername;
				password = dlgPassword;
			}
		}

		if (username != null && password != null)
		{
			LoginTokenResponse loginTokenResponse = getLoginToken(username, password);
			if (loginTokenResponse.status == LoginTokenResponse.Status.OK)
			{
				loginToken = loginTokenResponse.response;
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
			else if (firstLogin || loginTokenResponse.status == LoginTokenResponse.Status.LOGIN_ERROR)
			{
				clearSavedInfo();
				this.errorMessage = "Login failed";
				doLogin();
			}
		}


		return loginToken;
	}

	private LoginTokenResponse getLoginToken(String username, String password)
	{
		String loginToken = null;

		HttpClient httpclient = HttpClients.createDefault();
		HttpPost httppost = new HttpPost(CROWD_URL);

		String auth = username + ":" + password;
		byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("ISO-8859-1")));
		String authHeader = "Basic " + new String(encodedAuth);
		httppost.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
		httppost.addHeader(HttpHeaders.ACCEPT, "application/json");

		// execute the request
		HttpResponse response;
		try
		{
			response = httpclient.execute(httppost);
			HttpEntity responseEntity = response.getEntity();
			String responseString = EntityUtils.toString(responseEntity);
			if (response.getStatusLine().getStatusCode() == 200)
			{

				JSONObject loginTokenJSON = new JSONObject(responseString);
				loginToken = loginTokenJSON.getString("token");
				return new LoginTokenResponse(LoginTokenResponse.Status.OK, loginToken);
			}
			else
			{
				StringBuilder sb = new StringBuilder();
				sb.append("HTTP ERROR : ").append(response.getStatusLine().getStatusCode()).append(' ').append(responseString);
				return new LoginTokenResponse(LoginTokenResponse.Status.LOGIN_ERROR, sb.toString());
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
			return new LoginTokenResponse(LoginTokenResponse.Status.ERROR, ex.toString());
		}
	}

	@Override
	protected Control createContents(Composite parent)
	{
		Control contents = super.createContents(parent);
		setTitle("Servoy login");
		getShell().setText("Servoy");
		setMessage("Welcome, please login to Servoy");
		setTitleImage(Activator.getDefault().loadImageFromBundle("solution_wizard_description.png"));
		setErrorMessage(null);
		if (this.errorMessage != null)
		{
			setErrorMessage(this.errorMessage);
		}
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
		lbl.setText("Email");
		lbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		FontDescriptor descriptor = FontDescriptor.createFrom(lbl.getFont());
		descriptor = descriptor.setStyle(SWT.BOLD);
		lbl.setFont(descriptor.createFont(getShell().getDisplay()));
		Text usernameTxt = new Text(composite, SWT.BORDER);
		usernameTxt.setText(dlgUsername);
		usernameTxt.selectAll();
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
		if (!Utils.isAppleMacOS()) okBtn.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
	}

	@Override
	protected Control createButtonBar(Composite parent)
	{
		Control control = super.createButtonBar(parent);
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText("Forgot password?");
		lbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		lbl.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseUp(MouseEvent e)
			{
				try
				{
					PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL("https://servoy.com/profile/login/"));
				}
				catch (PartInitException | MalformedURLException e1)
				{
					ServoyLog.logError(e1);
				}
			}
		});
		GridData gd = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		gd.horizontalIndent = 10;
		lbl.setLayoutData(gd);
		lbl.setCursor(new Cursor(parent.getDisplay(), SWT.CURSOR_HAND));
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

class LoginTokenResponse
{
	enum Status
	{
		OK, LOGIN_ERROR, ERROR
	}

	Status status;
	String response;

	LoginTokenResponse(Status status, String response)
	{
		this.status = status;
		this.response = response;
	}
}