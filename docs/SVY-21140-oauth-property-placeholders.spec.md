# Spec: SVY-21140 — Support property placeholders (%%key%%) in OAuth config

## 1. Goal

Allow `clientId` and `apiSecret` (and any other string field) in the OAuth JSON
config stored in `solution_settings.obj` to contain `%%propertyName%%`
placeholders instead of literal secret values. At runtime, before constructing
the OAuth service, the placeholders are resolved via
`ApplicationServerRegistry.get().getServerAccess().getSettings()` — the same
`servoy.properties` / admin-panel property store already used throughout the
server. This keeps secrets out of version control without requiring a separate
authenticator module.

---

## 2. Background

### 2.1 Current OAuth config storage

`NewOAuthConfigWizard.performFinish()` serialises an `OAuthApiConfiguration`
POJO to a JSON string and stores it as a solution custom property under the key
`StatelessLoginHandler.OAUTH_CUSTOM_PROPERTIES` (`"oauth"`):

```json
{
  "api": "Microsoft",
  "clientId": "my-actual-client-id",
  "apiSecret": "my-actual-secret",
  "jwks_uri": "https://login.microsoftonline.com/common/discovery/v2.0/keys",
  "defaultScope": "openid email profile"
}
```

This string ends up in `solution_settings.obj` under `customProperties.oauth`
and is committed to version control alongside the solution.

### 2.2 Runtime consumption

`OAuthUtils.createOauthService(JSONObject auth, ...)` (in `servoy_ngclient`)
reads `clientId` and `apiSecret` directly from the `JSONObject` as raw strings.
There is currently no placeholder resolution anywhere in that path.

### 2.3 Existing `%%key%%` convention

The `%%key%%` syntax is already used in `servoy.properties` for native launcher
paths (e.g. `%%user.dir%%`). Reusing the same delimiters keeps the convention
consistent and avoids introducing a new syntax.

### 2.4 Settings access

Server properties are accessed at runtime via:
```java
Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
String value = settings.getProperty(key);
```
`getSettings()` returns the live `java.util.Properties` backed by
`servoy.properties` and the admin panel — the same source as
`application.getProperty(key)` in Servoy scripting.

---

## 3. Design

### 3.1 Placeholder resolution utility — `OAuthPropertyResolver`

New package-private class in `com.servoy.j2db.server.ngclient.auth`
(i.e. in the `servoy_ngclient` module, alongside `OAuthUtils`).

```java
/**
 * Resolves %%key%% placeholders in OAuth config JSON string values
 * using server properties.
 */
class OAuthPropertyResolver {

    static final Pattern PLACEHOLDER = Pattern.compile("%%([^%]+)%%");

    /**
     * Returns a copy of {@code json} with all %%key%% placeholders in
     * string values replaced by the corresponding server property.
     * Keys with no matching property are left as-is (not replaced).
     */
    static JSONObject resolve(JSONObject json, Properties settings) {
        JSONObject result = new JSONObject(json.toString());
        for (String key : result.keySet()) {
            Object val = result.get(key);
            if (val instanceof String) {
                result.put(key, resolvePlaceholders((String) val, settings));
            }
        }
        return result;
    }

    static String resolvePlaceholders(String value, Properties settings) {
        Matcher m = PLACEHOLDER.matcher(value);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String propKey = m.group(1);
            String propVal = settings.getProperty(propKey);
            m.appendReplacement(sb, propVal != null ? Matcher.quoteReplacement(propVal) : m.group(0));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
```

**Behaviour:**
- Only top-level string values are scanned (sufficient for `clientId`,
  `apiSecret`, `jwks_uri`, `defaultScope`, etc.).
- If a placeholder key is not found in `settings`, the `%%key%%` token is left
  unchanged so the error surfaces clearly at OAuth service construction time.
- The original `JSONObject` is not mutated; a copy is returned.

### 3.2 `OAuthUtils.createOauthService` — resolve before building

In `OAuthUtils.createOauthService(JSONObject auth, Map<String,String> additionalParameters, String serverURL)`:

```java
// Resolve %%key%% placeholders before reading any field values
Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
auth = OAuthPropertyResolver.resolve(auth, settings);
```

This single line is inserted at the top of the method, before the `ServiceBuilder`
is constructed. All subsequent field reads (`clientId`, `apiSecret`, etc.) then
operate on the resolved copy.

### 3.3 Wizard change — `NewOAuthApiPage` — inline `%%` syntax, no checkbox

The **Client ID** and **Client Secret** fields accept either a literal value or a
`%%propertyName%%` string typed directly by the developer. No checkbox or mode
switching is added — the field is always a plain text input.

The wizard detects the `%%...%%` pattern automatically:

- `isPageComplete()` / `canFinish()` accepts any non-empty string in these fields,
  whether it is a literal credential or a `%%propertyName%%` reference.
- `performFinish()` calls `OAuthPropertyResolver.containsPlaceholder(json)` to
  decide whether to run live `createOauthService` validation or the
  missing-property warning flow (§3.4).

