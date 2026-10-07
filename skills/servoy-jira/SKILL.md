---
name: servoy-jira
description: "Use when asked to create, read, update, search, link, or comment on Jira issues. Triggered by: create a jira case, jira issue, file a bug, update jira, link issues, search jira, JQL, or any Jira issue key like SVY-12345, SVYX-456, SERVOY-293."
---

# Jira API Reference

Load this skill when asked to create, update, read, or link Jira issues.

## Connection

Base URL: `https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3`
Auth: Basic auth via environment variable `ATLASSIAN_AUTH_BASIC` (base64-encoded `email:api-token`).

## Platform detection

Choose the correct shell based on the OS:
- **Windows**: Use PowerShell with `Invoke-RestMethod`
- **macOS/Linux**: Use bash with `curl`

**CRITICAL**: Do NOT use `curl` in PowerShell — it's an alias for `Invoke-WebRequest` and `-s`/`-H` flags will fail with cryptic errors.

---

## Reading an issue

### PowerShell (Windows)

```powershell
$token = $env:ATLASSIAN_AUTH_BASIC
$headers = @{ "Authorization" = "Basic $token" }
$response = Invoke-RestMethod -Uri "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/issue/{ISSUE_KEY}?fields=summary,description,comment,attachment,issuelinks,subtasks,status,priority,components,fixVersions,labels" -Headers $headers
$response | ConvertTo-Json -Depth 20
```

### bash (macOS/Linux)

```bash
TOKEN="$ATLASSIAN_AUTH_BASIC"
curl -s -H "Authorization: Basic $TOKEN" \
  "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/issue/{ISSUE_KEY}?fields=summary,description,comment,attachment,issuelinks,subtasks,status,priority,components,fixVersions,labels"
```

---

## Downloading an attachment

### PowerShell (Windows)

```powershell
$token = $env:ATLASSIAN_AUTH_BASIC
$headers = @{ "Authorization" = "Basic $token" }
Invoke-RestMethod -Uri "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/attachment/content/{ATTACHMENT_ID}" -Headers $headers
```

### bash (macOS/Linux)

```bash
TOKEN="$ATLASSIAN_AUTH_BASIC"
curl -s -L -H "Authorization: Basic $TOKEN" \
  "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/attachment/content/{ATTACHMENT_ID}"
```

---

## Searching issues (JQL)

### PowerShell (Windows)

```powershell
$token = $env:ATLASSIAN_AUTH_BASIC
$headers = @{ "Authorization" = "Basic $token" }
$response = Invoke-RestMethod -Uri "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/search?jql={URL_ENCODED_JQL}&fields=summary,status" -Headers $headers
$response | ConvertTo-Json -Depth 20
```

### bash (macOS/Linux)

```bash
TOKEN="$ATLASSIAN_AUTH_BASIC"
curl -s -H "Authorization: Basic $TOKEN" \
  "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/search?jql={URL_ENCODED_JQL}&fields=summary,status"
```

---

## Creating an issue

### PowerShell (Windows)

```powershell
$token = $env:ATLASSIAN_AUTH_BASIC
$headers = @{ "Authorization" = "Basic $token"; "Content-Type" = "application/json" }
$jsonBody = '<json string>'
Invoke-RestMethod -Uri "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/issue" `
  -Method POST -Headers $headers `
  -Body ([System.Text.Encoding]::UTF8.GetBytes($jsonBody)) `
  -ContentType "application/json"
```

### bash (macOS/Linux)

```bash
TOKEN="$ATLASSIAN_AUTH_BASIC"
JSON_BODY='<json string>'
curl -s -X POST \
  -H "Authorization: Basic $TOKEN" \
  -H "Content-Type: application/json" \
  -d "$JSON_BODY" \
  "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/issue"
