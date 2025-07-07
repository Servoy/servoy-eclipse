package com.servoy.eclipse.ui.wizards;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servoy.j2db.server.ngclient.auth.OAuthUtils.Provider;

/**
 * @author emera
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuthApiConfiguration
{
	private static Pattern placeholderPattern = Pattern.compile("\\{([^}]+)}");

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

	private final Map<String, String> additionalParameters = new HashMap<>(); // flattened in JSON

	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@JsonProperty("customParameters")
	private Map<String, String> customParameters;

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
		if (jwksUri == null) return null;

		String jwks = Provider.valueOf(this.api == null ? Provider.Custom.name() : this.api).getJwksUri();
		if (jwks != null && jwks.contains("{"))
		{
			return replacePlaceholders(jwks);
		}
		return jwksUri;
	}

	private String replacePlaceholders(String jwks)
	{
		if (customParameters == null || customParameters.isEmpty())
		{
			return jwks; // no replacements if no custom parameters
		}

		StringBuilder updatedJwks = new StringBuilder(jwks);
		Matcher matcher = placeholderPattern.matcher(jwks);

		while (matcher.find())
		{
			String placeholder = matcher.group(1);
			String replacement = customParameters.getOrDefault(placeholder, "");
			if (!replacement.isEmpty())
			{
				int start = matcher.start();
				int end = matcher.end();
				updatedJwks.replace(start, end, replacement);
				matcher = placeholderPattern.matcher(updatedJwks);
			}
		}
		return updatedJwks.toString();
	}

	public void setCustomParameter(String key, String value)
	{
		if (this.customParameters == null)
		{
			this.customParameters = new HashMap<>();
		}
		this.customParameters.put(key, value);
	}

	public Map<String, String> getCustomParameters()
	{
		if (this.customParameters == null)
		{
			this.customParameters = new HashMap<>();
		}
		return this.customParameters;
	}

	public void setCustomParameters(Map<String, String> customParameters)
	{
		this.customParameters = customParameters != null ? customParameters : new HashMap<>();
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
		if (isNullOrEmpty(getClientId()) || isNullOrEmpty(getClientSecret()) || isNullOrEmpty(getJwksUri()) || getJwksUri().contains("{"))
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