**No changes to `NewOAuthApiPage` field layout or labels.** The fields remain
exactly as they were before this ticket. The only wizard change is in
`performFinish()` (§3.4).

### 3.4 Wizard validation — placeholder configs: resolve against workspace settings, warn on missing keys

`NewOAuthConfigWizard.performFinish()` currently calls
`OAuthUtils.createOauthService(json, ...)` to validate the config before saving.
When any field contains a `%%...%%` placeholder, this call will fail because the
placeholder is not a real credential.

**Change:** Before calling `createOauthService`, check whether any string value
in the JSON contains a placeholder. If placeholders are present:

1. Attempt to resolve them against the current workspace's `servoy.properties`
   via `ApplicationServerRegistry.get().getServerAccess().getSettings()`.
2. Collect the names of any placeholder keys that have no matching property.
3. If there are unresolved keys, show a **warning dialog** (not a blocking error)
   listing the missing property names and telling the user where to set them:

```
The following properties are not yet set in servoy.properties:

  oauth.microsoft.clientId
  oauth.microsoft.apiSecret

Set them in the Servoy Admin Panel under 'Properties', or add them directly
to servoy.properties. The config will be saved and will work once the
properties are configured on the server.
```

   The user can choose **Save anyway** or **Cancel**. Choosing Save anyway
   proceeds to store the config; Cancel returns to the wizard.

4. If all placeholders resolve successfully, skip the live `createOauthService`
   call (the resolved values may still be invalid credentials, but that is a
   runtime concern) and save directly.

5. If no placeholders are present, run the existing `createOauthService`
   validation as before.

```java
boolean hasPlaceholders = OAuthPropertyResolver.containsPlaceholder(json);
if (hasPlaceholders) {
    Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
    List<String> missing = OAuthPropertyResolver.findUnresolved(json, settings);
    if (!missing.isEmpty()) {
        String msg = "The following properties are not yet set in servoy.properties:\n\n  " +
            String.join("\n  ", missing) +
            "\n\nSet them in the Servoy Admin Panel under 'Properties', or add them " +
            "directly to servoy.properties.\nThe config will be saved and will work " +
            "once the properties are configured on the server.";
        boolean proceed = MessageDialog.openQuestion(getShell(), "Missing properties", msg);
        if (!proceed) return false;
    }
    // placeholders present and user confirmed — save without live validation
} else {
    Object service = OAuthUtils.createOauthService(json, new HashMap<>(), "http://");
    if (service == null) {
        setErrorMessage("Could not create OAuth service. Check your configuration.");
        return false;
    }
}
editingSolution.putCustomProperty(
    new String[] { StatelessLoginHandler.OAUTH_CUSTOM_PROPERTIES },
    json.toString());
```

Add the following helpers to `OAuthPropertyResolver`:

```java
static boolean containsPlaceholder(JSONObject json) {
    for (String key : json.keySet()) {
        Object val = json.get(key);
        if (val instanceof String && PLACEHOLDER.matcher((String) val).find()) return true;
    }
    return false;
}

/** Returns the property key names referenced by %%key%% tokens that have no
 *  matching entry in {@code settings}. */
static List<String> findUnresolved(JSONObject json, Properties settings) {
    List<String> missing = new ArrayList<>();
    for (String key : json.keySet()) {
        Object val = json.get(key);
        if (val instanceof String) {
            Matcher m = PLACEHOLDER.matcher((String) val);
            while (m.find()) {
                String propKey = m.group(1);
                if (settings.getProperty(propKey) == null && !missing.contains(propKey))
                    missing.add(propKey);
            }
        }
    }
    return missing;
}
```

---

## 4. Example

Developer stores in `solution_settings.obj`:

```json
{
  "api": "Microsoft",
  "clientId": "%%oauth.microsoft.clientId%%",
  "apiSecret": "%%oauth.microsoft.apiSecret%%",
  "jwks_uri": "https://login.microsoftonline.com/common/discovery/v2.0/keys",
  "defaultScope": "openid email profile"
}
```

In `servoy.properties` (or admin panel) on each server:

```
oauth.microsoft.clientId=<real-client-id>
oauth.microsoft.apiSecret=<real-secret>
```

At runtime, `OAuthPropertyResolver.resolve()` substitutes the values before
`OAuthUtils` constructs the OAuth service. The solution repository never contains
the real secrets.

---

## 5. Implementation plan

1. **`OAuthPropertyResolver.java`** (`com.servoy.j2db.server.ngclient.auth` in `servoy_ngclient`):
   - New class with `PLACEHOLDER` pattern, `resolve(JSONObject, Properties)`,
     `resolvePlaceholders(String, Properties)`, `containsPlaceholder(JSONObject)`,
     `findUnresolved(JSONObject, Properties)`.

2. **`OAuthUtils.java`** (`com.servoy.j2db.server.ngclient.auth` in `servoy_ngclient`):
   - In `createOauthService(JSONObject, Map, String)`: resolve placeholders at
     the top of the method before any field reads (§3.2).

