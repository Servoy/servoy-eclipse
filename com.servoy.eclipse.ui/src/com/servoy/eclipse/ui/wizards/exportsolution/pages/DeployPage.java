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

package com.servoy.eclipse.ui.wizards.exportsolution.pages;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.wizards.ExportSolutionWizard;
import com.servoy.j2db.util.Utils;

/**
 * @author gboros
 *
 */
public class DeployPage extends WizardPage implements IJobChangeListener
{
	private final ExportSolutionWizard exportSolutionWizard;
	private Button deploy;
	private Text deployOutput;

	public DeployPage(ExportSolutionWizard exportSolutionWizard)
	{
		super("page6");
		setTitle("Deploy");
		setDescription("Deploy to Servoy application server");
		this.exportSolutionWizard = exportSolutionWizard;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent)
	{
		GridLayout gridLayout = new GridLayout(2, false);
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(gridLayout);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

		Label lbl = new Label(composite, SWT.NONE);
		lbl.setText("Deploy URL");
		final Text deployURLTxt = new Text(composite, SWT.BORDER);
		deployURLTxt.setText(exportSolutionWizard.getDeployURL());
		deployURLTxt.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

		lbl = new Label(composite, SWT.NONE);
		lbl.setText("Username");
		final Text usernameTxt = new Text(composite, SWT.BORDER);
		usernameTxt.setText(exportSolutionWizard.getDeployUsername());
		usernameTxt.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

		lbl = new Label(composite, SWT.NONE);
		lbl.setText("Password");
		// On MacOS, SWT 3.5 does not send events to listeners on password fields.
		// See: http://www.eclipse.org/forums/index.php?t=msg&goto=508058&
		int style = SWT.BORDER;
		if (!Utils.isAppleMacOS()) style |= SWT.PASSWORD;
		final Text passwordTxt = new Text(composite, style);
		passwordTxt.setText(exportSolutionWizard.getDeployPassword());
		passwordTxt.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
		if (Utils.isAppleMacOS()) passwordTxt.setEchoChar('\u2022');

		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		deploy = new Button(composite, SWT.PUSH);
		deploy.setLayoutData(gd);
		deploy.setText("Deploy");
		deploy.addSelectionListener(new SelectionListener()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				exportSolutionWizard.setDeployURL(deployURLTxt.getText());
				exportSolutionWizard.setDeployUsername(usernameTxt.getText());
				exportSolutionWizard.setDeployPassword(passwordTxt.getText());
				deployOutput.setText("");
				doDeploy(exportSolutionWizard.getDeployURL(), exportSolutionWizard.getDeployUsername(), exportSolutionWizard.getDeployPassword(),
					exportSolutionWizard.getModel().getFileName());
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e)
			{
				// TODO Auto-generated method stub
			}
		});

		gd = new GridData();
		gd.horizontalSpan = 2;
		lbl = new Label(composite, SWT.NONE);
		lbl.setLayoutData(gd);
		lbl.setText("Response");

		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 2;
		deployOutput = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		deployOutput.setEditable(false);
		deployOutput.setLayoutData(gd);
		setControl(composite);
	}

	private void doDeploy(final String url, final String username, final String password, final String exportFile)
	{
		deploy.setEnabled(false);
		Job job = new Job("Deploying to Servoy application server")
		{
			private void updateDeployOutput(final String message)
			{
				Display.getDefault().syncExec(new Runnable()
				{
					@Override
					public void run()
					{
						deployOutput.setText(message);
					}
				});
			}

			@Override
			protected IStatus run(IProgressMonitor monitor)
			{
				final StringBuilder responseMessage = new StringBuilder();
				// the file we want to upload
				File inFile = new File(exportFile);
				try
				{
					responseMessage.append("Deploy started ...").append('\n');
					updateDeployOutput(responseMessage.toString());

					HttpClient httpclient = HttpClients.createDefault();
					HttpPost httppost = new HttpPost(url);

					String auth = username + ":" + password;
					byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("ISO-8859-1")));
					String authHeader = "Basic " + new String(encodedAuth);
					httppost.setHeader(HttpHeaders.AUTHORIZATION, authHeader);


					MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
					multipartEntityBuilder.addPart("if", new FileBody(inFile));
					if (exportSolutionWizard.getModel().isProtectWithPassword())
					{
						multipartEntityBuilder.addPart("solution_password",
							new StringBody(exportSolutionWizard.getModel().getPassword(), ContentType.MULTIPART_FORM_DATA));
					}
					httppost.setEntity(multipartEntityBuilder.build());

					// execute the request
					HttpResponse response = httpclient.execute(httppost);

					if (response.getStatusLine().getStatusCode() == 200)
					{
						HttpEntity responseEntity = response.getEntity();
						String responseString = EntityUtils.toString(responseEntity);
						String[] responses = responseString.split("\n");

						for (String s : responses)
						{
							responseMessage.append(s.trim()).append('\n');
						}
						responseMessage.append("Done!");
					}
					else
					{
						responseMessage.append("HTTP ERROR : ").append(response.getStatusLine().getStatusCode()).append(' ').append(
							response.getStatusLine().getReasonPhrase());
					}
				}
				catch (ClientProtocolException e)
				{
					responseMessage.append("Unable to make connection").append('\n').append(e.getMessage());
					ServoyLog.logError(e);
				}
				catch (IOException e)
				{
					responseMessage.append("Unable to read file").append('\n').append(e.getMessage());
					ServoyLog.logError(e);
				}
				finally
				{
					updateDeployOutput(responseMessage.toString());
				}

				return Status.OK_STATUS;
			}
		};
		job.addJobChangeListener(this);
		job.schedule();
	}

	@Override
	public void setVisible(boolean visible)
	{
		super.setVisible(visible);
		if (visible)
		{
			deployOutput.setText("");
			deploy.setEnabled(false);
			exportSolutionWizard.doExport(this);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.runtime.jobs.IJobChangeListener#aboutToRun(org.eclipse.core.runtime.jobs.IJobChangeEvent)
	 */
	@Override
	public void aboutToRun(IJobChangeEvent event)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.runtime.jobs.IJobChangeListener#awake(org.eclipse.core.runtime.jobs.IJobChangeEvent)
	 */
	@Override
	public void awake(IJobChangeEvent event)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.runtime.jobs.IJobChangeListener#done(org.eclipse.core.runtime.jobs.IJobChangeEvent)
	 */
	@Override
	public void done(IJobChangeEvent event)
	{
		Display.getDefault().syncExec(new Runnable()
		{
			@Override
			public void run()
			{
				if (!deploy.isDisposed())
				{
					deploy.setEnabled(true);
				}
			}
		});
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.runtime.jobs.IJobChangeListener#running(org.eclipse.core.runtime.jobs.IJobChangeEvent)
	 */
	@Override
	public void running(IJobChangeEvent event)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.runtime.jobs.IJobChangeListener#scheduled(org.eclipse.core.runtime.jobs.IJobChangeEvent)
	 */
	@Override
	public void scheduled(IJobChangeEvent event)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.runtime.jobs.IJobChangeListener#sleeping(org.eclipse.core.runtime.jobs.IJobChangeEvent)
	 */
	@Override
	public void sleeping(IJobChangeEvent event)
	{
		// TODO Auto-generated method stub

	}
}