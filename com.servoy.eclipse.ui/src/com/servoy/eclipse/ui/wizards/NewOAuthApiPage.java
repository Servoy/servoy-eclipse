package com.servoy.eclipse.ui.wizards;

import java.util.Map;
import java.util.Optional;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.servoy.j2db.server.ngclient.OAuthUtils;

/**
 * @author emera
 */
public class NewOAuthApiPage extends WizardPage
{
	private static final String CUSTOM = "Custom";
	private static final String[] items = new String[] { OAuthUtils.GOOGLE, OAuthUtils.MICROSOFT_AD, OAuthUtils.APPLE, OAuthUtils.OKTA, OAuthUtils.LINKED_IN, CUSTOM };

	private Combo apiCombo;
	private Text authorizationBaseUrlText;
	private Text accessTokenEndpointText;
	private Text refreshTokenEndpointText;
	private Text revokeTokenEndpointText;
	private Text jwksUriText;
	private Text scopeText;
	private Button offlineButton;
	private Button onlineButton;

	private final NewOAuthConfigWizard wizard;
	private Text clientIdText;
	private Text clientSecretText;

	public NewOAuthApiPage(NewOAuthConfigWizard wizard)
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
		container.setLayout(new GridLayout(2, false));

		createApiCombo(container);
		OAuthApiConfiguration apiConfiguration = wizard.getModel().get();
		clientIdText = createField(container, "Client ID:", true, value -> wizard.getModel().get().setClientId(value),
			apiConfiguration.getClientId());
		clientSecretText = createField(container, "Client Secret:", true, value -> wizard.getModel().get().setClientSecret(value),
			apiConfiguration.getClientSecret());
		scopeText = createField(container, "Scope:", true, value -> wizard.getModel().get().getScope(), apiConfiguration.getScope());
		createAccessTypeRadios(container);
		authorizationBaseUrlText = createField(container, "Authorization Base URL:", false,
			value -> wizard.getModel().get().setAuthorizationBaseUrl(value), apiConfiguration.getAuthorizationBaseUrl());
		accessTokenEndpointText = createField(container, "Access Token Endpoint:", false,
			value -> wizard.getModel().get().setAccessTokenEndpoint(value), apiConfiguration.getAccessTokenEndpoint());
		refreshTokenEndpointText = createField(container, "Refresh Token Endpoint:", false,
			value -> wizard.getModel().get().setRefreshTokenEndpoint(value), apiConfiguration.getRefreshTokenEndpoint());
		revokeTokenEndpointText = createField(container, "Revoke Token Endpoint:", false,
			value -> wizard.getModel().get().setRevokeTokenEndpoint(value), apiConfiguration.getRevokeTokenEndpoint());
		jwksUriText = createField(container, "JWKS URI:", false, value -> wizard.getModel().get().setJwksUri(value),
			apiConfiguration.getJwksUri());

