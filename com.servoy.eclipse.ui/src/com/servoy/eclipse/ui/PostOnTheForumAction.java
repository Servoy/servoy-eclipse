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

package com.servoy.eclipse.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.ServoyLoginDialog;

/**
 * Post on the forum directly from the IDE.
 * @author emera
 */
public class PostOnTheForumAction implements IWorkbenchWindowActionDelegate
{

	public static final String ID = "com.servoy.eclipse.ui.NewForumPost";
	private IWorkbenchWindow window;


	public class ForumPostDialog extends TitleAreaDialog
	{
		private Text subjectText;
		private Combo topicsCombo;
		private Text description;

		private final String[] topics = new String[] { "I'm just getting started", "Installation", "Forms", "Methods", //
			"SQL Databases", "Web Development", "Plugins and Beans", "Programming with Servoy", "Servoy Headless Client", //
			"Servoy Web Client", "Servoy NGClient", "Eclipse Environment" };
		private final int[] topicIds = new int[] { 74, 11, 2, 3, 4, 9, 15, 22, 25, 34, 69, 38 };

		private String errorMessage;
		private static final String LOGIN_URL = "https://forum.servoy.com/ucp.php?mode=login&sid=";
		private static final String POST_URL = "https://forum.servoy.com/posting.php?mode=post&f=";

		protected ForumPostDialog(Shell parentShell)
		{
			super(parentShell);
		}

		public boolean post()
		{
			try (CloseableHttpClient client = HttpClients.createDefault())
			{
				String sid = getSID(client);
				if (sid == null || !login(client, sid)) return false;

				String form_token = getFormToken(client, sid);

				HttpPost httpPost = new HttpPost(POST_URL + topicIds[topicsCombo.getSelectionIndex()] + "&sid=" + sid);
				ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair("subject", subjectText.getText()));
				params.add(new BasicNameValuePair("message", description.getText()));

				//hidden params
				String timestamp = String.valueOf(System.currentTimeMillis());
				params.add(new BasicNameValuePair("lastclick", timestamp));
				params.add(new BasicNameValuePair("post", "Submit"));
				params.add(new BasicNameValuePair("attach_sig", "on"));
				params.add(new BasicNameValuePair("creation_time", timestamp));
				params.add(new BasicNameValuePair("form_token", form_token));//UUID.randomUUID().toString().replace("-", ""))); //TODO check if it does work with a generated form token
				params.add(new BasicNameValuePair("addbbcode20", "100"));
				params.add(new BasicNameValuePair("poll_length", "0"));
				params.add(new BasicNameValuePair("poll_title", ""));
				params.add(new BasicNameValuePair("poll_option_text", ""));
				params.add(new BasicNameValuePair("poll_max_options", ""));
				UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, StandardCharsets.UTF_8);
				httpPost.setEntity(entity);
				httpPost.addHeader("Connection", "keep-alive");
				httpPost.addHeader("Host", "forum.servoy.com");
				httpPost.addHeader("Origin", "https://forum.servoy.com");
				httpPost.addHeader("referer", POST_URL + topicIds[topicsCombo.getSelectionIndex()] + "&sid=" + sid);
				httpPost.addHeader("Content-Type", "multipart/form-data");