3. **`NewOAuthApiPage.java`** (`com.servoy.eclipse.ui`):
   - No layout changes. The Client ID and Client Secret fields remain plain text
     inputs that accept either a literal value or a `%%propertyName%%` string
     typed directly by the developer (§3.3).

4. **`NewOAuthConfigWizard.java`** (`com.servoy.eclipse.ui`):
   - In `performFinish()`: when placeholders are present, resolve against
     workspace settings, collect unresolved keys, show informational dialog
     listing the missing keys (copyable) and where to configure them on the
     admin page, then save directly (skip live `createOauthService` validation).
   - `canFinish()`: block Finish when clientId or clientSecret is empty.

5. **`ConfigServlet.java`** (`com.servoy.j2db.server.servlets` in `j2db_server`):
   - Add `oauth.properties` textarea on the Servoy Server Home page (same
     pattern as `system.properties` / `user.properties`).
   - Add `placeOAuthPropertiesInSettings()` to parse and persist `oauth.*` keys.
   - Existing `oauth.*` keys are removed before re-saving so deleted lines
     take effect.

6. **`OAuthPropertyResolverTest.java`** (`servoy_ngclient` tests):
   - Unit tests covering the cases in §7.

---

## 6. Acceptance criteria

- [ ] A `clientId` value of `%%oauth.microsoft.clientId%%` stored in
      `solution_settings.obj` is resolved to the matching `servoy.properties`
      value before the OAuth service is constructed.
- [ ] A `apiSecret` value of `%%oauth.microsoft.apiSecret%%` is resolved
      similarly.
- [ ] All other string fields in the OAuth JSON support the same substitution.
- [ ] If a placeholder key has no matching property, the `%%key%%` token is left
      as-is (not silently replaced with empty string), causing a clear failure at
      OAuth service construction.
- [ ] Non-placeholder configs (literal values) continue to work unchanged.
- [ ] The developer can type `%%propertyName%%` directly into the Client ID or
      Client Secret field; the value is stored as-is in `solution_settings.obj`.
- [ ] `canFinish()` returns false when Client ID or Client Secret is empty.
- [ ] When saving a placeholder config, `performFinish()` shows an informational
      dialog listing unresolved property keys (copyable) and telling the user to
      configure them on the admin page under 'oauth.properties'.
- [ ] `performFinish()` saves placeholder configs directly without calling
      `createOauthService`.
- [ ] `performFinish()` still validates non-placeholder configs via
      `createOauthService` as before.
- [ ] The Servoy Admin Panel home page has an `oauth.properties` textarea that
      persists `oauth.*` key-value pairs to `servoy.properties` via `settings.save()`.
- [ ] OAuth properties survive server restart.
- [ ] At runtime, `OAuthUtils.createOauthService` logs an error via the
      `stateless.login` logger when placeholders cannot be resolved.
- [ ] No compilation errors in `servoy_ngclient` or `com.servoy.eclipse.ui`.
- [ ] All test cases in `OAuthPropertyResolverTest` pass (see §7).

---

## 7. Test cases

`OAuthPropertyResolverTest` must cover:

| # | Test | What is asserted |
|---|------|-----------------|
| 1 | `resolve_singlePlaceholder_replaced` | `%%key%%` in `clientId` is replaced with the matching property value |
| 2 | `resolve_multiplePlaceholders_allReplaced` | Both `clientId` and `apiSecret` placeholders are resolved in one call |
| 3 | `resolve_unknownKey_leftAsIs` | A `%%missing.key%%` with no matching property is left unchanged in the output |
| 4 | `resolve_noPlaceholders_unchanged` | A JSON with no `%%...%%` tokens is returned byte-for-byte identical |
| 5 | `resolve_doesNotMutateInput` | The original `JSONObject` passed to `resolve()` is not modified |
| 6 | `containsPlaceholder_true` | Returns `true` when any string value contains `%%...%%` |
| 7 | `containsPlaceholder_false` | Returns `false` for a JSON with only literal values |
| 9 | `findUnresolved_missingKeys_returned` | Returns the property key names for all `%%key%%` tokens with no matching entry in the supplied `Properties` |
| 10 | `findUnresolved_allResolved_emptyList` | Returns an empty list when all placeholder keys have matching properties |

---

## 8. Out of scope

- Nested JSON object values (e.g. `customParameters` map entries) — only
  top-level string values are resolved in this ticket.
- Encrypting or obfuscating the property values in `servoy.properties`.
- Placeholder support for non-OAuth authenticator types.

---

## 9. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should unresolved placeholders (missing property key) fail silently (leave `%%key%%` as-is) or throw an explicit exception with a clear message? Leaving as-is surfaces the error at OAuth service construction; throwing earlier gives a better error message. | Developer | resolved — left as-is; log.error message explains the problem and where to configure |
| The wizard currently validates the config by constructing a live OAuth service. For placeholder configs this is skipped entirely. Should a dry-run validation be added (e.g. check that the property keys exist in the current workspace's `servoy.properties`)? | Product | resolved — wizard checks and shows informational dialog listing missing keys with copy support |
