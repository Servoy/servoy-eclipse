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
import com.servoy.j2db.server.ngclient.auth.OAuthUtils.OAuthParameters;
import com.servoy.j2db.server.ngclient.auth.OAuthUtils.Provider;

/**
 * @author emera
 */
public class NewOAuthConfigJsonConfigPage extends WizardPage
{
	private Text jsonTextArea;
	private final NewOAuthConfigWizard wizard;
	private Link warn_;

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
		warn_ = new Link(container, SWT.WRAP);
		warn_.setLayoutData(data);
		StringBuilder text = new StringBuilder("Adjustments to the configuration must be made according to the ");
		Provider api = Provider.valueOf(wizard.getJSON().optString(OAuthParameters.api.name(), Provider.Custom.name()));
		text.append(getDocumentationLink(api));
		warn_.setText(text.toString());
		warn_.addListener(SWT.Selection, event -> openLinkInBrowser());


		jsonTextArea = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		jsonTextArea.setLayoutData(new GridData(GridData.FILL_BOTH));
		jsonTextArea.addModifyListener(e -> {
			if (jsonTextArea.getText().trim().startsWith("{"))
				wizard.updateConfig(new JSONObject(jsonTextArea.getText().trim()));
		});

		setControl(container);
	}

	private void openLinkInBrowser()
	{
		Provider api = Provider.valueOf(wizard.getJSON().optString(OAuthParameters.api.name(), Provider.Custom.name()));
		String url = api.getDocs();
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
	}

	private String getDocumentationLink(Provider api)
	{
		if (api == Provider.Custom)
		{
			return "documentation of the selected provider.\n";
		}
		else
		{
			return "<a href=\"" + api.getDocs() +
				"\">" + api.name() + " documentation</a>";
		}
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
		StringBuilder text = new StringBuilder("New parameters and adjustments to the configuration must be made according to the ");
		Provider api = Provider.valueOf(wizard.getJSON().optString(OAuthParameters.api.name(), Provider.Custom.name()));
		text.append(getDocumentationLink(api));
		warn_.setText(text.toString());
	}
}