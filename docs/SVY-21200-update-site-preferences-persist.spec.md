# Spec: SVY-21200 â Servoy IDE update site preferences does not persist between updates

## 1. Goal

Preserve the user's enabled/disabled state of p2 update site repositories across product updates. Currently, when the IDE is updated via nightly or RC builds, repositories that the user disabled get re-enabled.

## 2. Root Cause Analysis

### 2.1 Product repository definitions

`com.servoy.eclipse.product/servoy.product` (lines 129â138) declares 8 update site repositories:

| Repository | Default enabled |
|---|---|
| Servoy Latest Releases | true |
| Servoy LTS Releases | false |
| Servoy Latest Release Candidates | true |
| SQLExplorer | true |
| Eclipse Release | true |
| Eclipse Updates | true |
| Nightly Build | true |
| Servoy AI feature | true |

### 2.2 The actual mechanism causing re-enabling

The root cause is an **Eclipse p2 platform bug**. No Servoy-specific code is involved.

**The flow:**

1. **Build time:** `tycho-p2-publisher-plugin:publish-products` reads the `.product` file and embeds the `<repositories>` entries as **repository references** into the published update site's `content.xml`/`content.jar` metadata. Each reference carries the `enabled` flag from the product file.

2. **Update time:** When the user performs "Check for Updates" or installs from the Servoy update site, the p2 engine loads the update site's `content.xml`, discovers the embedded repository references, and **re-applies their `enabled` state** to the user's local repository list â overriding any user customization.

### 2.3 Eclipse p2 code responsible

The exact code path in Eclipse p2:

1. **`LocalMetadataRepository.publishRepositoryReferences()`** â fires a `RepositoryEvent.DISCOVERED` event for every `<reference>` in content.xml, **every time** the repository is loaded (not just the first time). The `enabled` flag comes directly from content.xml with no check against existing user preferences.

2. **`AbstractRepositoryManager.notify()`** â handles the DISCOVERED event, creates a `RepositoryInfo` with `isEnabled` from the event, and calls `addRepository(info, true)`.

3. **`AbstractRepositoryManager.addRepository(RepositoryInfo, boolean)`** â has a `contains()` check that should prevent overwriting existing repos. **But this fails when:**
   - The **configuration area changes** during a product update (preferences become empty, all repos are treated as new and added with `enabled=true` from content.xml)
   - **URI canonicalization mismatch** (trailing slash, encoding) â `getKey(URI)` doesn't match

4. **`AbstractRepositoryManager.addRepository(URI)`** (public API) â unconditionally re-enables already-known repos: `if (!addRepository(location, true, true)) { setEnabled(location, true); }`

### 2.4 Verification: No Servoy code is involved

| Checked | Result |
|---------|--------|
| Servoy Java code using `IMetadataRepositoryManager` | None |
| Servoy Java code using `IArtifactRepositoryManager` | None |
| Servoy Java code calling `setEnabled` on p2 repos | None |
| `p2.inf` with `addRepository` touchpoint instructions | None |
| Startup code manipulating repositories | None |
| `org.eclipse.equinox.p2.reconciler` references | None in Servoy code |

## 3. Resolution

This is an Eclipse platform bug. The fix belongs in Eclipse p2, specifically in `AbstractRepositoryManager`: when processing a `DISCOVERED` event for a repository URI that is **already known** to the manager (even if currently disabled), it must **not** override the user's stored enabled state. Only genuinely new (never-before-seen) repositories should have their enabled flag applied from the reference.

**Eclipse bug:** https://github.com/eclipse-equinox/p2/issues/1097

**No change is needed in Servoy code.**

## 4. Bug Report Details

**Title:** `Repository references from content.xml override user-disabled state on update`

**Reproduction:**
1. Install an Eclipse RCP product that defines `<repositories>` in its `.product` file with `enabled="true"`
2. Disable one of those repositories via Window > Preferences > Install/Update > Available Software Sites
3. Update the product from the update site
4. Observe the disabled repository is re-enabled

**Expected behavior:** Already-known repositories should retain their user-configured enabled/disabled state when repository references are discovered from content.xml.

**Actual behavior:** The enabled state from content.xml unconditionally overrides the user's preference, particularly after configuration area changes during product updates.

**Affected components:**
- `org.eclipse.equinox.p2.metadata.repository` â `LocalMetadataRepository.publishRepositoryReferences()`
- `org.eclipse.equinox.p2.repository` â `AbstractRepositoryManager.notify()`, `AbstractRepositoryManager.addRepository()`
