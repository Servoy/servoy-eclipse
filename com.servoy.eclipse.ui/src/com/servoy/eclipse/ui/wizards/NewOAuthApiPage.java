package com.servoy.eclipse.ui.wizards;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.json.JSONObject;

import com.servoy.j2db.server.ngclient.StatelessLoginHandler;

/**
 *
 * @author emera
 */
public class NewOAuthApiPage extends WizardPage
{
	private static final String MICROSOFT_JWKS = "https://login.microsoftonline.com/common/discovery/v2.0/keys";
	private static final String GOOGLE_JWKS = "https://www.googleapis.com/oauth2/v3/certs";
	private static final String[] items = new String[] { "Google", "Microsoft", "Custom" };
	private Combo apiCombo;
	private Text clientIdText;
	private Text clientSecretText;
	private Button onlineAccessType;
	private Button offlineAccessType;
	private Text scopeText;
	private Text jwksUriText;
	private Text authorizationBaseUrlText;
	private Text accessTokenEndpointText;
	private final NewOAuthConfigWizard wizard;

	protected NewOAuthApiPage(NewOAuthConfigWizard wizard)
	{
		super("OAuth Configuration");
		setTitle("OAuth Configuration Wizard");
		setDescription("Setup the OAuth configuration for stateless login");
		this.wizard = wizard;
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
		apiCombo.setItems(items);
		apiCombo.addModifyListener(e -> updateApiSettings());

		Label clientIdLabel = new Label(container, SWT.NONE);
		clientIdLabel.setText("Client ID:");
		clientIdText = new Text(container, SWT.BORDER);
		clientIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		clientIdText.addListener(SWT.FocusOut, e -> updateClientId());
		clientIdText.addModifyListener(e -> updateClientId());

		Label clientSecretLabel = new Label(container, SWT.NONE);
		clientSecretLabel.setText("Client Secret:");
		clientSecretText = new Text(container, SWT.BORDER);
		clientSecretText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		clientSecretText.addListener(SWT.FocusOut, e -> updateClientSecret());
		clientSecretText.addModifyListener(e -> updateClientSecret());

		Label accessTypeLabel = new Label(container, SWT.NONE);
		accessTypeLabel.setText("Refresh token (if available):");
		Composite accessTypeGroup = new Composite(container, SWT.NONE);
		accessTypeGroup.setLayout(new RowLayout(SWT.HORIZONTAL));
		offlineAccessType = new Button(accessTypeGroup, SWT.RADIO);
		offlineAccessType.setText("Yes");
		offlineAccessType.setSelection(true);
		offlineAccessType.addListener(SWT.DefaultSelection, e -> updateGetRefreshToken());

		onlineAccessType = new Button(accessTypeGroup, SWT.RADIO);
		onlineAccessType.setText("No");
		onlineAccessType.addListener(SWT.DefaultSelection, e -> updateGetRefreshToken());

		Label scopeLabel = new Label(container, SWT.NONE);
		scopeLabel.setText("Default scope:");
		scopeText = new Text(container, SWT.BORDER);
		scopeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		scopeText.addListener(SWT.FocusOut, e -> updateScope());
		scopeText.addModifyListener(e -> updateScope());

		int indexOfCustom = Arrays.asList(items).indexOf("Custom");
		Label authorizationBaseUrlLabel = new Label(container, SWT.NONE);
		authorizationBaseUrlLabel.setText("Authorization Base URL:");
		authorizationBaseUrlText = new Text(container, SWT.BORDER);
		authorizationBaseUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		authorizationBaseUrlText.addListener(SWT.FocusOut, e -> updateAuthorizationBaseUrl());
		authorizationBaseUrlText.addModifyListener(e -> updateAuthorizationBaseUrl());
		authorizationBaseUrlText.setEnabled(apiCombo.getSelectionIndex() == indexOfCustom);

		Label accessTokenEndpointLabel = new Label(container, SWT.NONE);
		accessTokenEndpointLabel.setText("Access Token Endpoint:");
		accessTokenEndpointText = new Text(container, SWT.BORDER);
		accessTokenEndpointText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		accessTokenEndpointText.addListener(SWT.FocusOut, e -> updateAccessTokenEndpoint());
		accessTokenEndpointText.addModifyListener(e -> updateAccessTokenEndpoint());
		accessTokenEndpointText.setEnabled(apiCombo.getSelectionIndex() == indexOfCustom);

		Label jwksUriLabel = new Label(container, SWT.NONE);
		jwksUriLabel.setText("JWKS URI:");
		jwksUriText = new Text(container, SWT.BORDER);
		jwksUriText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		jwksUriText.addListener(SWT.FocusOut, e -> updateJWKS_URI());
		jwksUriText.addModifyListener(e -> updateJWKS_URI());
		jwksUriText.setEnabled(apiCombo.getSelectionIndex() == indexOfCustom);
		jwksUriText.setToolTipText("The JSON Web Key Set (JWKS) is a set of keys containing the public keys " +
			" used to verify the id token issued by the Authorization Server.");

		setControl(container);
		setPageComplete(isPageComplete());
	}

	private void updateScope()
	{
		wizard.getJSON().put(StatelessLoginHandler.DEFAULT_SCOPE, scopeText.getText());
		wizard.getContainer().updateButtons();
	}

	private void updateClientId()
	{
		wizard.getJSON().put(StatelessLoginHandler.CLIENT_ID, clientIdText.getText());
		wizard.getContainer().updateButtons();
	}

