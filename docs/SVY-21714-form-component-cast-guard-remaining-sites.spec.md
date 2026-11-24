# Spec: SVY-21714 — Guard the form-component value cast at the nine remaining sites

> **This change spans TWO git repositories, both checked out under
> `C:\Users\Cristian\git\servoy\master\`.** Three sites live in **`servoy-client`**
> (`servoy_ngclient`), six in **`servoy-eclipse`** (the Developer IDE). The shared guard
> helper `FormComponentPropertyType.asFormComponentValue(...)` already exists and is shipped
> (SVY-21667) in `servoy-client`'s `servoy_ngclient`; this spec only *calls* it at nine more
> sites. **No MANIFEST / pom / Export-Package change is expected** — `com.servoy.eclipse.*`
> plugins already resolve `com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType`
> from the `servoy_ngclient` workspace project (confirmed in the SVY-21667 spec §3.3d). Two
> separate commits are required: one in `servoy-client`, one in `servoy-eclipse`.

## 1. Goal

A form-component property (e.g. a `servoycore-listformcomponent`'s `containedForm`) whose
value is written as a bare form-name/UUID **string** instead of the canonical
`{ "svy_form": "<UUID>" }` object still throws a raw
`java.lang.ClassCastException: String cannot be cast to JSONObject` at nine sites that
SVY-21667 left unguarded — three in the NG client (`servoy-client`) and six in the Developer
IDE (`servoy-eclipse`). The goal is to apply the **exact** SVY-21667 guard at each of the
nine sites: call `FormComponentPropertyType.asFormComponentValue(value, formName, elementName,
propertyName)` before every `(JSONObject)` cast on a form-component value, and `continue`/skip
that property when it returns `null`, so the value is rejected-and-reported (one clear
`Debug.error` naming the form/element/property) instead of aborting the whole render or build.
This is a pass-through for valid `JSONObject` values — no behaviour change for well-formed
data — and no coercion of the string into the object form.

## 2. Background

### 2.1 The defect shape (identical at all nine sites)

Every site follows the shape SVY-21667 already fixed elsewhere:

```java
Object propertyValue = fe.getPropertyValue(pd.getName());
Form frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, fs); // tolerant lookup
if (frm == null) continue;                                                // null guard
...
getFormComponentCache(fe, pd, (JSONObject)propertyValue, frm, fs);        // UNGUARDED cast
```

`FormComponentPropertyType.getForm(...)`
(`servoy_ngclient/.../property/types/FormComponentPropertyType.java:230`, confirmed) accepts a
`String`, a `UUID`, a `JSForm` **or** a `JSONObject`, so a bare-string `containedForm` that
happens to name/UUID a real form resolves to a non-null `Form`. The `if (frm == null) continue;`
guard therefore passes while `propertyValue` is still a `String`, and the following
`(JSONObject)propertyValue` cast throws a context-free `ClassCastException`. In the NG client
the reproduced symptom is a blank page; the designer / outline / ghost-handler sites are in the
IDE, so the same value also breaks *editing* a form.

### 2.2 The SVY-21667 helper and pattern

SVY-21667 introduced the fix once and shipped it (servoy-client `488c6adf1`):

```java
// com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType (lines 254-277, confirmed)
public static JSONObject asFormComponentValue(Object value, String formName, String elementName, String propertyName)
{
    if (value instanceof JSONObject) return (JSONObject)value;
    Debug.error("Form component property '" + propertyName + "' on element '" + elementName +
        "' of form '" + formName + "' has an unexpected value: " +
        (value == null ? "null" : value.getClass().getSimpleName() + " " + value) +
        ". Expected a JSON object of the form { \"" + SVY_FORM + "\": \"<form UUID>\" }. " +
        "Skipping this form-component property. (SVY-21667)");
    return null;
}
```

It is a `public static` method, so every one of the nine sites — in either repo — can call it
directly as `FormComponentPropertyType.asFormComponentValue(...)`. It is a strict
pass-through: for a real `JSONObject` it returns the value unchanged; for anything else it logs
one clear, actionable, greppable (`(SVY-21667)`) message and returns `null`.

### 2.3 Why these nine were deferred

- The **three servoy-client sites** were discovered *during* SVY-21667 (that spec's §3.3c,
  "Three further unguarded sites found, left unfixed by explicit decision (2026-11-02)") and
  left unfixed only because the user chose **"Document only, fix later."** SVY-21714 is that
  "fix later." SVY-21667 §3.3c already pre-wrote the guidance for exactly these three (see §3.2
  below), including that they need **no** `recursionCheck` rebalancing.
- The **six servoy-eclipse sites** are additional instances of the same shape, found later
  while testing SVY-21668.

`ServoyFormBuilder.addFormMarkers` (servoy-eclipse) is **not** among the nine — it was already
fixed under SVY-21667 commit `1ae8b12c` (triage §"Ticket premise check") and is out of scope.

## 3. Design

### 3.1 The guard pattern to apply

At each site, between the existing `if (frm == null) continue;` and the `(JSONObject)`-casting
call, insert the guard:

```java
JSONObject fcValue = FormComponentPropertyType.asFormComponentValue(
    propertyValue,          // the SAME raw value the site currently casts
    <formName>,             // owning form name, or null if none in scope
    <elementName>,          // element name, or null if none in scope
    pd.getName());          // the form-component property name (e.g. "containedForm")
