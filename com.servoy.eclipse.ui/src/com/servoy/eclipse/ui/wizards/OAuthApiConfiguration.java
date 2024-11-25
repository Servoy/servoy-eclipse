package com.servoy.eclipse.ui.wizards;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servoy.j2db.server.ngclient.OAuthUtils;

/**
 * @author emera
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuthApiConfiguration
{

	@JsonProperty("api")
	private String api;

	@JsonProperty("clientId")
	private String clientId;

	@JsonProperty("apiSecret")
	private String clientSecret;

	@JsonProperty("jwks_uri")
	private String jwksUri;

	@JsonProperty("defaultScope")
	private String scope;

	@JsonIgnore
	private String defaultTenantId;

	@JsonProperty("tenant")
	private String tenant; // nullable, only for APIs that support tenants

	private final Map<String, String> additionalParameters = new HashMap<>(); // flattened in JSON

	//nullable, only for custom api
	@JsonProperty("authorizationBaseUrl")
	private String authorizationBaseUrl;

	@JsonProperty("accessTokenEndpoint")
	private String accessTokenEndpoint;

	@JsonProperty("refreshTokenEndpoint")
	private String refreshTokenEndpoint;

	@JsonProperty("revokeTokenEndpoint")
	private String revokeTokenEndpoint;

	public String getApi()
	{
		return api;
	}

	public void setApi(String api)
	{
		this.api = api;
	}

	public String getClientId()
	{
		return clientId;
	}

	public void setClientId(String clientId)
	{
		this.clientId = clientId;
	}

	public String getClientSecret()
	{
		return clientSecret;
	}

	public void setClientSecret(String clientSecret)
	{
		this.clientSecret = clientSecret;
	}

	public String getJwksUri()
	{
		if (jwksUri == null) return jwksUri;
		String effectiveTenant = (tenant == null || tenant.trim().isEmpty()) ? defaultTenantId : tenant;
		String jwks = OAuthUtils.getJWKS_URI(this.api);
		return jwks != null && jwks.contains("{tenant}") ? jwks.replace("{tenant}", effectiveTenant) : jwksUri;
	}

	public void setJwksUri(String jwksUrl)
	{
		this.jwksUri = jwksUrl;
	}

	public String getScope()
	{
		return scope;
	}

	public void setScope(String scope)
	{
		this.scope = scope;
	}

	public void setDefaultTenant(String defaultTenant)
	{
		this.defaultTenantId = defaultTenant;
	}

	public String getTenant()
	{
		return tenant;
	}

	public void setTenant(String tenant)
	{
		this.tenant = tenant;
	}

	@JsonAnyGetter
	public Map<String, String> getAdditionalParameters()
	{
		return additionalParameters;
	}

	@JsonAnySetter
	public void setAdditionalParameter(String key, String value)
	{
		this.additionalParameters.put(key, value);
	}

	public void setAuthorizationBaseUrl(String url)
	{
		this.authorizationBaseUrl = url;
	}

	public void setAccessTokenEndpoint(String url)
	{
		this.accessTokenEndpoint = url;
	}

	public void setRefreshTokenEndpoint(String url)
	{
		this.refreshTokenEndpoint = url;
	}

	public void setRevokeTokenEndpoint(String url)
	{
		this.revokeTokenEndpoint = url;
	}

	public String getAuthorizationBaseUrl()
	{
		return authorizationBaseUrl;
	}

	public String getAccessTokenEndpoint()
	{
		return accessTokenEndpoint;
	}

	public String getRefreshTokenEndpoint()
	{
		return refreshTokenEndpoint;
	}

	public String getRevokeTokenEndpoint()
	{
		return revokeTokenEndpoint;
	}

	// JSON Conversion Methods
	public String toJson()
	{
		try
		{
			ObjectMapper objectMapper = new ObjectMapper();
			return objectMapper.writeValueAsString(this);
		}
		catch (Exception e)
		{
			throw new RuntimeException("Failed to convert to JSON", e);
		}
	}

	@JsonIgnore
	public boolean isValid()
	{
		if (isNullOrEmpty(getClientId()) || isNullOrEmpty(getClientSecret()) || isNullOrEmpty(getJwksUri()))
		{
			return false;
		}
		if (getApi() == null && (isNullOrEmpty(getAuthorizationBaseUrl()) || isNullOrEmpty(getAccessTokenEndpoint())))
		{
			return false;
		}
		return true;
	}

	private boolean isNullOrEmpty(String value)
	{
		return value == null || value.trim().isEmpty();
	}

	public static OAuthApiConfiguration fromJson(String json)
	{
		try
		{
			ObjectMapper objectMapper = new ObjectMapper();
			return objectMapper.readValue(json, OAuthApiConfiguration.class);
		}
		catch (Exception e)
		{
			throw new RuntimeException("Failed to parse JSON", e);
		}
	}

	@Override
	public String toString()
	{
		return toJson();
	}
}