	private void updateClientSecret()
	{
		wizard.getJSON().put(StatelessLoginHandler.API_SECRET, clientSecretText.getText());
		wizard.getContainer().updateButtons();
	}

	private void updateJWKS_URI()
	{
		wizard.getJSON().put(StatelessLoginHandler.JWKS_URI, jwksUriText.getText());
		wizard.getContainer().updateButtons();
	}

	private void updateAuthorizationBaseUrl()
	{
		wizard.getJSON().put(StatelessLoginHandler.AUTHORIZATION_BASE_URL, authorizationBaseUrlText.getText());
		wizard.getContainer().updateButtons();
	}

	private void updateAccessTokenEndpoint()
	{
		wizard.getJSON().put(StatelessLoginHandler.ACCESS_TOKEN_ENDPOINT, accessTokenEndpointText.getText());
		wizard.getContainer().updateButtons();
	}

	private void updateGetRefreshToken()
	{
		String provider = apiCombo.getText();
		if ("Google".equals(provider) || "Microsoft".equals(provider))
		{
			if (offlineAccessType.getSelection())
			{
				wizard.getJSON().put("access_type", "offline");
			}
			else
			{
				wizard.getJSON().put("access_type", "online");
			}
		}
	}

	private void updateApiSettings()
	{
		String provider = apiCombo.getText();
		JSONObject oauthJson = wizard.getJSON();

		if (!"Custom".equals(provider) && oauthJson.has(StatelessLoginHandler.AUTHORIZATION_BASE_URL))
		{
			oauthJson.remove(StatelessLoginHandler.AUTHORIZATION_BASE_URL);
			oauthJson.remove(StatelessLoginHandler.ACCESS_TOKEN_ENDPOINT);
		}
		switch (provider)
		{
			case "Google" :
				oauthJson.put(StatelessLoginHandler.OAUTH_API, "Google");
				oauthJson.put(StatelessLoginHandler.JWKS_URI, GOOGLE_JWKS);
				oauthJson.remove("tenant");
				break;
			case "Microsoft" :
				oauthJson.put(StatelessLoginHandler.OAUTH_API, "Microsoft");
				oauthJson.put(StatelessLoginHandler.JWKS_URI, MICROSOFT_JWKS);
				oauthJson.put("tenant", "");
				break;
			case "Custom" :
				oauthJson.remove(StatelessLoginHandler.OAUTH_API);
				oauthJson.put(StatelessLoginHandler.JWKS_URI, "");
				break;
		}
		authorizationBaseUrlText.setEnabled("Custom".equals(provider));
		accessTokenEndpointText.setEnabled("Custom".equals(provider));
		jwksUriText.setEnabled("Custom".equals(provider));

		getWizard().getContainer().updateButtons();
	}

	@Override
	public boolean canFlipToNextPage()
	{
		boolean isComplete = !clientIdText.getText().isEmpty() && !clientSecretText.getText().isEmpty();
		if (isComplete)
		{
			JSONObject json = ((NewOAuthConfigWizard)getWizard()).getJSON();
			json.put(StatelessLoginHandler.CLIENT_ID, clientIdText.getText().trim());
			json.put(StatelessLoginHandler.API_SECRET, clientSecretText.getText().trim());
			json.put(StatelessLoginHandler.DEFAULT_SCOPE, scopeText.getText().trim());
			if ("Custom".equals(getApiSelection()))
			{
				json.put(StatelessLoginHandler.ACCESS_TOKEN_ENDPOINT, accessTokenEndpointText.getText().trim());
				json.put(StatelessLoginHandler.AUTHORIZATION_BASE_URL, authorizationBaseUrlText.getText().trim());
				json.put(StatelessLoginHandler.JWKS_URI, jwksUriText.getText().trim());
			}
		}
		return isComplete;
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
			initValues();
		}
	}

	private void initValues()
	{
		JSONObject oauthJson = wizard.getJSON();
		String selected = oauthJson.optString(StatelessLoginHandler.OAUTH_API, "Custom");
		apiCombo.select(IntStream.range(0, items.length).filter(i -> items[i].equals(selected)).findFirst().orElse(-1));
		clientIdText.setText(oauthJson.optString(StatelessLoginHandler.CLIENT_ID, ""));
		clientSecretText.setText(oauthJson.optString(StatelessLoginHandler.API_SECRET, ""));
		if (oauthJson.has(StatelessLoginHandler.DEFAULT_SCOPE) && !"".equals(oauthJson.get(StatelessLoginHandler.DEFAULT_SCOPE)))
		{
			scopeText.setText(oauthJson.getString(StatelessLoginHandler.DEFAULT_SCOPE));
		}
		else
		{
			scopeText.setText("openid email");
		}
		if ("Custom".equals(selected))
		{
			accessTokenEndpointText.setText(oauthJson.optString(StatelessLoginHandler.ACCESS_TOKEN_ENDPOINT, ""));
			authorizationBaseUrlText.setText(oauthJson.optString(StatelessLoginHandler.AUTHORIZATION_BASE_URL, ""));
		}
		jwksUriText.setText(oauthJson.optString(StatelessLoginHandler.JWKS_URI, ""));
		authorizationBaseUrlText.setEnabled("Custom".equals(selected));
		accessTokenEndpointText.setEnabled("Custom".equals(selected));
		jwksUriText.setEnabled("Custom".equals(selected));

	}
}