if (fcValue == null) continue;   // reject-and-report: skip this property
... getFormComponentCache(..., fcValue, frm, ...);  // pass the guarded JSONObject, not the raw value
```

- Pass the **same object** the site currently casts as the first argument.
- Replace the `(JSONObject)<rawValue>` argument in the downstream call with the guarded
  `fcValue` local.
- `continue` (or the site's equivalent skip) on `null`.
- Per SVY-21667 §3.3c, **none** of the nine sites needs `recursionCheck`/`recursiveCheck`
  rebalancing on rejection — except where a `recursiveCheck.add(...)` has *already run* before
  the cast in the same loop iteration (GhostHandler and DesignerWebsocketSession's first site,
  which use a `forms`/`recursiveCheck` set); at those, mirror the SVY-21667 primary-site rule
  and `remove` the just-added name before `continue`. See each site below for the exact call.
- Where no real form/element is in scope, pass `null` for that argument; the message degrades
  gracefully (`element 'null'`) and is still far better than a context-free `ClassCastException`.

### 3.2 servoy-client sites (3)

Repo: `C:\Users\Cristian\git\servoy\master\servoy-client`, project `servoy_ngclient`,
package root `src/com/servoy/j2db/server/ngclient/`. Per SVY-21667 §3.3c these three have
**no** `recursionCheck`/`recursiveCheck` set, so a plain `continue` on rejection is sufficient
— no `remove()` call.

**3.2.1 `ChildrenJSONGenerator.java` — `visit(IPersist)` (~line 243)** *(reproduced site)*
Current (confirmed, inside the `for (PropertyDescription pd : properties)` loop):
```java
Object propertyValue = fe.getPropertyValue(pd.getName());
Form frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, context.getSolution());
if (frm == null) continue;
frm = context.getSolution().getFlattenedForm(frm);
isResponsive = frm.isResponsiveLayout();
...
children.add("children_" + pd.getName());
writer.key("children_" + pd.getName());
writer.array();
FormComponentCache fccc = FormElementHelper.INSTANCE.getFormComponentCache(fe, pd,
    (JSONObject)propertyValue, frm, context.getSolution());
