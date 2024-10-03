package com.servoy.eclipse.ui.wizards;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.json.JSONObject;

import com.servoy.eclipse.ui.Activator;
import com.servoy.j2db.server.ngclient.StatelessLoginHandler;

/**
 *
 * @author emera
 */
public class NewOAuthApiPage extends WizardPage
{
	private static final String MICROSOFT_JWKS = "https://login.microsoftonline.com/common/discovery/v2.0/keys";
	private static final String GOOGLE_JWKS = "https://www.googleapis.com/oauth2/v3/certs";
	private Combo apiCombo;
	private Text clientIdText;
	private Text clientSecretText;
	private Button onlineAccessType;
	private Button offlineAccessType;
	private Text scopeText;
	private final Map<String, Control> keysToControl = new HashMap<>();
	private Text jwksUriText;
	private Text authorizationBaseUrlText;
	private Text accessTokenEndpointText;
	private Text jsonTextArea;

	protected NewOAuthApiPage()
	{
		super("OAuth Configuration");
		setTitle("OAuth Configuration Wizard");
		setDescription("Setup the OAuth configuration for stateless login");
	}

	@Override
	public void createControl(Composite parent)
	{
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 10;
		layout.marginWidth = 20;
		container.setLayout(layout);

		Label apiLabel = new Label(container, SWT.NONE);
		apiLabel.setText("OAuth Provider:");

		apiCombo = new Combo(container, SWT.READ_ONLY);
		String[] items = new String[] { "Google", "Microsoft", "Custom" };
		apiCombo.setItems(items);
		JSONObject oauthJson = ((NewOAuthConfigWizard)getWizard()).getJSON();
		apiCombo.select(0); // TODO default selection
		apiCombo.addModifyListener(e -> updateJsonContent());

		Label clientIdLabel = new Label(container, SWT.NONE);
		clientIdLabel.setText("Client ID:");

		clientIdText = new Text(container, SWT.BORDER);
		clientIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		clientIdText.setText(oauthJson.optString(StatelessLoginHandler.CLIENT_ID, ""));
		clientIdText.addListener(SWT.FocusOut, e -> dialogChanged());
		clientIdText.addModifyListener(e -> dialogChanged());
		keysToControl.put(StatelessLoginHandler.CLIENT_ID, clientIdText);

		Label clientSecretLabel = new Label(container, SWT.NONE);
		clientSecretLabel.setText("Client Secret:");

		clientSecretText = new Text(container, SWT.BORDER);
		clientSecretText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		clientSecretText.setText(oauthJson.optString(StatelessLoginHandler.API_SECRET, ""));
		clientSecretText.addListener(SWT.FocusOut, e -> dialogChanged());
		clientSecretText.addModifyListener(e -> dialogChanged());
		keysToControl.put(StatelessLoginHandler.API_SECRET, clientSecretText);

		Label accessTypeLabel = new Label(container, SWT.NONE);
		accessTypeLabel.setText("Access Type:");

		Composite accessTypeGroup = new Composite(container, SWT.NONE);
		accessTypeGroup.setLayout(new RowLayout(SWT.HORIZONTAL));

		//TODO remember me
		offlineAccessType = new Button(accessTypeGroup, SWT.RADIO);
		offlineAccessType.setText("Offline");
		offlineAccessType.setSelection(true);
		offlineAccessType.addListener(SWT.DefaultSelection, e -> dialogChanged());

		onlineAccessType = new Button(accessTypeGroup, SWT.RADIO);
		onlineAccessType.setText("Online");
		onlineAccessType.addListener(SWT.DefaultSelection, e -> dialogChanged());

		Label scopeLabel = new Label(container, SWT.NONE);
		scopeLabel.setText("Default scope:");

		scopeText = new Text(container, SWT.BORDER);
		scopeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		scopeText.setText(oauthJson.optString(StatelessLoginHandler.DEFAULT_SCOPE, "email"));
		scopeText.addListener(SWT.FocusOut, e -> dialogChanged());
		scopeText.addModifyListener(e -> dialogChanged());
		keysToControl.put(StatelessLoginHandler.DEFAULT_SCOPE, scopeText);

		int indexOfCustom = Arrays.asList(items).indexOf("Custom");

		Label authorizationBaseUrlLabel = new Label(container, SWT.NONE);
		authorizationBaseUrlLabel.setText("Authorization Base URL:");

		authorizationBaseUrlText = new Text(container, SWT.BORDER);
		authorizationBaseUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		authorizationBaseUrlText.addListener(SWT.FocusOut, e -> dialogChanged());
		authorizationBaseUrlText.addModifyListener(e -> dialogChanged());
		authorizationBaseUrlText.setEnabled(apiCombo.getSelectionIndex() == indexOfCustom);
		keysToControl.put(StatelessLoginHandler.AUTHORIZATION_BASE_URL, authorizationBaseUrlText);

		Label accessTokenEndpointLabel = new Label(container, SWT.NONE);
		accessTokenEndpointLabel.setText("Access Token Endpoint:");

		accessTokenEndpointText = new Text(container, SWT.BORDER);
		accessTokenEndpointText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		accessTokenEndpointText.setText(oauthJson.optString(StatelessLoginHandler.ACCESS_TOKEN_ENDPOINT, ""));
		accessTokenEndpointText.addListener(SWT.FocusOut, e -> dialogChanged());
		accessTokenEndpointText.addModifyListener(e -> dialogChanged());
		accessTokenEndpointText.setEnabled(apiCombo.getSelectionIndex() == indexOfCustom);
		keysToControl.put(StatelessLoginHandler.ACCESS_TOKEN_ENDPOINT, accessTokenEndpointText);

		Label jwksUriLabel = new Label(container, SWT.NONE);
		jwksUriLabel.setText("JWKS URI:");

		jwksUriText = new Text(container, SWT.BORDER);
		jwksUriText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		jwksUriText.setText(oauthJson.optString(StatelessLoginHandler.JWKS_URI, ""));
		jwksUriText.addListener(SWT.FocusOut, e -> dialogChanged());
		jwksUriText.addModifyListener(e -> dialogChanged());
		jwksUriText.setEnabled(apiCombo.getSelectionIndex() == indexOfCustom);
		keysToControl.put(StatelessLoginHandler.JWKS_URI, jwksUriText);

		Image warn = Activator.getDefault().loadImageFromBundle("warning.png");
		CLabel warnLabel = new CLabel(container, SWT.ICON);
		warnLabel.setImage(warn);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		warnLabel.setLayoutData(data);
		warnLabel.setText("Careful");
		Label warn_ = new Label(container, SWT.WRAP);
		warn_.setLayoutData(data);
		warn_.setText("Adjustments to the configuration must be made according to the documentation of the selected provider.\n");

		jsonTextArea = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 2;
		gd.heightHint = 300;
		jsonTextArea.setLayoutData(gd);
		jsonTextArea.setEditable(true); // Allow the user to edit the JSON if needed
		jsonTextArea.setText(oauthJson.toString(4));
		//TODO sync json with the other text on modify

		setControl(container);
		setPageComplete(isPageComplete());
		updateJsonContent();
	}