```

### Available issue types for SVY project

| Name | ID | Use for |
|------|----|---------|
| Task | 10002 | Refactoring, technical work, improvements |
| Bug | 10004 | Defects, problems |
| New Feature | 10045 | New product features |

There is **no** "Improvement" issue type — use "Task" for refactoring/improvements.

### ADF description template

Description must use ADF (Atlassian Document Format) — not plain text or markdown.

```json
{
  "type": "doc",
  "version": 1,
  "content": [
    {
      "type": "paragraph",
      "content": [{"type": "text", "text": "Your paragraph text here"}]
    }
  ]
}
```

Valid block nodes: `paragraph`, `heading` (with `attrs.level`), `bulletList`, `orderedList`, `codeBlock`, `blockquote`, `rule`.
Headings use: `{"type": "heading", "attrs": {"level": 2}, "content": [{"type": "text", "text": "..."}]}`
List items: `{"type": "bulletList", "content": [{"type": "listItem", "content": [{"type": "paragraph", "content": [...]}]}]}`

---

## Linking issues

### PowerShell (Windows)

```powershell
$token = $env:ATLASSIAN_AUTH_BASIC
$headers = @{ "Authorization" = "Basic $token"; "Content-Type" = "application/json" }
$jsonBody = '{"type":{"name":"Relates"},"inwardIssue":{"key":"SVY-XXXXX"},"outwardIssue":{"key":"SVY-YYYYY"}}'
Invoke-RestMethod -Uri "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/issueLink" `
  -Method POST -Headers $headers `
  -Body ([System.Text.Encoding]::UTF8.GetBytes($jsonBody)) `
  -ContentType "application/json"
```

### bash (macOS/Linux)

```bash
TOKEN="$ATLASSIAN_AUTH_BASIC"
curl -s -X POST \
  -H "Authorization: Basic $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"type":{"name":"Relates"},"inwardIssue":{"key":"SVY-XXXXX"},"outwardIssue":{"key":"SVY-YYYYY"}}' \
  "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/issueLink"
```

Link type names: `"Relates"`, `"Blocks"`, `"Cloners"`, `"Duplicate"`.

---

## Adding a comment

Comments use ADF (same document format as descriptions). The body is `{"body": <ADF doc>}`.

**Windows quoting warning:** PowerShell mangles inline JSON that contains nested quotes.
Write the JSON body to a UTF-8 temp file and pass it by reference instead of inlining it.

### PowerShell (Windows)

```powershell
$token = $env:ATLASSIAN_AUTH_BASIC
$headers = @{ "Authorization" = "Basic $token"; "Content-Type" = "application/json" }
# $jsonBody built as a here-string / written to a temp file to avoid inline-quote mangling
$tmp = Join-Path $env:TEMP "jira-comment.json"
Set-Content -Path $tmp -Value $jsonBody -Encoding utf8
Invoke-RestMethod -Uri "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/issue/{ISSUE_KEY}/comment" `
  -Method POST -Headers $headers `
  -Body ([System.Text.Encoding]::UTF8.GetBytes((Get-Content -Raw $tmp))) -ContentType "application/json"
Remove-Item $tmp
```

### bash (macOS/Linux)

```bash
TOKEN="$ATLASSIAN_AUTH_BASIC"
curl -s -X POST -H "Authorization: Basic $TOKEN" -H "Content-Type: application/json" \
  -d "$JSON_BODY" \
  "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/issue/{ISSUE_KEY}/comment"
```

### Comment body with an ordered (numbered) list

A plain paragraph with `\n` does **not** render as a list — use `orderedList` +
`listItem` + `paragraph` nodes. A `heading` above it gives the block a title.

```json
{
  "body": {
    "type": "doc",
    "version": 1,
    "content": [
      {"type": "heading", "attrs": {"level": 3}, "content": [{"type": "text", "text": "Manual test plan"}]},
      {"type": "orderedList", "content": [
        {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "First step"}]}]},
        {"type": "listItem", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Second step"}]}]}
      ]},
      {"type": "paragraph", "content": [{"type": "text", "text": "-- posted by review assistant"}]}
    ]
  }
}
```

Use `bulletList` instead of `orderedList` for an unordered list; the item structure is
identical.

---

## Who am I (current user)

```powershell
$token = $env:ATLASSIAN_AUTH_BASIC
$headers = @{ "Authorization" = "Basic $token" }
$me = Invoke-RestMethod -Uri "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/myself" -Headers $headers
$me.accountId   # use this to compare against issue.fields.assignee.accountId
```

---

## Assigning an issue

Assign by `accountId`. Use `null` to unassign, or `"-1"` for the project default assignee.

```powershell
$token = $env:ATLASSIAN_AUTH_BASIC
$headers = @{ "Authorization" = "Basic $token"; "Content-Type" = "application/json" }
$jsonBody = '{"accountId":"<ACCOUNT_ID>"}'
Invoke-RestMethod -Uri "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/issue/{ISSUE_KEY}/assignee" `
  -Method PUT -Headers $headers `
  -Body ([System.Text.Encoding]::UTF8.GetBytes($jsonBody)) -ContentType "application/json"
```