```
Replacement: after `if (frm == null) continue;` (before the `frm = ...getFlattenedForm(frm);`
line — guard the raw value before any further work on it), insert
`JSONObject fccValue = FormComponentPropertyType.asFormComponentValue(propertyValue,
fe.getForm() != null ? fe.getForm().getName() : null, fe.getName(), pd.getName());
if (fccValue == null) continue;` and pass `fccValue` in place of `(JSONObject)propertyValue`.
`fe` is the `FormElement` in scope, giving both the owning form and element names.
*(Note the near-miss at `ChildrenJSONGenerator.java` ~511 that only reads
`frm.isResponsiveLayout()` without a following cast — SVY-21667 §3.3c flagged it as NOT
vulnerable; do not change it.)*

**3.2.2 `utils/FormComponentUtils.java` — `addFormComponentComponentChildrenInternal` (~line 92)**
Current (confirmed):
```java
Object rawPropertyValueOfFEsFCProperty = elementThatUsesAFormComponentInIt.getPropertyValue(pd.getName());
Form formComponent = FormComponentPropertyType.INSTANCE.getForm(rawPropertyValueOfFEsFCProperty, fs);
if (formComponent == null) continue;
FormComponentCache cache = FormElementHelper.INSTANCE.getFormComponentCache(elementThatUsesAFormComponentInIt, pd,
    (JSONObject)rawPropertyValueOfFEsFCProperty, formComponent, fs);
```
Replacement: after `if (formComponent == null) continue;` insert
`JSONObject fcValue = FormComponentPropertyType.asFormComponentValue(rawPropertyValueOfFEsFCProperty,
elementThatUsesAFormComponentInIt.getForm() != null ? elementThatUsesAFormComponentInIt.getForm().getName() : null,
elementThatUsesAFormComponentInIt.getName(), pd.getName()); if (fcValue == null) continue;` and
pass `fcValue` in place of `(JSONObject)rawPropertyValueOfFEsFCProperty`.
`elementThatUsesAFormComponentInIt` is the `FormElement` in scope.

**3.2.3 `property/types/FormComponentSabloValue.java` — `getComponents()` (~line 129)**
Current (confirmed, inner `for (PropertyDescription pd : properties)` loop):
```java
Object propertyValue = element.getPropertyValue(pd.getName());
Form frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, dal.getApplication().getFlattenedSolution());
if (frm == null) continue;
FormComponentCache innerCache = FormElementHelper.INSTANCE.getFormComponentCache(element, pd,
    (JSONObject)propertyValue, frm, dal.getApplication().getFlattenedSolution());
