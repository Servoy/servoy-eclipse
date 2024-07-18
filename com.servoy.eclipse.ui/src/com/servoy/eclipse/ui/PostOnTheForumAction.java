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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
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
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.ServoyLoginDialog;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.Utils;

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
		private Button post;
		private static final String POST_URL = "https://forum.servoy.com/submit_post.php?f=";

		protected ForumPostDialog(Shell parentShell)
		{
			super(parentShell);
		}

		public boolean post()
		{
			try (CloseableHttpClient client = HttpClients.createDefault())
			{
				HttpClientContext context = HttpClientContext.create();
				HttpPost httpPost = new HttpPost(POST_URL + topicIds[topicsCombo.getSelectionIndex()]);
				ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
				ISecurePreferences preferences = SecurePreferencesFactory.getDefault();
				ISecurePreferences node = preferences.node(ServoyLoginDialog.SERVOY_LOGIN_STORE_KEY);
				params.add(new BasicNameValuePair("username",
					node.get(ServoyLoginDialog.SERVOY_LOGIN_USERNAME, null)));
				params.add(new BasicNameValuePair("password", node.get(ServoyLoginDialog.SERVOY_LOGIN_PASSWORD, null)));
				params.add(new BasicNameValuePair("subject", subjectText.getText()));
				params.add(new BasicNameValuePair("message", description.getText()));

				UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, StandardCharsets.UTF_8);
				httpPost.setEntity(entity);
				CloseableHttpResponse response = client.execute(httpPost, context);
				String content = EntityUtils.toString(response.getEntity());
				if (response.getCode() == HttpStatus.SC_OK && content.contains("viewtopic.php"))
				{
					MessageDialog dialog = new MessageDialog(UIUtils.getActiveShell(), "Thank you for posting", null,
						"You can view your post on the Servoy forum.", MessageDialog.CONFIRM,
						new String[] { "View post", "Close" }, 0);
					dialog.setBlockOnOpen(true);
					if (dialog.open() == 0)
					{
						try
						{
							String url = HtmlUtils.unescape(content.startsWith("./") ? content.replaceFirst(".", "https://forum.servoy.com") : content);
							PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
						}
						catch (PartInitException e)
						{
							ServoyLog.logError(e);
						}
					}
				}
				else if (response.getCode() == HttpStatus.SC_UNAUTHORIZED)
				{
					setErrorMessage("Could not login on the forum.");
				}
				else
				{
					ServoyLog.logInfo("Cannot post to the forum " + EntityUtils.toString(response.getEntity()));
					setErrorMessage("Cannot post to the forum. Please try again later.");
				}
				return response.getCode() == HttpStatus.SC_OK;
			}
			catch (IOException | StorageException | ParseException e)
			{
				ServoyLog.logError(e);
			}
			return false;
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
			FontDescriptor descriptor = FontDescriptor.createFrom(topicLabel.getFont()).setStyle(SWT.BOLD);
			Font font = descriptor.createFont(getShell().getDisplay());
			topicLabel.setFont(font);
			topicLabel.addDisposeListener((e) -> descriptor.destroyFont(font));

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
			subjectLabel.setFont(font);
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

			subjectText.addListener(SWT.Modify, event -> {
				post.setEnabled(!Utils.stringIsEmpty(subjectText.getText()) && !Utils.stringIsEmpty(description.getText()));
			});
			description.addListener(SWT.Modify, event -> {
				post.setEnabled(!Utils.stringIsEmpty(subjectText.getText()) && !Utils.stringIsEmpty(description.getText()));
			});

			return topLevel;
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent)
		{
			post = createButton(parent, IDialogConstants.OK_ID, "Post", false);
			post.setEnabled(!Utils.stringIsEmpty(subjectText.getText()) && !Utils.stringIsEmpty(description.getText()));
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
