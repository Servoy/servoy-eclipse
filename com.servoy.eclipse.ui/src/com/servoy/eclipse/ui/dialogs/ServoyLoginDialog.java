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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

import org.apache.commons.codec.binary.Base64;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
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
import com.servoy.eclipse.ui.IServoyLoginListener;
import com.servoy.j2db.ClientVersion;
import com.servoy.j2db.util.Utils;

/**
 * @author gboros
 */
public class ServoyLoginDialog extends TitleAreaDialog
{
	public static final String SERVOY_LOGIN_STORE_KEY = "SERVOY_LOGIN_INFO";
	public static final String SERVOY_LOGIN_USERNAME = "USERNAME";
	public static final String SERVOY_LOGIN_PASSWORD = "PASSWORD";
	public static final String SERVOY_LOGIN_TOKEN = "TOKEN";
//	public static final String CROWD_URL = "https://middleware-dev.unifiedui.servoy-cloud.eu/servoy-service/rest_ws/api/developer_auth/getAuthToken";
	public static final String CROWD_URL = System.getProperty("servoy.api.url", "https://middleware-prod.unifiedui.servoy-cloud.eu") +
		"/servoy-service/rest_ws/api/developer_auth/getAuthToken";

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
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
				}
			}
			else if (firstLogin || loginTokenResponse.status == LoginTokenResponse.Status.LOGIN_ERROR)
			{
				clearSavedInfo();
				this.errorMessage = loginTokenResponse.status == LoginTokenResponse.Status.LOGIN_ERROR ? "Login failed, invalid credentials" : "Login failed";
				doLogin();
			}
		}
		if (ServoyLoginDialog.servoyLoginListener != null)
		{
			try
			{
				ServoyLoginDialog.servoyLoginListener.onLogin(username, loginToken);
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}

		return loginToken;
	}

	private LoginTokenResponse getLoginToken(String username, String password)
	{
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpget = new HttpGet(CROWD_URL);

		String auth = username + ":" + password;
		byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("ISO-8859-1")));
		String authHeader = "Basic " + new String(encodedAuth);
		httpget.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
		httpget.addHeader(HttpHeaders.ACCEPT, "application/json");
		httpget.addHeader("servoyVersion", ClientVersion.getBundleVersion());
		httpget.addHeader("os", Utils.getPlatformAsString());

		// execute the request
		try
		{
			return httpclient.execute(httpget, new HttpClientResponseHandler<LoginTokenResponse>()
			{

				@Override
				public LoginTokenResponse handleResponse(ClassicHttpResponse response) throws HttpException, IOException
				{
					HttpEntity responseEntity = response.getEntity();
					String responseString = EntityUtils.toString(responseEntity);
					if (response.getCode() == 200)
					{

						JSONObject loginTokenJSON = new JSONObject(responseString);
						String loginToken = loginTokenJSON.getString("token");
						return new LoginTokenResponse(LoginTokenResponse.Status.OK, loginToken);
					}
					else
					{
						StringBuilder sb = new StringBuilder();
						sb.append("HTTP ERROR : ").append(response.getCode()).append(' ').append(responseString);
						return new LoginTokenResponse(LoginTokenResponse.Status.LOGIN_ERROR, sb.toString());
					}
				}
			});
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
		gridLayout.marginRight = 20;
		gridLayout.marginLeft = 20;
		gridLayout.marginTop = 20;
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(gridLayout);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
		composite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

		Label lbl = new Label(composite, SWT.NONE);
		lbl.setText("Email");
		lbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		final FontDescriptor labelFontDescriptor = FontDescriptor.createFrom(lbl.getFont()).setStyle(SWT.BOLD);
		final Font labelFont = labelFontDescriptor.createFont(getShell().getDisplay());
		lbl.setFont(labelFont);
		lbl.addDisposeListener((e) -> labelFontDescriptor.destroyFont(labelFont));

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
		lbl.setFont(labelFont);
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
		if (!Utils.isAppleMacOS() && !Utils.isLinuxOS()) okBtn.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
	}

	@Override
	protected Control createButtonBar(Composite parent)
	{
		Control control = super.createButtonBar(parent);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 0;
		layout.marginLeft = 20;
		layout.marginRight = 20;
		((Composite)control).setLayout(layout);
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
					PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL("https://admin.servoy-cloud.eu/"));
				}
				catch (PartInitException | MalformedURLException e1)
				{
					ServoyLog.logError(e1);
				}
			}
		});
		GridData gd = new GridData(SWT.FILL, SWT.BEGINNING, true, true);
		gd.horizontalIndent = 10;
		lbl.setLayoutData(gd);
		Cursor handCursor = parent.getDisplay().getSystemCursor(SWT.CURSOR_HAND);
		lbl.setCursor(handCursor);
		if (Util.isMac())
		{
			Label lbl2 = new Label(parent, SWT.NONE);
			lbl2.setText("Having issues storing credentials?");
			lbl2.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			lbl2.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseUp(MouseEvent e)
				{
					SecureStorageInfoDialog infoDialog = new SecureStorageInfoDialog(parent.getShell());
					infoDialog.open();
				}
			});
			GridData gd2 = new GridData(SWT.FILL, SWT.BEGINNING, true, true);
			gd2.horizontalIndent = 10;
			lbl2.setLayoutData(gd2);
			lbl2.setCursor(handCursor);
		}

		control.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		return control;
	}

	public static void clearSavedInfo()
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

	private static IServoyLoginListener servoyLoginListener;

	public static void addLoginListener(IServoyLoginListener loginListener)
	{
		ServoyLoginDialog.servoyLoginListener = loginListener;
	}

	public static String getLoginToken()
	{
		String loginToken = null;

		ISecurePreferences preferences = SecurePreferencesFactory.getDefault();

		ISecurePreferences node = preferences.node(SERVOY_LOGIN_STORE_KEY);
		try
		{
			loginToken = node.get(SERVOY_LOGIN_TOKEN, null);
		}
		catch (StorageException ex)
		{
			ServoyLog.logError(ex);
		}
		return loginToken;
	}

	private class SecureStorageInfoDialog extends Dialog
	{

		public SecureStorageInfoDialog(Shell parentShell)
		{
			super(parentShell);
		}

		@Override
		protected Control createDialogArea(Composite parent)
		{
			Composite container = (Composite)super.createDialogArea(parent);
			Label text = new Label(container, SWT.BEGINNING);
			text.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false,
				false));
			text.setText("Please follow these steps so that the login credentials can be stored." +
				"   \n\n" +
				"	1st Step: Go to Preferences -> General -> Security -> Secure Storage" +
				"   \n\n" +
				"	2nd Step: Within \"Master password providers\" section select \"OS X Keystore Integration\" option." +
				"   \n\n" +
				"	3rd Step: Select \"Change Password...\" and provide a password hint." +
				"   \n\n" +
				"   Try to log in, your credentials should be stored now.");
			return container;
		}

		@Override
		protected void configureShell(Shell newShell)
		{
			super.configureShell(newShell);
			newShell.setText("Login credentials storing issue");
		}

		@Override
		protected Point getInitialSize()
		{
			return new Point(600, 250);
		}

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