```
Replacement: after `if (frm == null) continue;` insert
`JSONObject fcValue = FormComponentPropertyType.asFormComponentValue(propertyValue,
element.getForm() != null ? element.getForm().getName() : null, element.getName(), pd.getName());
if (fcValue == null) continue;` and pass `fcValue` in place of `(JSONObject)propertyValue`.
`element` is the `FormElement` in scope.

### 3.3 servoy-eclipse sites (four files: DesignerWebsocketSession x3, GhostHandler, WebFormComponentChildType, FormOutlineContentProvider)

> **Count used in this spec:** "nine sites" = 3 servoy-client + 6 servoy-eclipse, where each
> servoy-eclipse *site* is one guarded location: DesignerWebsocketSession (3), GhostHandler (1),
> WebFormComponentChildType (1 file-level site, several casts - §3.3.5, §3.3.6, §3.3.8) and
> FormOutlineContentProvider (1, §3.3.7).

Repo: `C:\Users\Cristian\git\servoy\master\servoy-eclipse`. All call
`FormComponentPropertyType.asFormComponentValue(...)` (fully-qualified, with the import already
present where `FormComponentPropertyType` is used — it is, at every site below).

**3.3.1 `com.servoy.eclipse.designer` — `editor/rfb/DesignerWebsocketSession.java`, first site (~line 980)**
*(note: actual path is `editor/rfb/`, not `rfb/` as the triage abbreviated.)*
Current (confirmed, inside a `for (PropertyDescription pd : properties)` loop that uses a
`forms` `Set<String>` for recursion detection):
```java
Object propertyValue = formElement.getPropertyValue(pd.getName());
Form frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, fs);
if (frm == null) continue;
if (!forms.add(frm.getName())) { Debug.error(...); continue; }
...
FormComponentCache cache = FormElementHelper.INSTANCE.getFormComponentCache(formElement, pd, (JSONObject)propertyValue, frm, fs);
```
Replacement: `forms.add(frm.getName())` has already run by the cast, so guard **and rebalance**:
immediately before the `getFormComponentCache` call insert
`JSONObject fcValue = FormComponentPropertyType.asFormComponentValue(propertyValue,
formElement.getForm() != null ? formElement.getForm().getName() : null, formElement.getName(), pd.getName());
if (fcValue == null) { forms.remove(frm.getName()); continue; }` and pass `fcValue` in place of
`(JSONObject)propertyValue`. `formElement` is the `FormElement` in scope.

**3.3.2 `DesignerWebsocketSession.java`, second site — `getFormComponentElement(...)` (~line 1034)**
Current (confirmed; guarded by `if (frm != null) { ... }`, no recursion set):
```java
Object propertyValue = formComponentEl.getPropertyValue(formComponentPropertyTypePD.getName());
Form frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, fs);
if (frm != null)
{
    FormComponentCache cache = FormElementHelper.INSTANCE.getFormComponentCache(formComponentEl, formComponentPropertyTypePD,
        (JSONObject)propertyValue, frm, fs);
    ...
```
Replacement: at the top of the `if (frm != null)` block insert
`JSONObject fcValue = FormComponentPropertyType.asFormComponentValue(propertyValue,
formComponentEl.getForm() != null ? formComponentEl.getForm().getName() : null, formComponentEl.getName(),
formComponentPropertyTypePD.getName()); if (fcValue == null) continue;` (the enclosing loop
makes `continue` correct — confirm it is inside the `for` when implementing; if this block is
not in a loop, use the method's existing skip/`return` path instead) and pass `fcValue` in place
of `(JSONObject)propertyValue`. `formComponentEl` is the `FormElement` in scope.

**3.3.3 `DesignerWebsocketSession.java`, third site — form-component templates loop (~line 1108)**
Current (confirmed, inside `for (PropertyDescription pd : properties)`, no recursion set):
```java
Object propertyValue = fe.getPropertyValue(pd.getName());
Form frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, fs);
if (frm == null) continue;
FormComponentCache cache = FormElementHelper.INSTANCE.getFormComponentCache(fe, pd, (JSONObject)propertyValue, frm, fs);
```
Replacement: after `if (frm == null) continue;` insert
`JSONObject fcValue = FormComponentPropertyType.asFormComponentValue(propertyValue,
fe.getForm() != null ? fe.getForm().getName() : null, fe.getName(), pd.getName());
if (fcValue == null) continue;` and pass `fcValue` in place of `(JSONObject)propertyValue`.
`fe` is the `FormElement` in scope.

**3.3.4 `com.servoy.eclipse.designer` — `editor/rfb/actions/handlers/GhostHandler.java` (~line 650)**
Current (confirmed, inside `for (PropertyDescription pd : properties)` using a `recursiveCheck`
`Set<String>`):
```java
Object propertyValue = formComponentEl.getPropertyValue(pd.getName());
Form frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, fs);
if (frm == null) continue;
if (!recursiveCheck.add(frm.getName())) { Debug.error(...); continue; }
FormComponentCache cache = FormElementHelper.INSTANCE.getFormComponentCache(formComponentEl, pd, (JSONObject)propertyValue, frm, fs);
```
Replacement: `recursiveCheck.add(frm.getName())` has already run, so guard **and rebalance**:
before the `getFormComponentCache` call insert
`JSONObject fcValue = FormComponentPropertyType.asFormComponentValue(propertyValue,
formComponentEl.getForm() != null ? formComponentEl.getForm().getName() : null, formComponentEl.getName(), pd.getName());
if (fcValue == null) { recursiveCheck.remove(frm.getName()); continue; }` and pass `fcValue` in
place of `(JSONObject)propertyValue`. `formComponentEl` is the `FormElement` in scope.

**3.3.5 `com.servoy.eclipse.model` — `util/WebFormComponentChildType.java`, `initializeElement()` (~line 407)**
Current (confirmed; inside `if (currentFCPersist != null) { ... }`, no recursion set):
```java
FormComponentCache cache = FormElementHelper.INSTANCE.getFormComponentCache(currentNgFormElementOfParentFC, currentFCCachedPD,
    (JSONObject)currentNgFormElementOfParentFC.getPropertyValue(currentFCCachedPD.getName()), currentFCPersist, fs);
