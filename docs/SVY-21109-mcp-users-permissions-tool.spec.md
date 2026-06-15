# Spec: SVY-21109 — MCP Tool for Users/Permissions Create/Update

## 1. Goal

Expose Servoy Developer's user and permission management as an MCP tool
(`IPermissionTool`), focused on configuring form security (`.sec` files). AI
assistants can create/update users, create permissions, and set form element
access rights — all without manual IDE interaction.

## 2. Background

### 2.1 Security model in Servoy Developer

Servoy Developer manages security at the workspace level through:

- **Users** — identified by name, password hash, and UUID. Stored in the
  resources project's `security.sec` file as JSON.
- **Permissions** (called "groups" in the codebase) — named security roles that
  define what access users have to form elements.
- **Form security** — access masks applied per permission to form elements.
  Stored in `<formName>.sec` files in the form's directory.

The form access constants are defined in `IRepository`:
- `VIEWABLE = 1` — whether the element is visible
- `ACCESSIBLE = 2` — whether the element is interactive
- `IMPLICIT_FORM_ACCESS = VIEWABLE | ACCESSIBLE` (default)

### 2.2 Form Editor Security Tab

Per the [Servoy documentation](https://docs.servoy.com/reference/servoy-developer/object-editors/form-editor/form-editor-security-tab.md):

- **Permissions Panel (left):** Lists all permissions configured in the solution.
- **Elements Matrix (right):** Shows all form elements for the selected
  permission, each with **Viewable** and **Accessible** checkboxes.
- **Toggle Viewable / Toggle Accessible:** Bulk actions for all elements.
- **"No rights unless explicitly specified":** Strict mode — denies access
  unless explicitly granted.

The form itself is also an entry in the elements list (form-level security).

### 2.3 Existing management APIs

`WorkspaceUserManager` (in `com.servoy.eclipse.model.repository`) implements
`IUserManager`, `IUserManagerInternal`, and `ISecurityInfoManager`. Key methods:

| Category | Methods |
|----------|---------|
| Users | `createUser`, `changeUserName`, `setPassword`, `getUsers`, `getUserUID` |
| Permissions (groups) | `createGroup`, `getGroups` |
| Form security | `setFormSecurityAccess(clientId, groupName, accessMask, formUUID, elementUID, solutionName)` |
| Persistence | `writeAllSecurityInformation(discardInvalidOldInfo)` |

Access point: `ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager()`

### 2.4 Form .sec file format

Each form's security is stored in `<formName>.sec` as a JSON object keyed by
permission name, containing element UUID to access mask mappings:

```json
{
  "Administrators": {
    "<form-uuid>": 3,
    "<button-element-uuid>": 3
  },
  "ReadOnly": {
    "<form-uuid>": 1,
    "<button-element-uuid>": 0
  }
}
```

Where `3 = VIEWABLE | ACCESSIBLE`, `1 = VIEWABLE only`, `0 = no access`.

### 2.5 MCP tool pattern

`com.servoy.eclipse.servoypilot` hosts an MCP server at
`http://localhost:8085/mcp/*`. Tools are Java interfaces annotated with
LangChain4j `@Tool` (method) and `@P` (parameter) annotations, registered in
`AllToolsForMCP`. Return type is `String` (typically markdown-formatted).

### 2.6 Dependencies

This tool assumes `IGenerateUUIDTool` (separate spec/tool) is available for
generating workspace-unique UUIDs when creating users.

## 3. Design

### 3.1 Tool interface: `IPermissionTool`

A single tool interface in `com.servoy.eclipse.servoypilot` package
`tools/security/` with the following methods:

#### User management

| Method | Parameters | Description |
|--------|-----------|-------------|
| `listUsers` | — | Returns all users with their names and UIDs |
| `createUser` | `userName`, `password`, `userUID?` | Creates a new user; auto-generates UUID if not provided. Password is plain text, hashed internally. |
| `changeUserName` | `oldName`, `newName` | Renames an existing user |
| `setUserPassword` | `userName`, `newPassword` | Changes a user's password (plain text, hashed internally) |

#### Permission management

| Method | Parameters | Description |
|--------|-----------|-------------|
| `createPermission` | `permissionName` | Creates a new permission |

#### Form security configuration

| Method | Parameters | Description |
|--------|-----------|-------------|
| `getFormSecurity` | `permissionName`, `formName`, `solutionName?` | Returns current form element access for the permission as a readable table |
| `setFormElementAccess` | `permissionName`, `formName`, `elementName?`, `viewable`, `accessible`, `solutionName?` | Sets VIEWABLE/ACCESSIBLE for a form or specific element. When `elementName` is omitted, sets access on the form itself. |
| `setFormSecurityBulk` | `permissionName`, `formName`, `accessEntries` (JSON array of `{elementName, viewable, accessible}`), `solutionName?` | Sets access for multiple elements in one call |

### 3.2 Error handling

Each tool method returns a markdown-formatted response:
- **Success:** descriptive confirmation with the resulting state
- **Error:** a clear error message explaining what went wrong and how to fix it

### 3.3 Persistence

After any mutation, the tool calls `writeAllSecurityInformation(false)` to
persist changes to the workspace `.sec` files.

### 3.4 Client ID handling

All `WorkspaceUserManager` methods require a `clientId` parameter. Obtained via
`ApplicationServerRegistry.get().getClientId()`.

### 3.5 Solution context

Form security operations require a solution name to resolve form UUIDs. If
`solutionName` is not provided, defaults to the active solution.

### 3.6 Naming convention (groups vs permissions)

In tool method names, descriptions, and responses, always use "permission"
(never "group"). Internally delegates to existing group API methods.

## 4. Implementation plan

1. **Create `IPermissionTool`** interface in
   `com.servoy.eclipse.servoypilot/src/.../tools/security/IPermissionTool.java`
   with all `@Tool`/`@P` annotated method signatures.

2. **Create `PermissionToolImpl`** implementation class in the same package.
   - Obtain `EclipseUserManager` via `ServoyModelManager`
   - Implement each method delegating to `WorkspaceUserManager` API
   - Hash passwords internally before passing to `createUser`/`setPassword`
   - Format responses as markdown
   - Call `writeAllSecurityInformation(false)` after mutations
   - Handle exceptions gracefully with descriptive messages

3. **Register in `AllToolsForMCP`** — add `IPermissionTool.class` to the registry.

4. **Handle form UUID resolution** — resolve form name to `Form` persist via
   `FlattenedSolution`, get its UUID. For elements, iterate form children by name.

5. **Handle `setFormSecurityBulk`** — accept JSON array of element entries,
   resolve each to UUID, apply all access masks, persist once.

## 5. Acceptance criteria

- [ ] `listUsers` returns all defined users with names and UIDs.
- [ ] `createUser` creates a new user with hashed password, persisted to
      `security.sec`.
- [ ] `changeUserName` and `setUserPassword` update user data and persist.
- [ ] `createPermission` creates a new permission and persists.
- [ ] `getFormSecurity` returns current access rights for all elements of a
      form for a given permission.
- [ ] `setFormElementAccess` sets VIEWABLE/ACCESSIBLE flags for a form or
      element per permission, written to the form's `.sec` file.
- [ ] `setFormSecurityBulk` sets access for multiple elements in one call.
- [ ] All tool methods return descriptive error messages when operations fail.
- [ ] Tool responses use "permission" terminology (never "group").
- [ ] The tool is accessible via the MCP server endpoint after registration.

## 6. Out of scope

- Delete operations (users, permissions).
- Table/column security configuration.
- UUID generation (separate `IGenerateUUIDTool` spec).
- UI changes to the existing Security Editor in Eclipse.
- Batch import/export of security configurations.
- Password policy enforcement.
- Menu item security (SVY-21114).
- Wildcard/pattern-based element selection in bulk operations.

## 7. Open questions

| Question | Owner | Status |
|----------|-------|--------|
| Should "No rights unless explicitly specified" (strict mode) be configurable via tool? | PM | open — defer to future enhancement |
