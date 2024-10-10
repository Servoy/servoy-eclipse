package com.servoy.eclipse.ui.wizards;

import java.net.URL;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.json.JSONObject;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.j2db.server.ngclient.StatelessLoginHandler;

/**
 * @author emera
 */
public class NewOAuthConfigJsonConfigPage extends WizardPage
{
	private static final String MICROSOFT_DOCS = "https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-implicit-grant-flow#send-the-sign-in-request";
	private static final String GOOGLE_DOCS = "https://developers.google.com/identity/protocols/oauth2/javascript-implicit-flow#oauth-2.0-endpoints";
	private Text jsonTextArea;
	private final NewOAuthConfigWizard wizard;

	protected NewOAuthConfigJsonConfigPage(NewOAuthConfigWizard wizard)
	{
		super("OAuth JSON");
		setTitle("OAuth JSON Configuration");
		setDescription("Review and modify the OAuth JSON configuration.");
		this.wizard = wizard;
	}

	@Override
	public void createControl(Composite parent)
	{
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		container.setLayout(layout);

		Image warn = Activator.getDefault().loadImageFromBundle("warning.png");
		CLabel warnLabel = new CLabel(container, SWT.ICON);
		warnLabel.setImage(warn);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		warnLabel.setLayoutData(data);
		warnLabel.setText("Careful");
		Link warn_ = new Link(container, SWT.WRAP);
		warn_.setLayoutData(data);
		StringBuilder text = new StringBuilder("Adjustments to the configuration must be made according to the ");
		String api = wizard.getJSON().optString(StatelessLoginHandler.OAUTH_API, "Custom");
		text.append(getDocumentationLink(api));
		warn_.setText(text.toString());
		warn_.addListener(SWT.Selection, event -> {
			String url = null;
			switch (api)
			{
				case "Google" :
					url = GOOGLE_DOCS;
					break;
				case "Microsoft" :
					url = MICROSOFT_DOCS;
					break;
			}
			if (url != null)
			{
				try
				{
					PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
		});


		jsonTextArea = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		jsonTextArea.setLayoutData(new GridData(GridData.FILL_BOTH));
		jsonTextArea.addModifyListener(e -> {
			wizard.updateConfig(new JSONObject(jsonTextArea.getText()));
		});

		setControl(container);
	}

	private String getDocumentationLink(String api)
	{
		switch (api)
		{
			case "Google" :
				return "<a href=\"" + GOOGLE_DOCS +
					"\">Google documentation</a>";
			case "Microsoft" :
				return "<a href=\"" + MICROSOFT_DOCS +
					"\">Microsoft documentation</a>";
		}
		return "documentation of the selected provider.\n";
	}

	public String getOAuthJson()
	{
		return jsonTextArea.getText();
	}

	public void updateJsonContent()
	{
		if (jsonTextArea != null)
		{
			jsonTextArea.setText(((NewOAuthConfigWizard)getWizard()).getJSON().toString(4));
		}
	}

	@Override
	public void setVisible(boolean visible)
	{
		super.setVisible(visible);
		updateJsonContent();
	}
}