```
Replacement: `getPropertyValue(...)` is evaluated twice and the value is cast once. Hoist it to
a local first, guard, then pass the guard result:
```java
Object fcRawValue = currentNgFormElementOfParentFC.getPropertyValue(currentFCCachedPD.getName());
JSONObject fcValue = FormComponentPropertyType.asFormComponentValue(fcRawValue,
    currentNgFormElementOfParentFC.getForm() != null ? currentNgFormElementOfParentFC.getForm().getName() : null,
    currentNgFormElementOfParentFC.getName(), currentFCCachedPD.getName());
if (fcValue != null)
{
    FormComponentCache cache = FormElementHelper.INSTANCE.getFormComponentCache(currentNgFormElementOfParentFC,
        currentFCCachedPD, fcValue, currentFCPersist, fs);
    ... (the existing for-loop over cache.getFormComponentElements() goes here)
}
```
Because this site is **not** in a loop (it is the tail `if (currentFCPersist != null)` block of
`initializeElement()` that just returns `currentChildComponentSpec` afterwards), a `continue`
is not available — wrap the cache-building + element-matching in `if (fcValue != null) { ... }`
and fall through to the existing `return currentChildComponentSpec;` when it is null (i.e.
`element` simply stays unset, exactly as it would if `currentFCPersist` were null).
`currentNgFormElementOfParentFC` is the `FormElement` in scope.

**3.3.6 `WebFormComponentChildType.java`, `initializeElement()`, second occurrence (~line 383)** —
there is a **second, identical** `(JSONObject)currentNgFormElementOfParentFC.getPropertyValue(currentFCCachedPD.getName())`
cast earlier in the same method, inside the `while` loop's
`if (propertyInsideCurrentFCPD.getType() == FormComponentPropertyType.INSTANCE && currentFCPersist != null)`
branch. It has the identical defect shape and must be guarded the same way to fully close this
file (the triage counted the file once, at line 407; both casts are the same bug and are
covered together as this one servoy-eclipse "site"). Replacement there, inside that `if`:
```java
Object fcRawValue = currentNgFormElementOfParentFC.getPropertyValue(currentFCCachedPD.getName());
JSONObject fcValue = FormComponentPropertyType.asFormComponentValue(fcRawValue,
    currentNgFormElementOfParentFC.getForm() != null ? currentNgFormElementOfParentFC.getForm().getName() : null,
    currentNgFormElementOfParentFC.getName(), currentFCCachedPD.getName());
