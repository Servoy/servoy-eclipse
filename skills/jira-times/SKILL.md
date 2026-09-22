---
name: jira-times
description: "Report the time (worklog) the current user logged on Jira cases over a date range. Triggered by '/jira-times', 'jira times', 'how much time did I write', 'my worklog', 'time I logged on cases', or a phrase like 'get me all the times from <date> until now'."
---

# Skill: jira-times

Produce a table of Jira cases the current user logged worklog time on, over a date
range, including the date the time was written, the case key, its subject, its
Operational categorization, and the time spent. Ends with a total.

## Invocation

`/jira-times <from-date>` — report worklogs from `<from-date>` (inclusive) until now.

- `<from-date>` accepts natural forms like `22 september`, `2026-09-22`, `sep 22`.
  Normalize to `yyyy-MM-dd`. If no year is given, assume the current year.
- If no date is given, ask for the start date.
- The end of the range is always "now" (today) unless the user specifies otherwise.

## Connection

Reuse the `servoy-jira` skill's connection details.

- Base URL: `https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3`
- Auth: Basic auth via env var `ATLASSIAN_AUTH_BASIC`.
- Windows: use PowerShell `Invoke-RestMethod` (never `curl`).

## Steps

1. Resolve the current user's `accountId`:
   `GET /rest/api/3/myself` → `$me.accountId`.
2. Find candidate issues with worklogs by the current user in the range using the
   enhanced JQL search endpoint (`/rest/api/3/search/jql`, URL-encode the JQL):
   `worklogAuthor = currentUser() AND worklogDate >= <from-date>`
   Request `fields=summary,customfield_10063` — the latter is the cascading-select
   **Operational categorization** field.
3. For each issue, fetch its worklogs and keep only entries whose
   `author.accountId` matches the current user AND whose `started` date is
   `>= <from-date>`. Sum `timeSpentSeconds` per entry; capture the `started` date.
4. Read Operational categorization from `fields.customfield_10063`: use `.value`,
   and if `.child` exists append it as `value > child.value`. Empty when null.
5. Build a table sorted by date, then case:
   **Date | Case | Subject | Operational categorization | Time**.
   Format time as `Hh Mm`. Add a final **Total** row summing all seconds.

## Reference script (PowerShell / Windows)

```powershell
$token = $env:ATLASSIAN_AUTH_BASIC
$headers = @{ "Authorization" = "Basic $token" }
$base = "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3"

# --- set this from the user's requested start date (yyyy-MM-dd) ---
$fromStr = "2026-09-22"
$from = Get-Date $fromStr

$myId = (Invoke-RestMethod -Uri "$base/rest/api/3/myself" -Headers $headers).accountId

$jql = "worklogAuthor = currentUser() AND worklogDate >= $fromStr"
$enc = [System.Uri]::EscapeDataString($jql)
$search = Invoke-RestMethod -Uri "$base/rest/api/3/search/jql?jql=$enc&fields=summary,customfield_10063&maxResults=100" -Headers $headers

$rows = @()
foreach ($issue in $search.issues) {
  $k = $issue.key
  $summary = $issue.fields.summary
  $op = $issue.fields.customfield_10063
  $opVal = ""
  if ($null -ne $op) {
    $opVal = $op.value
    if ($null -ne $op.child) { $opVal = "$opVal > $($op.child.value)" }
  }
  $wl = Invoke-RestMethod -Uri "$base/rest/api/3/issue/$k/worklog?maxResults=1000" -Headers $headers
  foreach ($w in $wl.worklogs) {
    if ($w.author.accountId -eq $myId -and ([datetime]$w.started) -ge $from) {
      $rows += [pscustomobject]@{
        Date    = ([datetime]$w.started).ToString("yyyy-MM-dd")
        Key     = $k
        Summary = $summary
        OpCat   = $opVal
        Seconds = $w.timeSpentSeconds
      }
    }
  }
}

$total = 0
$rows | Sort-Object Date, Key | ForEach-Object {
  $h = [math]::Floor($_.Seconds/3600); $m = [math]::Floor(($_.Seconds%3600)/60)
  "{0}|{1}|{2}|{3}|{4}h {5}m" -f $_.Date, $_.Key, $_.Summary, $_.OpCat, $h, $m
  $total += $_.Seconds
}
$th = [math]::Floor($total/3600); $tm = [math]::Floor(($total%3600)/60)
"---TOTAL|$th h $tm m"
```

Then render the piped output as a Markdown table with columns
**Date | Case | Subject | Operational categorization | Time** and a **Total** row.

## Notes

- A case may have multiple worklog entries in the range — emit one row per entry
  (per date), not one row per case.
- `worklogDate` in JQL filters at day granularity; the per-entry `started >= $from`
  check keeps results exact.
- If the JQL search endpoint returns nothing, confirm you are using
  `/rest/api/3/search/jql` (the older `/rest/api/3/search` returns nothing on this
  instance).