```bash
TOKEN="$ATLASSIAN_AUTH_BASIC"
curl -s -X PUT -H "Authorization: Basic $TOKEN" -H "Content-Type: application/json" \
  -d '{"accountId":"<ACCOUNT_ID>"}' \
  "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/issue/{ISSUE_KEY}/assignee"
```

Success is `204 No Content` (empty body).

---

## Transitioning an issue (changing status)

Transition ids are **workflow-specific** — always read the available transitions for the
issue first, then match by target status name (`to.name`), never by a hardcoded id.

```powershell
$token = $env:ATLASSIAN_AUTH_BASIC
$headers = @{ "Authorization" = "Basic $token"; "Content-Type" = "application/json" }
# 1. list what's available from the current status
$t = Invoke-RestMethod -Uri "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/issue/{ISSUE_KEY}/transitions" -Headers $headers
$t.transitions | ForEach-Object { "$($_.id) $($_.name) -> $($_.to.name)" }
# 2. perform one (pick the id whose $_.to.name is the status you want)
$jsonBody = '{"transition":{"id":"<TRANSITION_ID>"}}'
Invoke-RestMethod -Uri "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/issue/{ISSUE_KEY}/transitions" `
  -Method POST -Headers $headers `
  -Body ([System.Text.Encoding]::UTF8.GetBytes($jsonBody)) -ContentType "application/json"
```

```bash
TOKEN="$ATLASSIAN_AUTH_BASIC"
curl -s -H "Authorization: Basic $TOKEN" \
  "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/issue/{ISSUE_KEY}/transitions"
curl -s -X POST -H "Authorization: Basic $TOKEN" -H "Content-Type: application/json" \
  -d '{"transition":{"id":"<TRANSITION_ID>"}}' \
  "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/issue/{ISSUE_KEY}/transitions"
```

Success is `204 No Content`. SVY project statuses: `Open`, `In Progress`, `In Review`,
`Resolved`, `Closed`, `Reopened`. From `In Review` the SVY workflow offers `Resolve Issue`
(→ `Resolved`) and `Code review problem` (→ `In Progress`) — but always confirm live.

**Note on JQL search:** this instance uses the enhanced search endpoint
`GET /rest/api/3/search/jql?jql=...&fields=...` (the older `/rest/api/3/search` returns
nothing here). URL-encode the JQL.

---

## Moving a case to "In Review" (SVY workflow screen requires Time Spent)

In the SVY workflow the `Open → In Progress → In Review` path is **not** a bare
transition: the "Ready for review" transition into `In Review` has a **transition screen
with a required `Time Spent` (worklog) field**. A plain `{"transition":{"id":...}}` fails
with `400 {"errorMessages":["Field Time Spent is required."]}`.

Learned the hard way, so follow this recipe:

1. **From `Open`**, first `Start Progress` (`Open → In Progress`), then "Ready for review".
   From `In Progress` only "Ready for review" is offered.
2. **Supply the worklog inside the transition**, in the `update.worklog[].add` block, as a
   `timeSpent` **string** (`"1h"`). The other shapes are rejected:
   - `fields.worklog` → `"Field does not support update 'worklog'"`
   - `fields.timetracking` → `"not on the appropriate screen"`
   - `update.worklog[].add` with `timeSpentSeconds` (number) → still `400 Time Spent required`
3. **Do NOT pre-book a worklog and then transition** — the screen demands its own entry, so
   you end up double-booking. If a worklog was already added, delete it first
   (`GET /issue/{KEY}/worklog` → `DELETE /issue/{KEY}/worklog/{id}`) and let the transition
   book the single hour.
4. Discover the required screen fields with
   `GET /issue/{KEY}/transitions?expand=transitions.fields` (shows `worklog`, `assignee`,
   and whether `resolution` is required) rather than guessing.