	private void updateJsonContent()
	{
		String provider = apiCombo.getText();
		JSONObject oauthJson = ((NewOAuthConfigWizard)getWizard()).getJSON();

		if ("Custom".equals(provider) && oauthJson.has(StatelessLoginHandler.AUTHORIZATION_BASE_URL))
		{
			oauthJson.remove(StatelessLoginHandler.AUTHORIZATION_BASE_URL);
			oauthJson.remove(StatelessLoginHandler.ACCESS_TOKEN_ENDPOINT);
		}
		switch (provider)
		{
			case "Google" :
				oauthJson.put(StatelessLoginHandler.OAUTH_API, "com.github.scribejava.apis.GoogleApi20");
				oauthJson.put(StatelessLoginHandler.JWKS_URI, GOOGLE_JWKS);
				break;
			case "Microsoft" :
				oauthJson.put(StatelessLoginHandler.OAUTH_API, "com.github.scribejava.apis.MicrosoftAzureActiveDirectory20Api");
				//TODO tenant, additional params, others?
				oauthJson.put(StatelessLoginHandler.JWKS_URI, MICROSOFT_JWKS);
				break;
			case "Custom" :
				oauthJson.remove(StatelessLoginHandler.OAUTH_API);
				oauthJson.put(StatelessLoginHandler.JWKS_URI, "");
				break;
		}

		getWizard().getContainer().updateButtons();
	}

	private void dialogChanged()
	{
		boolean isComplete = !clientIdText.getText().isEmpty() && !clientSecretText.getText().isEmpty();
		if (isComplete)
		{
			JSONObject json = ((NewOAuthConfigWizard)getWizard()).getJSON();
			json.put(StatelessLoginHandler.CLIENT_ID, clientIdText.getText().trim());
			json.put(StatelessLoginHandler.API_SECRET, clientSecretText.getText().trim());
			json.put("access_type", offlineAccessType.getSelection() ? "offline" : "online");
			json.put(StatelessLoginHandler.DEFAULT_SCOPE, scopeText.getText().trim());
			if ("Custom".equals(getApiSelection()))
			{
				json.put(StatelessLoginHandler.ACCESS_TOKEN_ENDPOINT, accessTokenEndpointText.getText().trim());
				json.put(StatelessLoginHandler.AUTHORIZATION_BASE_URL, authorizationBaseUrlText.getText().trim());
				json.put(StatelessLoginHandler.JWKS_URI, jwksUriText.getText().trim());
			}
			jsonTextArea.setText(json.toString(4));
		}
		setPageComplete(isComplete);
	}

	public String getApiSelection()
	{
		return apiCombo.getText();
	}

	@Override
	public void setVisible(boolean visible)
	{
		super.setVisible(visible);
		if (visible)
		{
			this.getWizard().getContainer().getShell().pack();
		}
	}
}