if (fcValue != null)
{
    FormComponentCache cache = FormElementHelper.INSTANCE.getFormComponentCache(currentNgFormElementOfParentFC,
        currentFCCachedPD, fcValue, currentFCPersist, fs);
    ... (the existing for-loop that reassigns currentNgFormElementOfParentFC / currentFCCachedPD / currentFCPersist)
}
```
When `fcValue` is null, skip the cache/element-matching block and let the `while` loop continue
with its existing `i++` — the nested form element simply is not resolved, matching the existing
behaviour when `currentFCPersist` is null for that branch.

**3.3.7 `com.servoy.eclipse.designer` — `outline/FormOutlineContentProvider.java`, `getPersistChildrenAsList()` (~line 443)**
*(Added after review: the original spec omitted this site although the triage listed it.)*
Current (confirmed, inside `for (PropertyDescription pd : propertiesOfFormComponentType)`, no recursion set):
```java
Object fcPropertyValue = formComponentComponentNGElement.getPropertyValue(pd.getName());
Form formThatIsFormComponent = FormComponentPropertyType.INSTANCE.getForm(fcPropertyValue, ModelUtils.getEditingFlattenedSolution(form));
if (formThatIsFormComponent == null) continue;
FormComponentCache fcCache = FormElementHelper.INSTANCE.getFormComponentCache(formComponentComponentNGElement, pd,
    (JSONObject)fcPropertyValue, formThatIsFormComponent, ModelUtils.getEditingFlattenedSolution(form));
