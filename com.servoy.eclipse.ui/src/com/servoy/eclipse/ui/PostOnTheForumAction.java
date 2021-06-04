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

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
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
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.util.UIUtils;
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

		private class ContentLengthHeaderRemover implements HttpRequestInterceptor
		{
			@Override
			public void process(HttpRequest request, HttpContext context) throws HttpException, IOException
			{
				request.removeHeaders(HTTP.CONTENT_LEN);// fighting org.apache.http.protocol.RequestContent's ProtocolException("Content-Length header already present");
			}
		}

		public boolean post()
		{
			CookieStore httpCookieStore = new BasicCookieStore();
			try (CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(httpCookieStore).addInterceptorFirst(new ContentLengthHeaderRemover())
				.build())
			{
				String sid = getSID(client);
				if (sid == null || !login(client, sid)) return false;

				String form_token = getFormToken(client, sid, httpCookieStore);

				HttpPost httpPost = new HttpPost(POST_URL + topicIds[topicsCombo.getSelectionIndex()] + "&sid=" + sid);

				MultipartEntityBuilder requestEntity = MultipartEntityBuilder.create();
				requestEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
				requestEntity.addTextBody("subject", subjectText.getText());
				requestEntity.addTextBody("addbbcode20", "100");
				requestEntity.addTextBody("message", description.getText());
				String timestamp = String.valueOf(System.currentTimeMillis());
				requestEntity.addTextBody("lastclick", timestamp);
				requestEntity.addTextBody("post", "Submit");
				requestEntity.addTextBody("attach_sig", "on");
				requestEntity.addTextBody("creation_time", timestamp);
				requestEntity.addTextBody("form_token", form_token);
				requestEntity.addBinaryBody("fileupload", new byte[0], ContentType.APPLICATION_OCTET_STREAM, "");
				requestEntity.addTextBody("filecomment", "");
				requestEntity.addTextBody("poll_title", "");
				requestEntity.addTextBody("poll_option_text", "");
				requestEntity.addTextBody("poll_max_options", "1");
				requestEntity.addTextBody("poll_length", "0");
				HttpEntity httpEntity = requestEntity.build();
				httpPost.setEntity(httpEntity);

				httpPost.addHeader(HttpHeaders.ACCEPT,
					"text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
				httpPost.addHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate, br");
				httpPost.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "ro-RO,ro;q=0.9,en-US;q=0.8,en;q=0.7");
				httpPost.addHeader(HttpHeaders.CACHE_CONTROL, "max-age=0");
				httpPost.addHeader(HttpHeaders.CONNECTION, "keep-alive");
				httpPost.addHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(httpEntity.getContentLength()));
				//httpPost.addHeader(HttpHeaders.CONTENT_TYPE, "multipart/form-data"); //OR the next line
				httpPost.addHeader(httpEntity.getContentType()); //also includes the boundary but then I get a 302 http status...
				//String cookies = httpCookieStore.getCookies().stream().map(c -> c.getName() + "=" + c.getValue()).collect(Collectors.joining("; "));
				httpPost.addHeader("Cookie",
					"_ga=GA1.2.755388126.1618211851; style_cookie=printonly; __utmc=134421478; __utmz=134421478.1621605136.2.2.utmcsr=support.servoy.com|utmccn=(referral)|utmcmd=referral|utmcct=/; ajs_user_id=\"1215447197c7034c75c99165ffd3ad77b65fe9fa\"; ajs_anonymous_id=\"54ca35ce-eea6-4066-9dfb-23b3ab040a08\"; phpbb3_5o7yg_k=; __utma=134421478.755388126.1618211851.1622716183.1622814302.20; __utmt=1; phpbb3_5o7yg_u=54779; phpbb3_5o7yg_sid=" +
						sid + "; __utmb=134421478.3.10.1622814302");
				httpPost.addHeader(HttpHeaders.HOST, "forum.servoy.com");
				httpPost.addHeader("Origin", "https://forum.servoy.com");
				httpPost.addHeader(HttpHeaders.REFERER, POST_URL + topicIds[topicsCombo.getSelectionIndex()]);
				httpPost.addHeader("sec-ch-ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"90\", \"Google Chrome\";v=\"90\"");
				httpPost.addHeader("sec-ch-ua-mobile", "?0");
				httpPost.addHeader("Sec-Fetch-Dest", "document");
				httpPost.addHeader("Sec-Fetch-Mode", "navigate");
				httpPost.addHeader("Sec-Fetch-Site", "same-origin");
				httpPost.addHeader("Sec-Fetch-User", "?1");
				httpPost.addHeader("Upgrade-Insecure-Requests", "1");
				httpPost.addHeader(HttpHeaders.USER_AGENT,
					"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Safari/537.36");

				//TODO remove printing the headers and content
				for (Header h : httpPost.getAllHeaders())
				{
					System.out.println(h.getName() + ": " + h.getValue());
				}
				System.out.println(EntityUtils.toString(httpEntity));

				CloseableHttpResponse response = client.execute(httpPost);
				HttpEntity respEntity = response.getEntity();
				if (respEntity != null)//TODO rem
				{
					String content = EntityUtils.toString(respEntity);
					System.out.println(content); //TODO rem
					System.out.println(" STATUS " + response.getStatusLine().getReasonPhrase());
				}
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
				{
					//TODO button for showing the post on the forum
					UIUtils.showInformation(getParentShell(), "Thank you for posting", "View your post on the Servoy forum here ...");
				}
				else
				{
					setErrorMessage("Connot post to the forum. Please try again later.");
				}
				return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
			}
			catch (IOException | StorageException e)
			{
				ServoyLog.logError(e);
			}
			return false;
		}


		private String getFormToken(CloseableHttpClient client, String sid, CookieStore cookiesStore) throws ClientProtocolException, IOException
		{
			HttpGet httpGet = new HttpGet(POST_URL + topicIds[topicsCombo.getSelectionIndex()] + "&sid=" + sid);
//			String cookies = cookiesStore.getCookies().stream().map(c -> c.getName() + "=" + c.getValue()).collect(Collectors.joining("; "));
//			httpGet.addHeader("Cookie", cookies);
			httpGet.addHeader("Cookie",
				"_ga=GA1.2.755388126.1618211851; style_cookie=printonly; __utmc=134421478; __utmz=134421478.1621605136.2.2.utmcsr=support.servoy.com|utmccn=(referral)|utmcmd=referral|utmcct=/; ajs_user_id=\"1215447197c7034c75c99165ffd3ad77b65fe9fa\"; ajs_anonymous_id=\"54ca35ce-eea6-4066-9dfb-23b3ab040a08\"; phpbb3_5o7yg_k=; __utma=134421478.755388126.1618211851.1622716183.1622814302.20; __utmt=1; phpbb3_5o7yg_u=54779; phpbb3_5o7yg_sid=" +
					sid + "; __utmb=134421478.3.10.1622814302");
			httpGet.addHeader("Connection", "keep-alive");
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
		String loginToken = ServoyLoginDialog.getLoginToken();
		if (loginToken == null)
		{
			loginToken = new ServoyLoginDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell()).doLogin();
		}
		if (loginToken == null) return;
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