```powershell
$token = $env:ATLASSIAN_AUTH_BASIC
$headers = @{ "Authorization" = "Basic $token"; "Content-Type" = "application/json" }
# resolve the "Ready for review" transition id live (never hardcode)
$t = Invoke-RestMethod -Uri "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/issue/{ISSUE_KEY}/transitions" -Headers $headers
$review = ($t.transitions | Where-Object { $_.to.name -eq "In Review" }).id
# `started` must be ISO8601 with an offset; book 1h on the work date
$jsonBody = '{"transition":{"id":"' + $review + '"},"update":{"worklog":[{"add":{"timeSpent":"1h","started":"2026-10-06T09:00:00.000+0000"}}]}}'
Invoke-RestMethod -Uri "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/issue/{ISSUE_KEY}/transitions" `
  -Method POST -Headers $headers `
  -Body ([System.Text.Encoding]::UTF8.GetBytes($jsonBody)) -ContentType "application/json"
```

The `started` timestamp drives the day the hour lands on (e.g. a case's commit date).

---

## Setting Fix Version and Sprint

`fixVersions` and Sprint are plain field updates via `PUT /issue/{KEY}`, done **before**
the transition. **Always ask the user which fix version and which sprint to use** — these
are not fixed values. (In recent work they were `2026.12` and `sprint 385`, but confirm
each time.)

- **Fix Version** — `fixVersions` is an **array of version objects**, set by name:
  `{"fields":{"fixVersions":[{"name":"2026.12"}]}}`. List valid names with
  `GET /project/SVY/versions` (match an unreleased version).
- **Sprint** — a **custom field, `customfield_10020`**, set by the **numeric sprint id**
  (not the name): `{"fields":{"customfield_10020":4936}}`. Read a case already on the
  target sprint to find the id (`...?fields=customfield_10020` returns `{id, name, state,
  boardId, ...}`), or list sprints via the board/agile API.

```powershell
$jsonBody = '{"fields":{"fixVersions":[{"name":"2026.12"}],"customfield_10020":4936}}'
Invoke-RestMethod -Uri "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/issue/{ISSUE_KEY}" `
  -Method PUT -Headers $headers `
  -Body ([System.Text.Encoding]::UTF8.GetBytes($jsonBody)) -ContentType "application/json"
```

**Field parity when batching:** when moving several cases to the same state, mirror the
field profile of a case that is already correctly `In Review` (fix version, sprint, and a
worklog) so the batch is consistent. Inspect a reference case first:
`GET /issue/{REFERENCE_KEY}?fields=status,fixVersions,customfield_10020,aggregatetimespent`.

---

## Error handling

### PowerShell (Windows)

```powershell
try {
    $response = Invoke-RestMethod -Uri $uri -Method POST -Headers $headers -Body ([System.Text.Encoding]::UTF8.GetBytes($jsonBody)) -ContentType "application/json"
    $response | ConvertTo-Json
} catch {
    $_.Exception.Response.GetResponseStream() | ForEach-Object {
        $reader = New-Object System.IO.StreamReader($_)
        $reader.ReadToEnd()
    }
}
```

### bash (macOS/Linux)

```bash
response=$(curl -s -w "\n%{http_code}" -X POST \
  -H "Authorization: Basic $TOKEN" \
  -H "Content-Type: application/json" \
  -d "$JSON_BODY" \
  "$URL")
http_code=$(echo "$response" | tail -1)
body=$(echo "$response" | sed '$d')
if [ "$http_code" -ge 400 ]; then echo "Error $http_code: $body"; fi
```

---

## Common mistakes to avoid

- Do NOT use `curl` with `-H` flags in PowerShell — use `Invoke-RestMethod` with `-Headers` hashtable
- Do NOT use `{"name": "Improvement"}` as issue type — it doesn't exist, use `"Task"`
- Do NOT pass `-Body $jsonBody` as a plain string in PowerShell — always wrap with `[System.Text.Encoding]::UTF8.GetBytes()`
- Do NOT construct JSON via `ConvertTo-Json` on deeply nested hashtables for the request body — build the JSON string directly to avoid escaping/depth issues
- Do NOT forget to link related issues after creation
- Query issue types first if unsure: `GET /rest/api/3/issue/createmeta/SVY/issuetypes`
- Do NOT transition into `In Review` without a worklog — the SVY screen requires
  `Time Spent`; supply it as `update.worklog[].add.timeSpent` = `"1h"` (string), and never
  pre-book a worklog and then transition (that double-books). See "Moving a case to In Review".
- Do NOT hardcode the Sprint by name — it is `customfield_10020` and takes the numeric
  sprint **id**. Fix Version is `fixVersions` and takes `[{ "name": "..." }]`.
- Do NOT assume the fix version / sprint values — **ask the user** which to use each time.