		setControl(container);
		updateApiSettings();
	}

	private void createApiCombo(Composite container)
	{
		Label label = new Label(container, SWT.NONE);
		label.setText("API:");

		apiCombo = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
		apiCombo.setItems(items);

		wizard.getModel().ifPresentOrElse(model -> {
			String selectedApi = model.getApi(); // Get API from the existing model
			if (selectedApi != null && !selectedApi.isEmpty())
			{
				int index = apiCombo.indexOf(selectedApi);
				if (index != -1)
				{
					apiCombo.select(index);
				}
				else
				{
					apiCombo.select(apiCombo.indexOf(CUSTOM));
				}
			}
			else
			{
				apiCombo.select(apiCombo.indexOf(CUSTOM));
			}
		}, () -> {
			// Create a default model with the first API (Google)
			OAuthApiConfiguration defaultModel = new OAuthApiConfiguration();
			defaultModel.setApi(OAuthUtils.GOOGLE);
			defaultModel.setJwksUri(OAuthUtils.GOOGLE_JWKS);
			defaultModel.setScope("openid email");
			wizard.setModel(defaultModel); // Store the new model
			apiCombo.select(0); // Default to first item
		});

		apiCombo.addModifyListener(e -> updateApiSettings());
	}


	private Text createField(Composite container, String labelText, boolean editable, java.util.function.Consumer<String> setter,
		String initialValue)
	{
		Label label = new Label(container, SWT.NONE);
		label.setText(labelText);

		Text field = new Text(container, SWT.BORDER);
		field.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		field.setEnabled(editable);

		if (initialValue != null)
		{
			field.setText(initialValue);
		}
		if (setter != null)
		{
			field.addModifyListener(e -> setter.accept(field.getText()));
			getWizard().getContainer().updateButtons();
		}
		return field;
	}

	private void createAccessTypeRadios(Composite container)
	{
		Label label = new Label(container, SWT.NONE);
		label.setText("Refresh token (if available):");

		Composite radioContainer = new Composite(container, SWT.NONE);
		radioContainer.setLayout(new GridLayout(2, true));

		offlineButton = new Button(radioContainer, SWT.RADIO);
		offlineButton.setText("Yes");
		boolean refresh = refreshToken();
		offlineButton.setSelection(refresh);
		offlineButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				updateGetRefreshToken();
			}
		});

		onlineButton = new Button(radioContainer, SWT.RADIO);
		onlineButton.setText("No");
		onlineButton.setSelection(!refresh);
		onlineButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				updateGetRefreshToken();
			}
		});
	}

	private boolean refreshToken()
	{
		String provider = apiCombo.getText();
		if (OAuthUtils.GOOGLE.equals(provider) || OAuthUtils.MICROSOFT_AD.equals(provider))
		{
			Map<String, String> additionalParameters = wizard.getModel().get().getAdditionalParameters();
			return "offline".equals(additionalParameters.get("access_type"));
		}
		if (OAuthUtils.APPLE.equals(provider) || CUSTOM.equals(provider) && wizard.getModel().get().getRefreshTokenEndpoint() != null)
		{
			//Apple always returns the refresh token together with the access token
			return true;
		}
		return false;
	}

	private void updateGetRefreshToken()
	{
		String provider = apiCombo.getText();
		//Google and Microsoft need the access_type parameter offline to return the refresh token
		if (OAuthUtils.GOOGLE.equals(provider) || OAuthUtils.MICROSOFT_AD.equals(provider))
		{
			if (onlineButton.getSelection())
			{
				wizard.getModel().get().setAdditionalParameter("access_type", "online");
			}
			else
			{
				wizard.getModel().get().setAdditionalParameter("access_type", "offline");
			}
		}
	}

	private void updateApiSettings()
	{
		String provider = apiCombo.getText();
		OAuthApiConfiguration model = wizard.getModel().orElseGet(() -> {
			OAuthApiConfiguration newModel = new OAuthApiConfiguration();
			wizard.setModel(newModel);
			return newModel;
		});

		if (jwksUriText == null)
		{
			// prevent execution if fields are not initialized
			return;
		}

		boolean isCustomSelected = CUSTOM.equals(provider);
		authorizationBaseUrlText.setEnabled(isCustomSelected);
		accessTokenEndpointText.setEnabled(isCustomSelected);
		refreshTokenEndpointText.setEnabled(isCustomSelected);
		revokeTokenEndpointText.setEnabled(isCustomSelected);
		jwksUriText.setEnabled(isCustomSelected);

		if (model.getApi() == null && !isCustomSelected)
		{
			clearFieldsForCustomApi(model);
		}
		boolean isAPIChanged = !provider.equals(model.getApi()) || CUSTOM.equals(provider) && model.getApi() != null;

		if (isAPIChanged)
		{
			String clientId = clientIdText.getText().trim();
			String clientSecret = clientSecretText.getText().trim();

			model = new OAuthApiConfiguration();
			model.setClientId(clientId);
			model.setClientSecret(clientSecret);

			switch (provider)
			{
				case OAuthUtils.GOOGLE :
					model.setApi(OAuthUtils.GOOGLE);
					model.setJwksUri(OAuthUtils.GOOGLE_JWKS);
					model.setScope("openid email");
					break;

				case "Microsoft" :
				case OAuthUtils.MICROSOFT_AD :
					model.setApi(OAuthUtils.MICROSOFT_AD);
					model.setJwksUri(OAuthUtils.MICROSOFT_JWKS);
					model.setScope("openid email");
					model.setCustomParameter("tenant", "common");
					break;

				case OAuthUtils.APPLE :
					model.setApi(OAuthUtils.APPLE);
					model.setJwksUri(OAuthUtils.APPLE_JWKS);
					model.setScope("name email");
					model.setAdditionalParameter("response_mode", "form_post");
					break;

				case OAuthUtils.OKTA :
					model.setApi(OAuthUtils.OKTA);
					model.setJwksUri(OAuthUtils.OKTA_JWKS);
					model.setScope("openid profile email");
					model.setCustomParameter("domain", "");
					break;

				case OAuthUtils.LINKED_IN :
					model.setApi(OAuthUtils.LINKED_IN);
					model.setJwksUri(OAuthUtils.LINKEDIN_JWKS);
					model.setScope("openid email");
					break;

				case CUSTOM :
					model.setApi(null);
					model.setJwksUri("");
					break;

				default :
					throw new IllegalArgumentException("Unsupported API provider: " + provider);
			}
		}

		scopeText.setText(model.getScope() != null ? model.getScope() : "");
		jwksUriText.setText(model.getJwksUri() != null ? model.getJwksUri() : "");
		wizard.setModel(model);
		updateGetRefreshToken();
		wizard.getContainer().updateButtons();
	}

	private void clearFieldsForCustomApi(OAuthApiConfiguration model)
	{
		model.setApi(null);
		model.setJwksUri(null);
		model.setAuthorizationBaseUrl(null);
		model.setAccessTokenEndpoint(null);
		model.setRefreshTokenEndpoint(null);
		model.setRevokeTokenEndpoint(null);
		jwksUriText.setText("");
		authorizationBaseUrlText.setText("");
		accessTokenEndpointText.setText("");
		refreshTokenEndpointText.setText("");
		revokeTokenEndpointText.setText("");
	}

	@Override
	public void setVisible(boolean visible)
	{
		super.setVisible(visible);
		if (visible)
		{
			Optional<OAuthApiConfiguration> optionalModel = wizard.getModel();
			if (optionalModel.isPresent())
			{
				OAuthApiConfiguration model = optionalModel.get();
				clientIdText.setText(model.getClientId() != null ? model.getClientId() : "");
				clientSecretText.setText(model.getClientSecret() != null ? model.getClientSecret() : "");
				scopeText.setText(model.getScope() != null ? model.getScope() : "");
				apiCombo.setText(model.getApi() != null ? model.getApi() : CUSTOM);
				jwksUriText.setText(model.getJwksUri() != null ? model.getJwksUri() : "");

				if (model.getApi() == null) //custom
				{
					authorizationBaseUrlText.setText(
						model.getAuthorizationBaseUrl() != null ? model.getAuthorizationBaseUrl() : "");
					accessTokenEndpointText.setText(
						model.getAccessTokenEndpoint() != null ? model.getAccessTokenEndpoint() : "");
					refreshTokenEndpointText.setText(
						model.getRefreshTokenEndpoint() != null ? model.getRefreshTokenEndpoint() : "");
					revokeTokenEndpointText.setText(
						model.getRevokeTokenEndpoint() != null ? model.getRevokeTokenEndpoint() : "");
				}
			}
			else
			{
				apiCombo.select(0);
			}
		}
	}
}