```
Replacement: after `if (formThatIsFormComponent == null) continue;` insert
`JSONObject fcValue = FormComponentPropertyType.asFormComponentValue(fcPropertyValue,
formComponentComponentNGElement.getForm() != null ? formComponentComponentNGElement.getForm().getName() : null,
formComponentComponentNGElement.getName(), pd.getName()); if (fcValue == null) continue;` and pass `fcValue` in
place of `(JSONObject)fcPropertyValue`. A plain `continue` is correct (no recursion set). `formComponentComponentNGElement`
is the `FormElement` in scope.

**3.3.8 `WebFormComponentChildType.java` — root parent value (`initializeElement()` ~line 344, `getJson(...)` ~lines 501/519)**
*(Added after a full sweep of every `getForm(...)` / `getFormComponentCache(...)` caller.)*
The class casts the parent's root form-component property
(`getParent().getProperty(fcCompAndPropPath[1])`) to `JSONObject` in three places, *before* any
`getForm` check. A bare-string value made the constructor throw, which breaks every caller that
builds one: `FormOutlineContentProvider`, `PersistFinder.searchForPersist` (designer selection,
property edits, drag/drop) and `VisualFormEditorTabSequencePage`.
- `initializeElement()`: a missing (`null`) root value returns `null` silently (nothing to report);
  otherwise read the value through `asFormComponentValue(...)` (reports once, naming the parent's
  form via `getAncestor(IRepository.FORMS)`, the parent element and the root property) and
  `return null` on rejection. The guard runs before `FormElementHelper.getFormElement(...)`, so a
  rejected value does no further work. `propertyDescription`
  and `element` then stay `null`; every consumer already null-checks both
  (`getTypeName`, `getPropertyDefaultValueClone`, `getAllObjectsAsList`,
  `convertTo/FromJavaType`, `DesignerPropertyAdapterFactory`, `PersistFinder`, `PersistPropertySource`).
- `getJson(...)`: a private `getRootFCValueOfParent()` returns the value only when it is a
  `JSONObject`, else `null`; on `null` `getJson` returns an empty, detached `JSONObject` instead of
  casting. The mutation branch's `jsonObject.getJSONObject(...)` becomes `optJSONObject(...)` so a
  string in the parent's own json falls through to the same guarded read. No log here - the
  constructor already reported the bad value.
- Write methods (`setProperty`, `removeProperty`, `clearProperty`, `clearCustomProperty`) go
  through a private `getJsonForMutation(...)` that refuses the edit when the parent's root value
  is not a `JSONObject`: it logs one `Debug.error` naming the edit, the child and the parent
  property, and the method returns **without** calling `getParent().flagChanged()` or touching
  the form's `lastModified`. Before this, the edit went into the detached empty object while the
  form was still marked dirty, so the user lost the edit with no message (peer review, SVY-21714).
  The value must be fixed in the `.frm` first; the builder and Outline already report it.

> **Both WebFormComponentChildType casts are inside the single method `initializeElement()`
> (lines 332-426 as read).** Treat them as one file-level site with two edits. Do NOT guard the
> three *other* `JSONObject` casts in this file (`getJson(...)` at ~473/477/494 and the
> `optJSONObject`/`new JSONObject()` uses) — those are not form-component-value casts of a
> `getForm`-resolved property and are not in scope.

### 3.4 Git history

Carried from the triage (§"Git history findings"), confirmed:
- `servoy-client@488c6adf1` (2026-11-02) "SVY-21667 Guard form-component value cast, report
  clear error [ai]" introduced `asFormComponentValue` and fixed the first batch; its spec
  §3.3c explicitly recorded the three servoy-client sites as found-but-deferred ("Document only,
  fix later") — the direct provenance of SVY-21714.
- `servoy-eclipse@1ae8b12c` / `servoy-client@2375e818` (2026-11-03) "SVY-21667 Guard
  form-component cast in Servoy Project Builder [ai]" fixed `ServoyFormBuilder.addFormMarkers`,
  which is why that site is excluded here.
- No regression / dependency-version angle: these nine sites were never guarded — not broken by
  an upgrade. This is pre-existing code SVY-21667 began hardening and SVY-21714 finishes.

## 4. Implementation plan

Work happens in **BOTH** checkouts. Edit every Eclipse-project file through the `eclipse-coder`
tools (never built-in edit/write). After **every** file edit run
`tools["eclipse-ide"].getCompilationErrors` on that file and fix before moving on;
`tools["eclipse-coder"].organizeImports` where needed (no new imports expected — `JSONObject`,
`Debug` and `FormComponentPropertyType` are already imported at every site). No MANIFEST / pom /
Export-Package changes — the helper already exists and is exported.

servoy-client (`C:\Users\Cristian\git\servoy\master\servoy-client`, project `servoy_ngclient`):
1. `src/com/servoy/j2db/server/ngclient/ChildrenJSONGenerator.java` — guard `visit(...)` (§3.2.1).
2. `src/com/servoy/j2db/server/ngclient/utils/FormComponentUtils.java` — guard
   `addFormComponentComponentChildrenInternal` (§3.2.2).
3. `src/com/servoy/j2db/server/ngclient/property/types/FormComponentSabloValue.java` — guard
   `getComponents()` (§3.2.3).

servoy-eclipse (`C:\Users\Cristian\git\servoy\master\servoy-eclipse`):
4. `com.servoy.eclipse.designer/src/com/servoy/eclipse/designer/editor/rfb/DesignerWebsocketSession.java`
   — three sites (§3.3.1 with `forms.remove`, §3.3.2, §3.3.3).
5. `com.servoy.eclipse.designer/src/com/servoy/eclipse/designer/editor/rfb/actions/handlers/GhostHandler.java`
   — one site with `recursiveCheck.remove` (§3.3.4).
6. `com.servoy.eclipse.model/src/com/servoy/eclipse/model/util/WebFormComponentChildType.java`
   — both casts in `initializeElement()` (§3.3.5 and §3.3.6).
6a. Same file — the root parent-value casts (§3.3.8).
6b. `com.servoy.eclipse.designer/src/com/servoy/eclipse/designer/outline/FormOutlineContentProvider.java`
   — `getPersistChildrenAsList` (§3.3.7).

7. **Re-confirm each line number by reading before editing** — line numbers drift; locate each
   `(JSONObject)` cast with `grep` / `eclipse-ide` search first. The triage's `rfb/` paths are
   actually `editor/rfb/`.
8. **Verify:** after all edits, `getCompilationErrors` clean on all seven changed files across
   both repos; fix the two highest-severity SpotBugs levels in any changed code.
9. **Two commits, two repos** — one in servoy-client, one in servoy-eclipse; each subject must
   carry `SVY-21714` and end with `[ai]`. Ask for commit permission per the repo rules; do not
   push without explicit approval.

## 5. Acceptance criteria

- [ ] A form-component property written as a bare form-name/UUID string no longer throws
      `ClassCastException` at any of the nine sites (three servoy-client, six servoy-eclipse —
      counting the two `WebFormComponentChildType` casts as that one file's site).
- [ ] Each guarded site logs one clear `Debug.error` naming the form, element and property (and
      the actual value + expected `{ "svy_form": "<UUID>" }` shape, from the shared helper) when
      it rejects a malformed value, then skips that property; the rest of the form still
      renders in the NG client and still edits/builds in the IDE.
- [ ] Valid `{ "svy_form": "<UUID>" }` values behave exactly as before — the guard is a
      pass-through, no new log noise, no behavioural change.
- [ ] Sites that already added a form name to a `forms`/`recursiveCheck` set before the cast
      (DesignerWebsocketSession §3.3.1, GhostHandler §3.3.4) `remove` that name on rejection, so
      a later legitimate reference to the same form in the same traversal is not wrongly treated
      as recursive.
- [ ] `getCompilationErrors` reports no errors in any of the seven changed files across both
      repos; the two highest-severity SpotBugs levels are clean in the changed code.
- [ ] Both `(JSONObject)` casts inside `WebFormComponentChildType.initializeElement()` are
      guarded (not only the one the triage named at line 407).
- [ ] The root form-component value read from the parent in `WebFormComponentChildType`
      (`initializeElement()` and `getJson(...)`, §3.3.8) is guarded, so constructing a
      `WebFormComponentChildType` (Outline, PersistFinder, tab sequence page) on a malformed
      parent no longer throws.

## 6. Out of scope

- The optional single shared resolve-and-validate helper (triage Approach 2) — deferred.
- `ServoyFormBuilder.addFormMarkers` — already fixed under SVY-21667 commit `1ae8b12c`.
- Any change to `FormComponentPropertyType.getForm(...)`'s String/UUID/JSForm tolerance.
- Coercing a bare string into `{ "svy_form": <UUID> }` (triage Approach 2 / SVY-21667 out of
  scope) — this is reject-and-report only.
- The non-form-component `JSONObject` casts in `WebFormComponentChildType.getJson(...)` and the
  `ChildrenJSONGenerator` ~511 `isResponsiveLayout()` read (no following cast) — not vulnerable.
- Deduplicating the log line across structurally independent sites that each touch the same bad
  value in one operation — tracked as the SVY-21667 low-priority follow-up, not this fix.
- Per-site tests for the pure forwarder sites (the `WebFormComponentChildType` root-value guard,
  §3.3.8, is the exception: it changes control flow, so it is covered by
  `WebFormComponentChildTypeTest.MalformedRootFormComponentValue` in `com.servoy.eclipse.model.tests`,
  plainjunit). The shared helper already has unit coverage
  (`FormComponentPropertyTypeAsFormComponentValueTest`) and integration coverage
  (`FormComponentPropertyTypeSabloRhinoGuardTest`) from SVY-21667. If the code-review/test phase
  wants per-site coverage for the six IDE sites, that is handled downstream in the pipeline, not
  mandated by this spec.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| §3.3.2 `DesignerWebsocketSession.getFormComponentElement`: is the `if (frm != null)` block inside a loop (so `continue` is valid) or straight-line (so the skip must be a `return`/fall-through)? Confirm by reading the enclosing method before editing. | implementer | open |
| Should the six servoy-eclipse sites get dedicated per-site tests, or is the shared SVY-21667 helper coverage plus a manual malformed-`.frm` check sufficient? | implementer / reviewer | open |
| Both repos must be committed for the symptom to be fully closed — confirm the servoy-client commit lands (or is already present) so the IDE sites, which call its exported helper, resolve it at build time. | implementer | open (resolved by SVY-21667 already shipping the helper) |