				CloseableHttpResponse response = client.execute(httpPost);
				HttpEntity respEntity = response.getEntity();
				if (respEntity != null)//TODO rem
				{
					String content = EntityUtils.toString(respEntity);
					System.out.println(content); //TODO rem
				}
				return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
			}
			catch (IOException | StorageException e)
			{
				ServoyLog.logError(e);
			}
			return false;
		}


		private String getFormToken(CloseableHttpClient client, String sid) throws ClientProtocolException, IOException
		{
			HttpGet httpGet = new HttpGet(POST_URL + topicIds[topicsCombo.getSelectionIndex()] + "&sid=" + sid);
			HttpResponse httpresponse = client.execute(httpGet);
			if (httpresponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
			{
				setErrorMessage("Cannot connect to the forum. Please try again later.");
				return null;
			}
			String content = EntityUtils.toString(httpresponse.getEntity());
			int beginIndex = content.indexOf("form_token") + 19;
			String token = content.substring(beginIndex, beginIndex + 32);
			return token;
		}

		private boolean login(CloseableHttpClient client, String sid) throws StorageException, IOException, ClientProtocolException
		{
			ISecurePreferences preferences = SecurePreferencesFactory.getDefault();
			ISecurePreferences node = preferences.node(ServoyLoginDialog.SERVOY_LOGIN_STORE_KEY);
			HttpPost httpPost = new HttpPost(LOGIN_URL + sid);
			ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("username",
				node.get(ServoyLoginDialog.SERVOY_LOGIN_USERNAME, null)));
			params.add(new BasicNameValuePair("password", node.get(ServoyLoginDialog.SERVOY_LOGIN_PASSWORD, null)));
			params.add(new BasicNameValuePair("login", "Login"));
			params.add(new BasicNameValuePair("redirect", "./index.php?"));

			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, StandardCharsets.UTF_8);
			httpPost.setEntity(entity);
			httpPost.addHeader("Connection", "keep-alive");
			httpPost.addHeader("referer", "https://forum.servoy.com/index.php?sid=" + sid);
			HttpResponse httpresponse = client.execute(httpPost);

			//Printing the status and the contents of the response
			System.out.println(EntityUtils.toString(httpresponse.getEntity()));
			System.out.println(httpresponse.getStatusLine());

			if (httpresponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
			{
				setErrorMessage("Could not log in.");
				return false;
			}
			return true;
		}


		private String getSID(CloseableHttpClient client) throws IOException, ClientProtocolException
		{
			HttpGet httpGet = new HttpGet("https://forum.servoy.com/");
			HttpResponse httpresponse = client.execute(httpGet);
			if (httpresponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
			{
				getShell().setText("Cannot connect to the forum. Please try again later.");
				return null;
			}
			String content = EntityUtils.toString(httpresponse.getEntity());
			int beginIndex = content.indexOf("index.php?sid=") + 14;
			String sid = content.substring(beginIndex, beginIndex + 32);
			return sid;
		}

		@Override
		protected Control createContents(Composite parent)
		{
			Control contents = super.createContents(parent);
			setTitle("Servoy Forum");
			getShell().setText("Ask on the Forum");
			setMessage("Post your question here:");
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
			Composite topLevel = new Composite(parent, SWT.NONE);
			topLevel.setLayout(gridLayout);
			GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
			data.widthHint = 500;
			data.heightHint = 400;
			topLevel.setLayoutData(data);
			topLevel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

			Label topicLabel = new Label(topLevel, SWT.NONE);
			topicLabel.setText("Topic");
			topicLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			FontDescriptor descriptor = FontDescriptor.createFrom(topicLabel.getFont());
			descriptor = descriptor.setStyle(SWT.BOLD);
			topicLabel.setFont(descriptor.createFont(getShell().getDisplay()));

			topicsCombo = new Combo(topLevel, SWT.READ_ONLY);
			topicsCombo.setItems(topics);
			topicsCombo.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			GridData gridData = new GridData();
			gridData.horizontalAlignment = GridData.FILL;
			gridData.grabExcessHorizontalSpace = true;
			topicsCombo.setLayoutData(gridData);
			topicsCombo.select(0);

			Label subjectLabel = new Label(topLevel, SWT.NONE);
			subjectLabel.setText("Subject");
			subjectLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			subjectLabel.setFont(descriptor.createFont(getShell().getDisplay()));
			subjectText = new Text(topLevel, SWT.BORDER);
			subjectText.setLayoutData(gridData);

			description = new Text(topLevel, SWT.MULTI | SWT.BORDER);
			gridData = new GridData();
			gridData.horizontalAlignment = GridData.FILL;
			gridData.verticalAlignment = GridData.FILL;
			gridData.grabExcessHorizontalSpace = true;
			gridData.grabExcessVerticalSpace = true;
			gridData.horizontalSpan = 2;
			description.setLayoutData(gridData);

			return topLevel;
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent)
		{
			Button post = createButton(parent, IDialogConstants.OK_ID, "Post", false);
			Button cancel = createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
			parent.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			post.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			cancel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		}

		@Override
		protected Control createButtonBar(Composite parent)
		{
			Control control = super.createButtonBar(parent);
			control.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			return control;
		}

		@Override
		protected void okPressed()
		{
			if (post())
			{
				super.okPressed();
			}
		}


		@Override
		public boolean isHelpAvailable()
		{
			return false;
		}
	}

	@Override
	public void run(IAction action)
	{
		ForumPostDialog dialog = new ForumPostDialog(window.getShell());
		dialog.open();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection)
	{
	}


	@Override
	public void dispose()
	{
	}

	@Override
	public void init(IWorkbenchWindow win)
	{
		this.window = win;
	}
}
